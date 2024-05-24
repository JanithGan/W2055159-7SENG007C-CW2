package com.uow.ds.cw.clients;

import grpc.generated.ReserveItemRequest;
import grpc.generated.ReserveItemServiceGrpc;
import grpc.generated.StatusMessageResponse;
import io.grpc.ManagedChannel;

import java.util.Scanner;
import java.util.UUID;


public class ReserveItemServiceClient extends BaseServiceClient {
    ReserveItemServiceGrpc.ReserveItemServiceBlockingStub clientStub = null;

    @Override
    public void initializeConnection(ManagedChannel channel) {
        this.channel = channel;
        clientStub = ReserveItemServiceGrpc.newBlockingStub(channel);
    }

    public void processUserRequests(Scanner userInput) throws InterruptedException {
        System.out.println("RESERVE ITEM ...");
        System.out.print("\nEnter [Item Id, Reserved Quantity]: ");

        // Get parameters from input
        String[] input = userInput.nextLine().trim().split(",");
        String itemId = input[0];
        int reservedQuantity = Integer.parseInt(input[1]);

        System.out.println("Sending request... ReserveItem | Id: " + itemId);

        ReserveItemRequest request = ReserveItemRequest
                .newBuilder()
                .setId(String.valueOf(UUID.randomUUID()))
                .setItemId(itemId)
                .setReservedQuantity(reservedQuantity)
                .build();

        StatusMessageResponse response = clientStub.reserveItem(request);
        System.out.println("ReserveItem: " + response.getStatus() + ". " + response.getMessage());
        Thread.sleep(1000);
    }
}
