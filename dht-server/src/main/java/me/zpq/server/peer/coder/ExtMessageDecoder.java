package me.zpq.server.peer.coder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;
import me.zpq.server.peer.message.ExtendedMessage;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * @author zpq
 * @date 2021/2/22 8:33
 */
@Slf4j
public class ExtMessageDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {

        int bufLen = byteBuf.readableBytes();
        // 解决粘包问题（不够一个包头的长度）
        // 4字节是报文中使用了一个int表示了报文长度
        if (bufLen < 4) {
            return;
        }
        // 标记一下当前的readIndex的位置
        byteBuf.markReaderIndex();
        int length = byteBuf.readInt();

        // 读到的消息体长度如果小于我们传送过来的消息长度，则resetReaderIndex。重置读索引,继续接收
        if (byteBuf.readableBytes() < length) {
            // 配合markReaderIndex使用的。把readIndex重置到mark的地方
            byteBuf.resetReaderIndex();
            return;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(length);
        byteBuf.readBytes(byteBuffer);
        byteBuffer.flip();
        byte bittorrentMessageId = byteBuffer.get();
        byte extendedMessageId = -1;
        /*
         * http://www.bittorrent.org/beps/bep_0003.html
         * 0 - choke
         * 1 - unchoke
         * 2 - interested
         * 3 - not interested
         * 4 - have
         * 5 - bitfield
         * 6 - request
         * 7 - piece
         * 8 - cancel
         * 'choke', 'unchoke', 'interested', and 'not interested' have no payload.
         */
        byte[] message = null;
        if (Arrays.binarySearch(new int[]{0, 1, 2, 3}, bittorrentMessageId) < 0) {

            extendedMessageId = byteBuffer.get();
            message = new byte[length - 2];
            byteBuffer.get(message);
        }
        ExtendedMessage extendedMessage = ExtendedMessage.builder()
                .bittorrentMessageId(bittorrentMessageId)
                .extendedMessageId(extendedMessageId)
                .message(message)
                .build();
        list.add(extendedMessage);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        log.error("ExtMessageDecoder exceptionCaught {} {}", cause.getClass(), cause.getMessage());
        ctx.close();
    }

}
