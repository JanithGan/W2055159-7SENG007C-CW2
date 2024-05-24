package com.uow.ds.cw;

import grpc.generated.*;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class DataStoreImpl implements DataStore {
    private final ConcurrentHashMap<String, Item> items = new ConcurrentHashMap<String, Item>();
    private final ConcurrentHashMap<String, Reservation> reservations = new ConcurrentHashMap<String, Reservation>();

    @Override
    public void addItem(AddItemRequest addItemRequest) {
        Item item = Item.newBuilder()
                .setId(addItemRequest.getId())
                .setName(addItemRequest.getName())
                .setPrice(addItemRequest.getPrice())
                .setAvailableQuantity(addItemRequest.getInitialQuantity())
                .build();
        items.put(addItemRequest.getId(), item);
    }

    @Override
    public void updateItem(UpdateItemRequest updateItemRequest) throws IllegalArgumentException {
        Item item = items.get(updateItemRequest.getId());

        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }
        System.out.println();

        Item updatedItem = item.toBuilder().setPrice(updateItemRequest.getPrice()).build();
        items.put(updatedItem.getId(), updatedItem);
    }

    @Override
    public void updateItemInventory(UpdateItemInventoryRequest updateItemInventoryRequest) throws IllegalArgumentException {
        Item item = items.get(updateItemInventoryRequest.getId());

        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }

        Item updatedItem =
                item.toBuilder().setAvailableQuantity(item.getAvailableQuantity() + updateItemInventoryRequest.getAddedQuantity()).build();
        items.put(updatedItem.getId(), updatedItem);
    }

    @Override
    public Collection<Item> getAllItems() {
        return items.values();
    }

    @Override
    public void reserveItem(ReserveItemRequest reserveItemRequest) throws IllegalArgumentException {
        Item item = items.get(reserveItemRequest.getItemId());

        if (item == null) {
            throw new IllegalArgumentException("Item not found");
        }

        // Get remaining
        int remainingQuantity = item.getAvailableQuantity() - reserveItemRequest.getReservedQuantity();
        if (remainingQuantity < 0) {
            throw new IllegalArgumentException("Cannot reserve item due to availability");
        }

        // Update item
        if (remainingQuantity == 0) {
            items.remove(item.getId());
        } else {
            Item updatedItem = item.toBuilder().setAvailableQuantity(remainingQuantity).build();
            items.put(updatedItem.getId(), updatedItem);
        }

        Reservation reservation = Reservation.newBuilder()
                .setId(reserveItemRequest.getId())
                .setItem(item)
                .setReservedQuantity(reserveItemRequest.getReservedQuantity())
                .build();

        reservations.put(reserveItemRequest.getId(), reservation);
    }

    @Override
    public Collection<Reservation> getAllReservations() {
        return reservations.values();
    }
}
