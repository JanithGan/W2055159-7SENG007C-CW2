package com.uow.ds.cw.services;

import com.uow.ds.cw.*;
import grpc.generated.Status;
import grpc.generated.StatusMessageResponse;
import grpc.generated.UpdateItemInventoryRequest;
import grpc.generated.UpdateItemInventoryServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.List;
import java.util.UUID;

public class UpdateItemInventoryServiceImpl extends UpdateItemInventoryServiceGrpc.UpdateItemInventoryServiceImplBase implements DistributedTxListener {
    UpdateItemInventoryServiceGrpc.UpdateItemInventoryServiceBlockingStub clientStub = null;

    private final InventoryManagementServer server;
    private final DataStoreImpl dataStore;

    private Status status = Status.FAILURE;
    private String statusMessage = "";

    private AbstractMap.SimpleEntry<String, UpdateItemInventoryRequest> tempDataHolder;

    public UpdateItemInventoryServiceImpl(InventoryManagementServer inventoryManagementServer,
                                          DataStoreImpl dataStore) {
        this.server = inventoryManagementServer;
        this.dataStore = dataStore;
    }

    @Override
    public void updateItemInventory(UpdateItemInventoryRequest request,
                                    StreamObserver<StatusMessageResponse> responseObserver) {
        if (server.isLeader()) {
            // Act as PRIMARY
            try {
                System.out.println("Updating item inventory as PRIMARY");
                startDistributedTx(request.getId(), request);
                updateSecondaryServers(request);

                if (validateUpdateItemInventoryRequest(request)) {
                    ((DistributedTxCoordinator) server.getTransactionUpdateItemInventory()).perform();
                } else {
                    ((DistributedTxCoordinator) server.getTransactionUpdateItemInventory()).sendGlobalAbort();
                }
            } catch (Exception e) {
                System.out.println("Error while updating item inventory a new item: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Act As SECONDARY
            if (request.getIsSentByPrimary()) {
                System.out.println("Updating item inventory on SECONDARY, on PRIMARY's command");
                startDistributedTx(request.getId(), request);

                if (validateUpdateItemInventoryRequest(request)) {
                    ((DistributedTxParticipant) server.getTransactionUpdateItemInventory()).voteCommit();
                } else {
                    ((DistributedTxParticipant) server.getTransactionUpdateItemInventory()).voteAbort();
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


    private boolean validateUpdateItemInventoryRequest(UpdateItemInventoryRequest UpdateItemInventoryRequest) {
        return UpdateItemInventoryRequest.getAddedQuantity() > 0;
    }

    private void UpdateItemInventoryToStore() {
        if (tempDataHolder != null) {
            UpdateItemInventoryRequest request = tempDataHolder.getValue();
            dataStore.updateItemInventory(request);
            System.out.println("Item: " + request.getId() + ", Updated Inventory in Data Store");

            status = Status.SUCCESS;
            statusMessage = "Item Inventory Updated Successfully";
            tempDataHolder = null;
        }
    }

    // On Commit / Abort

    @Override
    public void onGlobalCommit() {
        UpdateItemInventoryToStore();
    }

    @Override
    public void onGlobalAbort() {
        tempDataHolder = null;
        status = Status.FAILURE;
        System.out.println("Transaction aborted by the coordinator");
    }

    private StatusMessageResponse callServer(UpdateItemInventoryRequest UpdateItemInventoryRequest,
                                             boolean isSentByPrimary, String host, int port) {
        System.out.println("Call server: " + host + ":" + port);
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();

        clientStub = UpdateItemInventoryServiceGrpc.newBlockingStub(channel);
        UpdateItemInventoryRequest request =
                UpdateItemInventoryRequest.toBuilder().setIsSentByPrimary(isSentByPrimary).build();
        return clientStub.updateItemInventory(request);
    }

    private StatusMessageResponse callPrimary(UpdateItemInventoryRequest UpdateItemInventoryRequest) {
        System.out.println("Calling primary server...");

        String[] currentLeaderData = server.getCurrentLeaderData();
        String host = currentLeaderData[0];
        int port = Integer.parseInt(currentLeaderData[1]);

        return callServer(UpdateItemInventoryRequest, false, host, port);
    }

    private void updateSecondaryServers(UpdateItemInventoryRequest UpdateItemInventoryRequest) throws KeeperException
            , InterruptedException {
        System.out.println("Updating secondary servers...");
        List<String[]> othersData = server.getOthersData();

        for (String[] data : othersData) {
            String host = data[0];
            int port = Integer.parseInt(data[1]);

            callServer(UpdateItemInventoryRequest, true, host, port);
        }
    }

    private void startDistributedTx(String itemId, UpdateItemInventoryRequest UpdateItemInventoryRequest) {
        try {
            server.getTransactionUpdateItemInventory().start(itemId, String.valueOf(UUID.randomUUID()));
            tempDataHolder = new AbstractMap.SimpleEntry<>(itemId, UpdateItemInventoryRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
