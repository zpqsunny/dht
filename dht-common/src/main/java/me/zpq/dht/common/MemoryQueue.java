package me.zpq.dht.common;

public interface MemoryQueue {

    void leftPush(PeerNode peerNode);

    PeerNode rightPop();

    int size();
}
