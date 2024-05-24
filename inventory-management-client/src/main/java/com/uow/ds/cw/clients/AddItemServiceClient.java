package com.uow.ds.cw.clients;

import grpc.generated.AddItemRequest;
import grpc.generated.AddItemServiceGrpc;
import grpc.generated.StatusMessageResponse;
import io.grpc.ManagedChannel;

import java.util.Scanner;
import java.util.UUID;


public class AddItemServiceClient extends BaseServiceClient {
    AddItemServiceGrpc.AddItemServiceBlockingStub clientStub = null;

    @Override
    public void initializeConnection(ManagedChannel channel) {
        this.channel = channel;
        clientStub = AddItemServiceGrpc.newBlockingStub(channel);
    }

    public void processUserRequests(Scanner userInput) throws InterruptedException {
        System.out.println("ADD ITEM ...");
        System.out.print("\nEnter [Item Name, Price, Initial Quantity]: ");

        // Get parameters from input
        String[] input = userInput.nextLine().trim().split(",");
        String itemName = input[0];
        double price = Double.parseDouble(input[1]);
        int availableQuantity = Integer.parseInt(input[2]);

        System.out.println("Sending request... AddItem | Name: " + itemName);

        AddItemRequest request = AddItemRequest
                .newBuilder()
                .setId(String.valueOf(UUID.randomUUID()))
                .setName(itemName)
                .setPrice(price)
                .setInitialQuantity(availableQuantity)
                .build();

        StatusMessageResponse response = clientStub.addItem(request);
        System.out.println("AddItem: " + response.getStatus() + ". " + response.getMessage());
        Thread.sleep(1000);
    }
}
