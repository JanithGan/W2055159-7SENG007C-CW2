package com.uow.ds.cw.services;

import com.uow.ds.cw.*;
import grpc.generated.Status;
import grpc.generated.StatusMessageResponse;
import grpc.generated.ReserveItemRequest;
import grpc.generated.ReserveItemServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.UUID;

public class ReserveItemServiceImpl extends ReserveItemServiceGrpc.ReserveItemServiceImplBase implements DistributedTxListener {
    ReserveItemServiceGrpc.ReserveItemServiceBlockingStub clientStub = null;

    private final InventoryManagementServer server;
    private final DataStoreImpl dataStore;

    private Status status = Status.FAILURE;
    private String statusMessage = "";

    private AbstractMap.SimpleEntry<String, ReserveItemRequest> tempDataHolder;

    public ReserveItemServiceImpl(InventoryManagementServer inventoryManagementServer, DataStoreImpl dataStore) {
        this.server = inventoryManagementServer;
        this.dataStore = dataStore;
    }

    @Override
    public void reserveItem(ReserveItemRequest request, StreamObserver<StatusMessageResponse> responseObserver) {
        if (server.isLeader()) {
            // Act as PRIMARY
            try {
                System.out.println("Reserving item as PRIMARY");
                startDistributedTx(request.getId(), request);
                updateSecondaryServers(request);

                if (validateReserveItemRequest(request)) {
                    ((DistributedTxCoordinator) server.getTransactionReserveItem()).perform();
                } else {
                    ((DistributedTxCoordinator) server.getTransactionReserveItem()).sendGlobalAbort();
                }
            } catch (Exception e) {
                System.out.println("Error while reserving a new item: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Act As SECONDARY
            if (request.getIsSentByPrimary()) {
                System.out.println("Reserving new item on SECONDARY, on PRIMARY's command");
                startDistributedTx(request.getId(), request);

                if (validateReserveItemRequest(request)) {
                    ((DistributedTxParticipant) server.getTransactionReserveItem()).voteCommit();
                } else {
                    ((DistributedTxParticipant) server.getTransactionReserveItem()).voteAbort();
                }
            } else {
                // Call PRIMARY server
                StatusMessageResponse response = callPrimary(request);
                if (response.getStatus() == Status.SUCCESS) {
                    status = Status.SUCCESS;
                }
            }
        }
        StatusMessageResponse response = StatusMessageResponse
                .newBuilder()
                .setStatus(status)
                .setMessage(statusMessage)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    private boolean validateReserveItemRequest(ReserveItemRequest ReserveItemRequest) {
        return ReserveItemRequest.getReservedQuantity() > 0;
    }

    private void ReserveItemToStore() {
        if (tempDataHolder != null) {
            ReserveItemRequest request = tempDataHolder.getValue();
            dataStore.reserveItem(request);
            System.out.println("Item: " + request.getId() + ", Updated in Data Store");

            status = Status.SUCCESS;
            statusMessage = "Item Updated Successfully";
            tempDataHolder = null;
        }
    }

    // On Commit / Abort

    @Override
    public void onGlobalCommit() {
        ReserveItemToStore();
    }

    @Override
    public void onGlobalAbort() {
        tempDataHolder = null;
        status = Status.FAILURE;
        System.out.println("Transaction aborted by the coordinator");
    }

    private StatusMessageResponse callServer(ReserveItemRequest ReserveItemRequest, boolean isSentByPrimary,
                                             String host, int port) {
        System.out.println("Call server: " + host + ":" + port);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        clientStub = ReserveItemServiceGrpc.newBlockingStub(channel);
        ReserveItemRequest request = ReserveItemRequest.toBuilder().setIsSentByPrimary(isSentByPrimary).build();
        return clientStub.reserveItem(request);
    }

    private StatusMessageResponse callPrimary(ReserveItemRequest ReserveItemRequest) {
        System.out.println("Calling primary server...");

        String[] currentLeaderData = server.getCurrentLeaderData();
        String host = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);

        return callServer(ReserveItemRequest, false, host, port);
    }

    private void updateSecondaryServers(ReserveItemRequest ReserveItemRequest) throws KeeperException,
            InterruptedException {
        System.out.println("Updating secondary servers...");
        List<String[]> othersData = server.getOthersData();

        for (String[] data : othersData) {
            String host = data[0];
            int port = Integer.parseInt(data[1]);

            callServer(ReserveItemRequest, true, host, port);
        }
    }

    private void startDistributedTx(String itemId, ReserveItemRequest ReserveItemRequest) {
        try {
            server.getTransactionReserveItem().start(itemId, String.valueOf(UUID.randomUUID()));
            tempDataHolder = new AbstractMap.SimpleEntry<>(itemId, ReserveItemRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
