package me.zpq.peer.coder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import me.zpq.peer.message.HandshakeMessage;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * @author zpq
 * @date 2021/2/22 8:33
 */
@Slf4j
public class HandshakeDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {

        if (byteBuf.readableBytes() >= 68) {

            ByteBuffer byteBuffer = ByteBuffer.allocate(68);
            byteBuf.readBytes(byteBuffer);
            byteBuffer.flip();
            byte protocolLength = byteBuffer.get();
            byte[] protocol = new byte[19];
            byteBuffer.get(protocol);
            byte[] extension = new byte[8];
            byteBuffer.get(extension);
            byte[] infoHash = new byte[20];
            byteBuffer.get(infoHash);
            byte[] peerId = new byte[20];
            byteBuffer.get(peerId);

            HandshakeMessage handshakeMessage = HandshakeMessage.builder()
                    .protocol(new String(protocol))
                    .extension(extension)
                    .infoHash(infoHash)
                    .peerId(new String(peerId))
                    .build();
            list.add(handshakeMessage);
        }
    }
}
