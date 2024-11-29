package me.zpq.server;

import lombok.extern.slf4j.Slf4j;
import me.zpq.dht.common.MemoryQueue;
import me.zpq.dht.common.PeerNode;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class MemoryQueueImpl implements MemoryQueue {

    private final Set<String> hashSet = new HashSet<>();

    private final Queue<PeerNode> peerNodeQueue = new LinkedBlockingQueue<>();

    @Override
    public void leftPush(PeerNode peerNode) {

        if (hashSet.contains(peerNode.hash())) {
            return;
        }
        hashSet.add(peerNode.hash());
        peerNodeQueue.add(peerNode);
    }

    @Override
    public PeerNode rightPop() {

        PeerNode peerNode = peerNodeQueue.poll();
        if (peerNode == null) {
            return null;
        }
        hashSet.remove(peerNode.hash());
        return peerNode;
    }

    @Override
    public int size() {

        return hashSet.size();
    }
}
