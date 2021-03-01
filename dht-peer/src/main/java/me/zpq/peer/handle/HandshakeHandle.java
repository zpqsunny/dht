package me.zpq.peer.handle;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import me.zpq.peer.MetadataConstant;
import me.zpq.peer.coder.ExtMessageDecoder;
import me.zpq.peer.coder.ExtMessageEncoder;
import me.zpq.peer.message.ExtendedMessage;
import me.zpq.peer.message.HandshakeMessage;
import org.apache.commons.codec.binary.Hex;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zpq
 * @date 2021/2/22 8:44
 */
@Slf4j
public class HandshakeHandle extends SimpleChannelInboundHandler<HandshakeMessage> {

    public byte[] hash;

    public HandshakeHandle(byte[] hash) {
        this.hash = hash;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HandshakeMessage msg) throws Exception {

        if (!msg.getProtocol().equals(MetadataConstant.PROTOCOL)) {

            log.error("protocol != BitTorrent, protocol: {}", msg.getProtocol());
            ctx.close();
            return;
        }
        if (this.hash.length != msg.getInfoHash().length) {

            log.error("info hash length is diff");
            ctx.close();
            return;
        }
        for (int i = 0; i < 20; i++) {

            if (this.hash[i] != msg.getInfoHash()[i]) {

                log.error("info hash byte is diff");
                ctx.close();
                return;
            }
        }
        log.info("validatorHandshake success");
        ctx.pipeline()
                .addLast("extHandshakeDecoder", new ExtMessageDecoder())
                .addLast("extHandshakeEncoder", new ExtMessageEncoder())
                .addLast("extHandshakeHandle", new ExtHandshakeHandle(this.hash))
        ;
        Map<String, BEncodedValue> m = new HashMap<>(16);
        Map<String, BEncodedValue> utMetadata = new HashMap<>(16);
        utMetadata.put(MetadataConstant.UT_METADATA, new BEncodedValue(1));
        m.put(MetadataConstant.M, new BEncodedValue(utMetadata));
        ExtendedMessage extendedMessage = ExtendedMessage.builder()
                .bittorrentMessageId(20)
                .extendedMessageId(0)
                .message(BEncoder.encode(m).array())
                .build();
        log.info("try to extHandShake");
        ctx.writeAndFlush(extendedMessage);
        ctx.pipeline().remove("handshakeDecoder");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        byte[] extension = new byte[]{0, 0, 0, 0, 0, 16, 0, 0};
        HandshakeMessage handshakeMessage = HandshakeMessage.builder()
                .protocol(MetadataConstant.PROTOCOL)
                .extension(extension)
                .infoHash(this.hash)
                .peerId(MetadataConstant.PEER_ID)
                .build();
        ctx.writeAndFlush(handshakeMessage);
        log.info("try to handshake");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        log.error("HandshakeHandle exceptionCaught {} {}", cause.getClass(), cause.getMessage());
        ctx.close();
    }
}
