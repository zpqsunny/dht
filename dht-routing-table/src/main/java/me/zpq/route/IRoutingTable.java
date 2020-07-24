package me.zpq.route;

import java.util.Collection;

/**
 * @author zpq
 * @date 2020/7/23
 */
public interface IRoutingTable {

    void put(NodeTable nodeTable);

    NodeTable get(String id);

    NodeTable replace(NodeTable nodeTable);

    boolean has(String id);

    Collection<NodeTable> values();

    void remove(String id);

    void clear();

    int size();
}
