package com.uow.ds.cw.clients;

import grpc.generated.StatusMessageResponse;
import grpc.generated.UpdateItemInventoryRequest;
import grpc.generated.UpdateItemInventoryServiceGrpc;
import io.grpc.ManagedChannel;

import java.util.Scanner;


public class UpdateItemInventoryServiceClient extends BaseServiceClient {
    UpdateItemInventoryServiceGrpc.UpdateItemInventoryServiceBlockingStub clientStub = null;

    @Override
    public void initializeConnection(ManagedChannel channel) {
        this.channel = channel;
        clientStub = UpdateItemInventoryServiceGrpc.newBlockingStub(channel);
    }

    public void processUserRequests(Scanner userInput) throws InterruptedException {
        System.out.println("UPDATE ITEM INVENTORY ...");
        System.out.print("\nEnter [Item Id, Added Quantity]: ");

        // Get parameters from input
        String[] input = userInput.nextLine().trim().split(",");
        String id = input[0];
        int addedQuantity = Integer.parseInt(input[1]);

        System.out.println("Sending request... UpdateItemInventory | Id: " + id);

        UpdateItemInventoryRequest request = UpdateItemInventoryRequest
                .newBuilder()
                .setId(id)
                .setAddedQuantity(addedQuantity)
                .build();

        StatusMessageResponse response = clientStub.updateItemInventory(request);
        System.out.println("UpdateItem: " + response.getStatus() + ". " + response.getMessage());
        Thread.sleep(1000);
    }
}
