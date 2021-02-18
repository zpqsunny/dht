package me.zpq.server;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * @author zpq
 * @date 2021/2/13
 */
@Slf4j
public class DHTRequestDecoder extends MessageToMessageDecoder<DatagramPacket> {

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket datagramPacket, List<Object> out) throws Exception {

        ByteBuf content = datagramPacket.content();
        byte[] req = new byte[content.readableBytes()];
        content.readBytes(req);

        BEncodedValue data = BDecoder.decode(new ByteArrayInputStream(req));
        DHTRequest dhtRequest = DHTRequest.builder()
                .data(data)
                .sender(datagramPacket.sender())
                .build();
        out.add(dhtRequest);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        log.error("BDecoder fail ...");
    }
}
