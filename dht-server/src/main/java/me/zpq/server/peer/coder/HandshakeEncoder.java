package me.zpq.server.peer.coder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import me.zpq.server.peer.message.HandshakeMessage;

import java.nio.ByteBuffer;

/**
 * @author zpq
 * @date 2021/2/22 8:33
 */
public class HandshakeEncoder extends MessageToByteEncoder<HandshakeMessage> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, HandshakeMessage handshakeMessage, ByteBuf byteBuf) throws Exception {

        ByteBuffer byteBuffer = ByteBuffer.allocate(68);
        byteBuffer.put((byte) handshakeMessage.getProtocol().length())
                .put(handshakeMessage.getProtocol().getBytes())
                .put(handshakeMessage.getExtension())
                .put(handshakeMessage.getInfoHash())
                .put(handshakeMessage.getPeerId().getBytes());
        byteBuf.writeBytes(byteBuffer.array());
    }
}
