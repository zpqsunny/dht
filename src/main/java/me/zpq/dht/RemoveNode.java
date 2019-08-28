package me.zpq.dht;

import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;

public class RemoveNode extends TimerTask {

    private Map<byte[], NodeTable> table;

    private long timeout;

    public RemoveNode(Map<byte[], NodeTable> table, long timeout) {

        this.table = table;
        this.timeout = timeout;
    }

    @Override
    public void run() {

        Iterator<Map.Entry<byte[], NodeTable>> iterator = table.entrySet().iterator();
        while (iterator.hasNext()) {

            Map.Entry<byte[], NodeTable> next = iterator.next();
            long diff = System.currentTimeMillis() - next.getValue().getTime();
            if (diff > timeout) {

                iterator.remove();
            }
        }
    }
}
