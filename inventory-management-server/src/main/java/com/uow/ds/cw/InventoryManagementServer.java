package com.uow.ds.cw;

import com.uow.ds.cw.services.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class InventoryManagementServer {
    private static final String LOCALHOST = "localhost";
    public static final String ZOOKEEPER_URL = LOCALHOST + ":2181";
    private final int serverPort;

    // Leader lock
    private final DistributedLock leaderLock;
    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    private byte[] leaderData;

    // Services
    private final AddItemServiceImpl addItemService;
    private final UpdateItemServiceImpl updateItemService;
    private final UpdateItemInventoryServiceImpl updateItemInventoryService;
    private final ViewItemsServiceImpl viewItemsService;
    private final ReserveItemServiceImpl reserveItemService;
    private final ViewReservationsServiceImpl viewReservationsService;

    // Transaction Participants
    private DistributedTx transactionAddItem;
    private DistributedTx transactionUpdateItem;
    private DistributedTx transactionUpdateItemInventory;
    private DistributedTx transactionReserveItem;

    public InventoryManagementServer(String host, int port) throws InterruptedException, IOException, KeeperException {
        this.serverPort = port;
        leaderLock = new DistributedLock("InventoryManagementServerNodes", host + ":" + port);

        // Initialize Data Store
        DataStoreImpl dataStore = new DataStoreImpl();

        // Initialize Services
        addItemService = new AddItemServiceImpl(this, dataStore);
        updateItemService = new UpdateItemServiceImpl(this, dataStore);
        updateItemInventoryService = new UpdateItemInventoryServiceImpl(this, dataStore);
        viewItemsService = new ViewItemsServiceImpl(dataStore);
        reserveItemService = new ReserveItemServiceImpl(this, dataStore);
        viewReservationsService = new ViewReservationsServiceImpl(dataStore);

        // Initialize Transaction Participants
        transactionAddItem = new DistributedTxParticipant(addItemService);
        transactionUpdateItem = new DistributedTxParticipant(updateItemService);
        transactionUpdateItemInventory = new DistributedTxParticipant(updateItemInventoryService);
        transactionReserveItem = new DistributedTxParticipant(reserveItemService);
    }

    public static void main(String[] args) throws Exception {
        Logger.getLogger("org.apache.zookeeper").setLevel(Level.ERROR);

        if (args.length != 1) {
            System.out.println("<port> is Not Provided");
        }

        int serverPort = Integer.parseInt(args[0]); // Get server port from inputs

        DistributedLock.setZooKeeperURL(ZOOKEEPER_URL);
        DistributedTx.setZooKeeperURL(ZOOKEEPER_URL);

        InventoryManagementServer server = new InventoryManagementServer(LOCALHOST, serverPort);
        server.startServer();
    }

    public void startServer() throws IOException, InterruptedException {
        Server server = ServerBuilder
                .forPort(serverPort)
                .addService(addItemService)
                .addService(updateItemService)
                .addService(updateItemInventoryService)
                .addService(viewItemsService)
                .addService(reserveItemService)
                .addService(viewReservationsService)
                .build();

        server.start();
        System.out.println("InventoryManagementServer started on port: " + serverPort);

        tryToBeLeader();
        server.awaitTermination();
    }

    // Leader Elections
    class LeaderCampaignThread implements Runnable {
        private byte[] currentLeaderData = null;

        @Override
        public void run() {
            System.out.println("Starting the leader campaign...");
            try {
                boolean leader = leaderLock.tryAcquireLock();

                // Trying to get the leader lock
                while (!leader) {
                    byte[] leaderData = leaderLock.getLockHolderData();

                    if (currentLeaderData != leaderData) {
                        currentLeaderData = leaderData;
                        setCurrentLeaderData(currentLeaderData);
                    }

                    Thread.sleep(10000);

                    leader = leaderLock.tryAcquireLock();
                }

                beTheLeader();
                currentLeaderData = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isLeader() {
        return isLeader.get();
    }

    private synchronized void setCurrentLeaderData(byte[] leaderData) {
        this.leaderData = leaderData;
    }

    public synchronized String[] getCurrentLeaderData() {
        return new String(leaderData).split(":");
    }

    public List<String[]> getOthersData() throws KeeperException, InterruptedException {
        List<String[]> result = new ArrayList<>();
        List<byte[]> othersData = leaderLock.getOthersData();

        for (byte[] data : othersData) {
            String[] dataStrings = new String(data).split(":");
            result.add(dataStrings);
        }

        return result;
    }

    private void tryToBeLeader() {
        Thread leaderCampaignThread = new Thread(new LeaderCampaignThread());
        leaderCampaignThread.start();
    }

    private void beTheLeader() {
        System.out.println("Acquired the leader lock. Now acting as PRIMARY!");
        isLeader.set(true);

        transactionAddItem = new DistributedTxCoordinator(addItemService);
        transactionUpdateItem = new DistributedTxCoordinator(updateItemService);
        transactionUpdateItemInventory = new DistributedTxCoordinator(updateItemInventoryService);
        transactionReserveItem = new DistributedTxCoordinator(reserveItemService);
    }

    public DistributedTx getTransactionAddItem() {
        return transactionAddItem;
    }

    public DistributedTx getTransactionUpdateItem() {
        return transactionUpdateItem;
    }

    public DistributedTx getTransactionUpdateItemInventory() {
        return transactionUpdateItemInventory;
    }

    public DistributedTx getTransactionReserveItem() {
        return transactionReserveItem;
    }
}
