package me.zpq.peer.handle;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import me.zpq.peer.message.ExtendedMessage;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static me.zpq.peer.MetadataConstant.*;

/**
 * @author zpq
 * @date 2021/2/22 8:44
 */
@Slf4j
public class ExtHandshakeHandle extends SimpleChannelInboundHandler<ExtendedMessage> {

    public byte[] hash;

    public int metaDataSize;

    public int utMetadata;

    public int block;

    public ExtHandshakeHandle(byte[] hash) {
        this.hash = hash;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ExtendedMessage msg) throws Exception {

        log.info("getMessageType {} extendedMessageId {}", msg.getBittorrentMessageId(), msg.getExtendedMessageId());
        if (msg.getBittorrentMessageId() == 20 && msg.getExtendedMessageId() == 0) {

            // ext handshake
            BEncodedValue decode = BDecoder.decode(new ByteArrayInputStream(msg.getMessage()));

            if (decode.getMap().get(METADATA_SIZE) == null) {

                log.error("metadata_size == null");
                ctx.close();
                return;
            }
            if (decode.getMap().get(METADATA_SIZE).getInt() <= 0) {

                log.error("metadata_size <= 0");
                ctx.close();
                return;
            }
            this.metaDataSize = decode.getMap().get(METADATA_SIZE).getInt();
            if (decode.getMap().get(M) == null) {

                log.error("m == null");
                ctx.close();
                return;
            }
            if (decode.getMap().get(M).getMap().get(UT_METADATA) == null) {

                log.error("m.ut_metadata == null");
                ctx.close();
                return;
            }
            this.utMetadata = decode.getMap().get(M).getMap().get(UT_METADATA).getInt();
            this.block = this.metaDataSize % 16384 > 0 ? this.metaDataSize / 16384 + 1 : this.metaDataSize / 16384;
            log.info("metaDataSize: {} block: {} utMetadata: {}", this.metaDataSize, this.block, this.utMetadata);
            for (int i = 0; i < this.block; i++) {

                Map<String, BEncodedValue> bEncode = new HashMap<>(6);
                bEncode.put(MSG_TYPE, new BEncodedValue(0));
                bEncode.put(PIECE, new BEncodedValue(i));
                ExtendedMessage extendedMessage = ExtendedMessage.builder()
                        .bittorrentMessageId(20)
                        .extendedMessageId(utMetadata)
                        .message(BEncoder.encode(bEncode).array())
                        .build();
                ctx.write(extendedMessage);
                log.info("request block index: {} ok", i);
            }
            ctx.flush();
            ctx.pipeline()
                    .addLast("metadataHandle", new MetadataHandle(this.metaDataSize, this.utMetadata, this.block, this.hash))
            ;
            ctx.pipeline().remove("extHandshakeHandle");
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        log.error("ExtHandshakeHandle exceptionCaught", cause);
        ctx.close();
    }
}
