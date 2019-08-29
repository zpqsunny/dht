package me.zpq.dht;

import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;

public class RemoveNode extends TimerTask {

    private Map<String, NodeTable> table;

    private long timeout;

    public RemoveNode(Map<String, NodeTable> table, long timeout) {

        this.table = table;
        this.timeout = timeout;
    }

    @Override
    public void run() {

        Iterator<Map.Entry<String, NodeTable>> iterator = table.entrySet().iterator();
        while (iterator.hasNext()) {

            Map.Entry<String, NodeTable> next = iterator.next();
            long diff = System.currentTimeMillis() - next.getValue().getTime();
            if (diff > timeout) {

                iterator.remove();
            }
        }
    }
}
