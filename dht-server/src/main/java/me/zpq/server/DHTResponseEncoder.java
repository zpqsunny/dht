package me.zpq.server;

import be.adaxisoft.bencode.BEncoder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * @author zpq
 * @date 2021/2/18 14:57
 */
public class DHTResponseEncoder extends MessageToMessageEncoder<DHTResponse> {

    @Override
    protected void encode(ChannelHandlerContext ctx, DHTResponse msg, List<Object> out) throws Exception {

        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        BEncoder.encode(msg.getData(), byteArray);

        out.add(new DatagramPacket(Unpooled.copiedBuffer(byteArray.toByteArray()), msg.getSender()));

    }
}
