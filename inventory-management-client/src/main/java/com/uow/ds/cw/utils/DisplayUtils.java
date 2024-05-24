package com.uow.ds.cw.utils;

public class DisplayUtils {
    public static void displayMenu() {
        System.out.println();
        displayDivider();
        System.out.println("--- PLEASE SELECT AN OPTION TO CONTINUE ---");
        displayDivider();
        System.out.println("1 -> Add Item");
        System.out.println("2 -> Update Item");
        System.out.println("3 -> Update Item Inventory");
        System.out.println("4 -> View Items");
        System.out.println("5 -> Reserve Item");
        System.out.println("6 -> View Reservations");
        System.out.println("7 -> Exit");
        displayDivider();
        System.out.print("ENTER YOUR CHOICE: ");
    }

    public static void displayInitMessage() {
        displayDivider();
        System.out.println("--- INVENTORY MANAGEMENT CLIENT STARTED ---");
        displayDivider();
    }

    public static void displayCloseMessage() {
        displayDivider();
        System.out.println("--- INVENTORY MANAGEMENT CLIENT STOPPED ---");
        displayDivider();
    }

    public static void displayDivider() {
        System.out.println("-------------------------------------------");
    }
}
