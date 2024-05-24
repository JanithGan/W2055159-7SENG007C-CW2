package com.uow.ds.cw.services;

import com.uow.ds.cw.*;
import grpc.generated.UpdateItemRequest;
import grpc.generated.UpdateItemServiceGrpc;
import grpc.generated.Status;
import grpc.generated.StatusMessageResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.UUID;

public class UpdateItemServiceImpl extends UpdateItemServiceGrpc.UpdateItemServiceImplBase implements DistributedTxListener {
    UpdateItemServiceGrpc.UpdateItemServiceBlockingStub clientStub = null;

    private final InventoryManagementServer server;
    private final DataStoreImpl dataStore;

    private Status status = Status.FAILURE;
    private String statusMessage = "";

    private AbstractMap.SimpleEntry<String, UpdateItemRequest> tempDataHolder;

    public UpdateItemServiceImpl(InventoryManagementServer inventoryManagementServer, DataStoreImpl dataStore) {
        this.server = inventoryManagementServer;
        this.dataStore = dataStore;
    }

    @Override
    public void updateItem(UpdateItemRequest request, StreamObserver<StatusMessageResponse> responseObserver) {
        if (server.isLeader()) {
            // Act as PRIMARY
            try {
                System.out.println("Updating item as PRIMARY");
                startDistributedTx(request.getId(), request);
                updateSecondaryServers(request);

                if (validateUpdateItemRequest(request)) {
                    ((DistributedTxCoordinator) server.getTransactionUpdateItem()).perform();
                } else {
                    ((DistributedTxCoordinator) server.getTransactionUpdateItem()).sendGlobalAbort();
                }
            } catch (Exception e) {
                System.out.println("Error while updating item: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Act As SECONDARY
            if (request.getIsSentByPrimary()) {
                System.out.println("Updating item on SECONDARY, on PRIMARY's command");
                startDistributedTx(request.getId(), request);

                if (validateUpdateItemRequest(request)) {
                    ((DistributedTxParticipant) server.getTransactionUpdateItem()).voteCommit();
                } else {
                    ((DistributedTxParticipant) server.getTransactionUpdateItem()).voteAbort();
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


    private boolean validateUpdateItemRequest(UpdateItemRequest UpdateItemRequest) {
        return UpdateItemRequest.getPrice() > 0;
    }

    private void updateItemToStore() {
        if (tempDataHolder != null) {
            UpdateItemRequest request = tempDataHolder.getValue();
            dataStore.updateItem(request);
            System.out.println("Item: " + request.getId() + ", Updated in Data Store");

            status = Status.SUCCESS;
            statusMessage = "Item Updated Successfully";
            tempDataHolder = null;
        }
    }

    // On Commit / Abort

    @Override
    public void onGlobalCommit() {
        updateItemToStore();
    }

    @Override
    public void onGlobalAbort() {
        tempDataHolder = null;
        status = Status.FAILURE;
        System.out.println("Transaction aborted by the coordinator");
    }

    private StatusMessageResponse callServer(UpdateItemRequest UpdateItemRequest, boolean isSentByPrimary,
                                             String host, int port) {
        System.out.println("Call server: " + host + ":" + port);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        clientStub = UpdateItemServiceGrpc.newBlockingStub(channel);
        UpdateItemRequest request = UpdateItemRequest.toBuilder().setIsSentByPrimary(isSentByPrimary).build();
        return clientStub.updateItem(request);
    }

    private StatusMessageResponse callPrimary(UpdateItemRequest UpdateItemRequest) {
        System.out.println("Calling primary server...");

        String[] currentLeaderData = server.getCurrentLeaderData();
        String host = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);

        return callServer(UpdateItemRequest, false, host, port);
    }

    private void updateSecondaryServers(UpdateItemRequest UpdateItemRequest) throws KeeperException,
            InterruptedException {
        System.out.println("Updating secondary servers...");
        List<String[]> othersData = server.getOthersData();

        for (String[] data : othersData) {
            String host = data[0];
            int port = Integer.parseInt(data[1]);

            callServer(UpdateItemRequest, true, host, port);
        }
    }

    private void startDistributedTx(String itemId, UpdateItemRequest UpdateItemRequest) {
        try {
            server.getTransactionUpdateItem().start(itemId, String.valueOf(UUID.randomUUID()));
            tempDataHolder = new AbstractMap.SimpleEntry<>(itemId, UpdateItemRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
