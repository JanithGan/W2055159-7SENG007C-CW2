package com.uow.ds.cw;

public interface DistributedTxListener {
    void onGlobalCommit();
    void onGlobalAbort();
}
