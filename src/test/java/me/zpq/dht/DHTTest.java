package me.zpq.dht;

import org.junit.Test;

import java.io.IOException;

public class DHTTest {

    @Test
    public void test() throws IOException {

        ClientHandler clientHandler = new ClientHandler("12345678901234567890",Helper.hexToByte("4453db065d7d35d1a11a8665ca3d74fadf95e083"));
        clientHandler.request();
//        System.out.println(new String(new byte[]{0, 0, 0, 124}));
    }
}
