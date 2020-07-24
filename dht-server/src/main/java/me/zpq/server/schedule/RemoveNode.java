package me.zpq.server.schedule;

import me.zpq.route.IRoutingTable;
import me.zpq.route.NodeTable;

import java.util.Collection;

public class RemoveNode implements Runnable {

    private final IRoutingTable routingTable;

    private final long NODE_EXPIRATION_TIME = 15 * 60;

    public RemoveNode(IRoutingTable routingTable) {

        this.routingTable = routingTable;
    }

    @Override
    public void run() {

        Collection<NodeTable> values = routingTable.values();

        values.forEach(nodeTable -> {

            long diff = System.currentTimeMillis() - nodeTable.getLastChanged();
            if (diff > NODE_EXPIRATION_TIME) {

                routingTable.remove(nodeTable.getId());
            }
        });
    }
}
