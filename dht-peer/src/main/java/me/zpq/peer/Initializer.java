package me.zpq.peer;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import me.zpq.peer.coder.HandshakeDecoder;
import me.zpq.peer.coder.HandshakeEncoder;
import me.zpq.peer.handle.HandshakeHandle;

import java.util.concurrent.TimeUnit;

/**
 * @author zpq
 * @date 2021/2/23 13:58
 */
public class Initializer extends ChannelInitializer<SocketChannel> {

    private byte[] hash;

    public Initializer(byte[] hash) {
        this.hash = hash;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {

        ch.pipeline()
                .addLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                .addLast("handshakeEncoder", new HandshakeEncoder())
                .addLast("handshakeDecoder", new HandshakeDecoder())
                .addLast("handshakeHandle", new HandshakeHandle(hash))
        ;
    }
}
