package me.zpq.route;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zpq
 * @date 2020/7/23
 */
public class RoutingTable implements IRoutingTable {

    private final Map<String, NodeTable> table;

    public RoutingTable() {

        this.table = new ConcurrentHashMap<>(16);
    }

    @Override
    public void put(NodeTable nodeTable) {

        table.put(nodeTable.getId(), nodeTable);
    }

    @Override
    public NodeTable get(String id) {

        return table.get(id);
    }

    @Override
    public NodeTable replace(NodeTable nodeTable) {

        return table.replace(nodeTable.getId(), nodeTable);
    }

    @Override
    public boolean has(String id) {

        return table.containsKey(id);
    }

    @Override
    public Collection<NodeTable> values() {

        return table.values();
    }

    @Override
    public void remove(String id) {

        table.remove(id);
    }

    @Override
    public void clear() {

        table.clear();
    }

    @Override
    public int size() {

        return table.size();
    }
}
