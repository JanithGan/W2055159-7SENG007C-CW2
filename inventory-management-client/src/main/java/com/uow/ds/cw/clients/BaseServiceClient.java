package com.uow.ds.cw.clients;

import io.grpc.ManagedChannel;

import java.util.Scanner;

public abstract class BaseServiceClient {
    ManagedChannel channel = null;

    public abstract void initializeConnection(ManagedChannel channel);

    public void closeConnection() {
        channel.shutdown();
    }
}
