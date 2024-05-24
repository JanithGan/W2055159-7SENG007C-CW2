package com.uow.ds.cw.services;

import com.uow.ds.cw.DataStoreImpl;
import grpc.generated.ViewItemsRequest;
import grpc.generated.ViewItemsResponse;
import grpc.generated.ViewItemsServiceGrpc;
import io.grpc.stub.StreamObserver;

public class ViewItemsServiceImpl extends ViewItemsServiceGrpc.ViewItemsServiceImplBase {
    private final DataStoreImpl dataStore;

    public ViewItemsServiceImpl(DataStoreImpl dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void viewItems(ViewItemsRequest request, StreamObserver<ViewItemsResponse> responseObserver) {
        System.out.println("Request to view all items received");
        ViewItemsResponse responseBuilder = ViewItemsResponse
                .newBuilder()
                .addAllItems(dataStore.getAllItems())
                .build();
        responseObserver.onNext(responseBuilder);
        responseObserver.onCompleted();
    }
}
