package com.uow.ds.cw.clients;

import grpc.generated.StatusMessageResponse;
import grpc.generated.UpdateItemRequest;
import grpc.generated.UpdateItemServiceGrpc;
import io.grpc.ManagedChannel;

import java.util.Scanner;


public class UpdateItemServiceClient extends BaseServiceClient {
    UpdateItemServiceGrpc.UpdateItemServiceBlockingStub clientStub = null;

    @Override
    public void initializeConnection(ManagedChannel channel) {
        this.channel = channel;
        clientStub = UpdateItemServiceGrpc.newBlockingStub(channel);
    }

    public void processUserRequests(Scanner userInput) throws InterruptedException {
        System.out.println("UPDATE ITEM ...");
        System.out.print("\nEnter [Item Id, Price]: ");

        // Get parameters from input
        String[] input = userInput.nextLine().trim().split(",");
        String id = input[0];
        double price = Double.parseDouble(input[1]);

        System.out.println("Sending request... UpdateItem | Id: " + id);

        UpdateItemRequest request = UpdateItemRequest
                .newBuilder()
                .setId(id)
                .setPrice(price)
                .build();

        StatusMessageResponse response = clientStub.updateItem(request);
        System.out.println("UpdateItem: " + response.getStatus() + ". " + response.getMessage());
        Thread.sleep(1000);
    }
}
