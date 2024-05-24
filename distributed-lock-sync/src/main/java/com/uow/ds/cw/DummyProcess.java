package com.uow.ds.cw;

import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class DummyProcess {
    public static final String ZOOKEEPER_URL = "localhost:2181";
    public static final String LOCK_NAME = "ResourcesLock";

    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public static void main(String[] args) {
        DistributedLock.setZooKeeperURL(ZOOKEEPER_URL);
        try {
            DistributedLock lock = new DistributedLock("ResourceLock", "DummyProcess");

            // Acquire the lock
            lock.acquireLock();
            System.out.println("Acquired the lock at: " + getCurrentTimeStamp());

            // Access critical section
            accessSharedResource();

            // Release the lock
            lock.releaseLock();
            System.out.println("Releasing the lock at: " + getCurrentTimeStamp());
        } catch (IOException | KeeperException | InterruptedException e) {
            System.out.printf("Error while creating the lock %s: %s\n", LOCK_NAME, e.getMessage());
            e.printStackTrace();
        }
    }

    private static void accessSharedResource() throws InterruptedException {
        long sleepDuration = Math.abs(new Random().nextInt() % 20);
        System.out.println("Accessing critical section. Time remaining: " + sleepDuration + " seconds.");
        Thread.sleep(sleepDuration * 1000);
    }

    private static String getCurrentTimeStamp() {
        return timeFormat.format(new Date(System.currentTimeMillis()));
    }
}
