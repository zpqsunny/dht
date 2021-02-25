package me.zpq.peer.coder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import me.zpq.peer.message.ExtendedMessage;

import java.nio.ByteBuffer;

/**
 * @author zpq
 * @date 2021/2/22 8:33
 */
public class ExtMessageEncoder extends MessageToByteEncoder<ExtendedMessage> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, ExtendedMessage extendedMessage, ByteBuf byteBuf) throws Exception {

        int length = extendedMessage.getMessage().length;
        ByteBuffer byteBuffer = ByteBuffer.allocate(length + 6);
        byteBuffer.putInt(length + 2)
                .put((byte) extendedMessage.getBittorrentMessageId())
                .put((byte) extendedMessage.getExtendedMessageId())
                .put(extendedMessage.getMessage());
        byteBuf.writeBytes(byteBuffer.array());
    }

}
