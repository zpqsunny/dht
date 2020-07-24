package me.zpq.peer.test;

import me.zpq.peer.Peer;
import org.junit.Test;

import java.io.IOException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;

/**
 * @author zpq
 * @date 2020/7/24
 */
public class PeerTest {

//    @Test
    public void test() throws IOException {

        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(2, 5,
                0L, TimeUnit.MINUTES, new LinkedBlockingQueue<>(), threadFactory);

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(threadFactory);

        scheduledExecutorService.scheduleAtFixedRate(new Peer(null, threadPoolExecutor), 1L,1L, TimeUnit.SECONDS);

        int read = System.in.read();
    }

//    @Test
    public void test2() throws IOException {

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                Random random = new Random();

                int i = random.nextInt();

                System.out.println("do " + i);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("finish " + i);

            }
        },1000L,1000L);

        int read = System.in.read();
    }
}
