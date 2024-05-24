package com.uow.ds.cw;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import java.io.IOException;

public abstract class DistributedTx implements Watcher {
    public static final String VOTE_COMMIT = "vote_commit";
    public static final String VOTE_ABORT = "vote_abort";
    public static final String GLOBAL_COMMIT = "global_commit";
    public static final String GLOBAL_ABORT = "global_abort";

    ZooKeeperClient client;
    static String zooKeeperUrl;

    DistributedTxListener listener;
    String currentTransaction;

    public static void setZooKeeperURL(String url){
        zooKeeperUrl = url;
    }

    public DistributedTx(DistributedTxListener listener){
        this.listener = listener;
    }

    public void start(String transactionId, String participantId) throws IOException {
        client = new ZooKeeperClient(zooKeeperUrl, 5000, this);
        onStartTransaction(transactionId, participantId);
    }

    abstract void onStartTransaction(String transactionId, String participantId);

    @Override
    public void process(WatchedEvent watchedEvent) { }
}