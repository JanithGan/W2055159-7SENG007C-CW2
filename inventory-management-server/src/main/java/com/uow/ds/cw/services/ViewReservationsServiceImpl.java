package com.uow.ds.cw.services;

import com.uow.ds.cw.DataStoreImpl;
import grpc.generated.ViewReservationsRequest;
import grpc.generated.ViewReservationsResponse;
import grpc.generated.ViewReservationsServiceGrpc;
import io.grpc.stub.StreamObserver;

public class ViewReservationsServiceImpl extends ViewReservationsServiceGrpc.ViewReservationsServiceImplBase {
    private final DataStoreImpl dataStore;

    public ViewReservationsServiceImpl(DataStoreImpl dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void viewReservations(ViewReservationsRequest request,
                                 StreamObserver<ViewReservationsResponse> responseObserver) {
        System.out.println("Request to view all reservations received");
        ViewReservationsResponse responseBuilder = ViewReservationsResponse
                .newBuilder()
                .addAllReservations(dataStore.getAllReservations())
                .build();
        responseObserver.onNext(responseBuilder);
        responseObserver.onCompleted();
    }
}
