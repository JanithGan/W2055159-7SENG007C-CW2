package com.uow.ds.cw;

import grpc.generated.*;

import java.util.Collection;

public interface DataStore {
    void addItem(AddItemRequest addItemRequest);

    void updateItem(UpdateItemRequest updateItemRequest);

    void updateItemInventory(UpdateItemInventoryRequest updateItemInventoryRequest);

    Collection<Item> getAllItems();

    void reserveItem(ReserveItemRequest reserveItemRequest);

    Collection<Reservation> getAllReservations();
}
