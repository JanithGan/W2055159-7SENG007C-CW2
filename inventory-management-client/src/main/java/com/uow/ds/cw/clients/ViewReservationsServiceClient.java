package com.uow.ds.cw.clients;

import com.uow.ds.cw.utils.DisplayUtils;
import grpc.generated.*;
import io.grpc.ManagedChannel;

public class ViewReservationsServiceClient extends BaseServiceClient {
    ViewReservationsServiceGrpc.ViewReservationsServiceBlockingStub clientStub = null;

    @Override
    public void initializeConnection(ManagedChannel channel) {
        this.channel = channel;
        clientStub = ViewReservationsServiceGrpc.newBlockingStub(channel);
    }

    public void processUserRequests() throws InterruptedException {
        System.out.println("VIEW RESERVATIONS ...");
        System.out.println("\nReceiving All Reservations");

        ViewReservationsRequest request = ViewReservationsRequest.newBuilder().build();
        ViewReservationsResponse response = clientStub.viewReservations(request);

        DisplayUtils.displayDivider();
        String tableFormatter = "%n| %-38s| %-15s| %-15s| %-15s|%n";
        String divider = "--------------------------------------------------------------------------------------------";

        System.out.printf(tableFormatter, "Reservation Id", "Item Name", "Price (LKR)", "Reserve Amount");

        for (Reservation reservation : response.getReservationsList()) {
            System.out.print(divider);
            System.out.printf(tableFormatter, reservation.getId(), reservation.getItem().getName(),
                    reservation.getItem().getPrice(),
                    reservation.getReservedQuantity());
        }
        System.out.println(divider);
        System.out.println();

        Thread.sleep(1000);
    }
}
