package com.uow.ds.cw.clients;

import com.uow.ds.cw.utils.DisplayUtils;
import grpc.generated.Item;
import grpc.generated.ViewItemsRequest;
import grpc.generated.ViewItemsResponse;
import grpc.generated.ViewItemsServiceGrpc;
import io.grpc.ManagedChannel;

public class ViewItemsServiceClient extends BaseServiceClient {
    ViewItemsServiceGrpc.ViewItemsServiceBlockingStub clientStub = null;

    @Override
    public void initializeConnection(ManagedChannel channel) {
        this.channel = channel;
        clientStub = ViewItemsServiceGrpc.newBlockingStub(channel);
    }

    public void processUserRequests() throws InterruptedException {
        System.out.println("VIEW ITEMS ...");
        System.out.println("\nReceiving All Items");

        ViewItemsRequest request = ViewItemsRequest.newBuilder().build();
        ViewItemsResponse response = clientStub.viewItems(request);

        DisplayUtils.displayDivider();
        String tableFormatter = "%n| %-38s| %-15s| %-15s| %-15s|%n";
        String divider = "--------------------------------------------------------------------------------------------";

        System.out.printf(tableFormatter, "Item Id", "Item Name", "Price (LKR)", "Inventory");

        for (Item item : response.getItemsList()) {
            System.out.print(divider);
            System.out.printf(tableFormatter, item.getId(), item.getName(), item.getPrice(),
                    item.getAvailableQuantity());
        }
        System.out.println(divider);
        System.out.println();

        Thread.sleep(1000);
    }
}
