package com.uow.ds.cw;

import com.uow.ds.cw.clients.*;
import com.uow.ds.cw.utils.DisplayUtils;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;

public class InventoryManagementClient {
    // Server Host
    private static final String host = "127.0.0.1"; // Server IPs are known

    // Clients
    private final AddItemServiceClient addItemClient;
    private final UpdateItemServiceClient updateItemClient;
    private final UpdateItemInventoryServiceClient updateItemInventoryClient;
    private final ViewItemsServiceClient viewItemsClient;
    private final ReserveItemServiceClient reserveItemsClient;
    private final ViewReservationsServiceClient viewReservationsClient;

    public InventoryManagementClient(AddItemServiceClient addItemClient, UpdateItemServiceClient updateItemClient,
                                     UpdateItemInventoryServiceClient updateItemInventoryClient,
                                     ViewItemsServiceClient viewItemsClient,
                                     ReserveItemServiceClient reserveItemClient,
                                     ViewReservationsServiceClient viewReservationsClient) {
        this.addItemClient = addItemClient;
        this.updateItemClient = updateItemClient;
        this.updateItemInventoryClient = updateItemInventoryClient;
        this.viewItemsClient = viewItemsClient;
        this.reserveItemsClient = reserveItemClient;
        this.viewReservationsClient = viewReservationsClient;
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0].trim()); // Server Port

        if (args.length != 1) {
            System.out.println("Server <port> is Not Provided");
            System.exit(1);
        }

        // Initialize Service Clients
        AddItemServiceClient addItemClient = new AddItemServiceClient();
        UpdateItemServiceClient updateItemClient = new UpdateItemServiceClient();
        UpdateItemInventoryServiceClient updateItemInventoryClient = new UpdateItemInventoryServiceClient();
        ViewItemsServiceClient viewItemsClient = new ViewItemsServiceClient();
        ReserveItemServiceClient reserveItemClient = new ReserveItemServiceClient();
        ViewReservationsServiceClient viewReservationsClient = new ViewReservationsServiceClient();

        // Initialize Client
        InventoryManagementClient inventoryManagementClient = new InventoryManagementClient(addItemClient,
                updateItemClient, updateItemInventoryClient, viewItemsClient, reserveItemClient,
                viewReservationsClient);

        // GRPC Channel Initialization
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        inventoryManagementClient.initializeConnection(channel);

        // Start Client
        inventoryManagementClient.startProcessUserRequests();
        System.out.println("Initializing Connecting to server at:" + "localhost:" + port);

        // Stop Client
        inventoryManagementClient.closeConnection();
    }

    private void startProcessUserRequests() {
        Scanner userInput = new Scanner(System.in);

        DisplayUtils.displayInitMessage();
        while (true) {
            DisplayUtils.displayMenu();
            String userChoice = userInput.nextLine();
            try {
                int choice = Integer.parseInt(userChoice);
                DisplayUtils.displayDivider();
                switch (choice) {
                    case 1:
                        addItemClient.processUserRequests(userInput);
                        break;
                    case 2:
                        updateItemClient.processUserRequests(userInput);
                        break;
                    case 3:
                        updateItemInventoryClient.processUserRequests(userInput);
                        break;
                    case 4:
                        viewItemsClient.processUserRequests();
                        break;
                    case 5:
                        reserveItemsClient.processUserRequests(userInput);
                        break;
                    case 6:
                        viewReservationsClient.processUserRequests();
                        break;
                    case 7:
                        DisplayUtils.displayCloseMessage();
                        System.exit(0);
                    default:
                        System.out.println("INVALID CHOICE ...");
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e);
            }
        }
    }

    private void initializeConnection(ManagedChannel channel) {
        addItemClient.initializeConnection(channel);
        updateItemClient.initializeConnection(channel);
        updateItemInventoryClient.initializeConnection(channel);
        viewItemsClient.initializeConnection(channel);
        reserveItemsClient.initializeConnection(channel);
        viewReservationsClient.initializeConnection(channel);
    }

    private void closeConnection() {
        addItemClient.closeConnection();
        updateItemClient.closeConnection();
        updateItemInventoryClient.closeConnection();
        viewItemsClient.closeConnection();
        reserveItemsClient.closeConnection();
        viewReservationsClient.closeConnection();
    }
}
