package me.zpq.server.test;

import lombok.extern.slf4j.Slf4j;
import me.zpq.dht.common.PeerNode;
import me.zpq.server.MemoryQueueImpl;
import org.junit.Test;

/**
 * @author zpq
 * @date 2020/7/23
 */
@Slf4j
public class ServerTest {

    @Test
    public void test() {
        MemoryQueueImpl memoryQueue = new MemoryQueueImpl();
        memoryQueue.leftPush(new PeerNode("a","1",1,System.currentTimeMillis()));
        memoryQueue.leftPush(new PeerNode("b","2",2,System.currentTimeMillis()));
        memoryQueue.leftPush(new PeerNode("c","3",3,System.currentTimeMillis()));
        memoryQueue.leftPush(new PeerNode("c","3",3,System.currentTimeMillis()));
        memoryQueue.leftPush(new PeerNode("d","4",4,System.currentTimeMillis()));
        memoryQueue.leftPush(new PeerNode("e","5",5,System.currentTimeMillis()));
        System.out.println(memoryQueue.size());
        System.out.println(memoryQueue.rightPop());
        System.out.println(memoryQueue.rightPop());
        System.out.println(memoryQueue.rightPop());
        System.out.println(memoryQueue.rightPop());
        System.out.println(memoryQueue.rightPop());
        System.out.println(memoryQueue.rightPop());
    }
}
