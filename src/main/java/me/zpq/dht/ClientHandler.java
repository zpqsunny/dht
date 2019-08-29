package me.zpq.dht;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    private ByteBuf buf;

    private static final Logger LOGGER = LoggerFactory.getLogger(MetaInfo.class);

    private String peerId;

    private byte[] infoHash;

    public ClientHandler(String peerId, byte[] infoHash) {
        this.peerId = peerId;
        this.infoHash = infoHash;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        buf = ctx.alloc().buffer(4);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        buf.release();
        buf = null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf m = (ByteBuf) msg;
        buf.writeBytes(m);
        m.release();
        if (buf.readableBytes() >= 4) {

        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String protocol = "BitTorrent protocol";
        byte[] extension = new byte[]{0, 0, 0, 0, 0, 16, 0, 0};
        ByteBuffer byteBuffer = ByteBuffer.allocate(68);
        byteBuffer.put((byte) protocol.length())
                .put(protocol.getBytes())
                .put(extension)
                .put(infoHash)
                .put(peerId.getBytes());
        ctx.writeAndFlush(Unpooled.copiedBuffer(byteBuffer.array()));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

        ByteBuf buffer = ctx.alloc().buffer();
        System.out.println(buffer.toString(CharsetUtil.UTF_8) + "channelReadComplete");
//        super.channelReadComplete(ctx);
    }
}
