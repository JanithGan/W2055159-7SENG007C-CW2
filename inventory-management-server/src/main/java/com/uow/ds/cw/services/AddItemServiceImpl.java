package com.uow.ds.cw.services;

import com.uow.ds.cw.*;

import grpc.generated.AddItemRequest;
import grpc.generated.AddItemServiceGrpc;
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

public class AddItemServiceImpl extends AddItemServiceGrpc.AddItemServiceImplBase implements DistributedTxListener {
    AddItemServiceGrpc.AddItemServiceBlockingStub clientStub = null;

    private final InventoryManagementServer server;
    private final DataStoreImpl dataStore;

    private Status status = Status.FAILURE;
    private String statusMessage = "";

    private AbstractMap.SimpleEntry<String, AddItemRequest> tempDataHolder;

    public AddItemServiceImpl(InventoryManagementServer inventoryManagementServer, DataStoreImpl dataStore) {
        this.server = inventoryManagementServer;
        this.dataStore = dataStore;
    }

    @Override
    public void addItem(AddItemRequest request, StreamObserver<StatusMessageResponse> responseObserver) {
        if (server.isLeader()) {
            // Act as PRIMARY
            try {
                System.out.println("Adding item as PRIMARY");
                startDistributedTx(request.getId(), request);
                updateSecondaryServers(request);

                if (validateAddItemRequest(request)) {
                    ((DistributedTxCoordinator) server.getTransactionAddItem()).perform();
                } else {
                    ((DistributedTxCoordinator) server.getTransactionAddItem()).sendGlobalAbort();
                }
            } catch (Exception e) {
                System.out.println("Error while adding a new item: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Act As SECONDARY
            if (request.getIsSentByPrimary()) {
                System.out.println("Adding new item on SECONDARY, on PRIMARY's command");
                startDistributedTx(request.getId(), request);

                if (validateAddItemRequest(request)) {
                    ((DistributedTxParticipant) server.getTransactionAddItem()).voteCommit();
                } else {
                    ((DistributedTxParticipant) server.getTransactionAddItem()).voteAbort();
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


    private boolean validateAddItemRequest(AddItemRequest addItemRequest) {
        return addItemRequest.getPrice() > 0 && addItemRequest.getInitialQuantity() > 0;
    }

    private void addItemToStore() {
        if (tempDataHolder != null) {
            AddItemRequest request = tempDataHolder.getValue();
            dataStore.addItem(request);
            System.out.println("Item: " + request.getName() + ", Added to Data Store");

            status = Status.SUCCESS;
            statusMessage = "Item Added Successfully";
            tempDataHolder = null;
        }
    }

    // On Commit / Abort

    @Override
    public void onGlobalCommit() {
        addItemToStore();
    }

    @Override
    public void onGlobalAbort() {
        tempDataHolder = null;
        status = Status.FAILURE;
        System.out.println("Transaction aborted by the coordinator");
    }


    private StatusMessageResponse callServer(AddItemRequest AddItemRequest, boolean isSentByPrimary, String host,
                                             int port) {
        System.out.println("Call server: " + host + ":" + port);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        clientStub = AddItemServiceGrpc.newBlockingStub(channel);
        AddItemRequest request = AddItemRequest.toBuilder().setIsSentByPrimary(isSentByPrimary).build();
        return clientStub.addItem(request);
    }

    private StatusMessageResponse callPrimary(AddItemRequest AddItemRequest) {
        System.out.println("Calling primary server...");

        String[] currentLeaderData = server.getCurrentLeaderData();
        String host = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);

        return callServer(AddItemRequest, false, host, port);
    }

    private void updateSecondaryServers(AddItemRequest AddItemRequest) throws KeeperException, InterruptedException {
        System.out.println("Updating secondary servers...");
        List<String[]> othersData = server.getOthersData();

        for (String[] data : othersData) {
            String host = data[0];
            int port = Integer.parseInt(data[1]);

            callServer(AddItemRequest, true, host, port);
        }
    }

    private void startDistributedTx(String itemId, AddItemRequest AddItemRequest) {
        try {
            server.getTransactionAddItem().start(itemId, String.valueOf(UUID.randomUUID()));
            tempDataHolder = new AbstractMap.SimpleEntry<>(itemId, AddItemRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
