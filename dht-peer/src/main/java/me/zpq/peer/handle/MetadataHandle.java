package me.zpq.peer.handle;

import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import me.zpq.peer.JsonMetaInfo;
import me.zpq.peer.MetadataConstant;
import me.zpq.peer.message.ExtendedMessage;
import org.apache.commons.codec.digest.DigestUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zpq
 * @date 2021/2/22 9:28
 */
@Slf4j
public class MetadataHandle extends SimpleChannelInboundHandler<ExtendedMessage> {

    public int metaDataSize;

    public int utMetadata;

    public int block;

    public ByteBuffer metadata;

    public int i;

    public byte[] hash;

    public MetadataHandle(int metaDataSize, int utMetadata, int block, byte[] hash) {
        this.metaDataSize = metaDataSize;
        this.utMetadata = utMetadata;
        this.block = block;
        this.metadata = ByteBuffer.allocate(metaDataSize);
        this.i = 0;
        this.hash = hash;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ExtendedMessage msg) throws Exception {

        log.info("getMessageType {} extendedMessageId {}", msg.getBittorrentMessageId(), msg.getExtendedMessageId());
        if (msg.getBittorrentMessageId() == 20 && msg.getExtendedMessageId() == 0) {

            Map<String, BEncodedValue> m = new HashMap<>(16);
            m.put(MetadataConstant.MSG_TYPE, new BEncodedValue(1));
            m.put(MetadataConstant.PIECE, new BEncodedValue(i));
            m.put(MetadataConstant.TOTAL_SIZE, new BEncodedValue(metaDataSize));
            byte[] data = BEncoder.encode(m).array();
            int length = msg.getMessage().length;
            int metadataLength = Math.min(16384, metaDataSize - i * 16384);
            if ((data.length + metadataLength) != length) {

                log.error("resolve block index: {} fail", i);
                ctx.close();
                return;
            }
            byte[] metadata = Arrays.copyOfRange(msg.getMessage(), data.length, msg.getMessage().length);
            this.metadata.put(metadata);
            log.info("resolve block index: {} ok", i);
            i++;
        }

        if (this.metadata.position() == this.metadata.capacity()) {

            log.info("receive finish");
            byte[] sha1 = DigestUtils.sha1(this.metadata.array());
            if (sha1.length != this.hash.length) {

                log.error("length fail");
                return;
            }
            for (int i = 0; i < sha1.length; i++) {

                if (this.hash[i] != sha1[i]) {

                    log.error("info hash not eq");
                    return;
                }
            }
            log.info("success");
            log.info("{}", JsonMetaInfo.show(this.metadata.array()));
            ctx.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        log.error("MetadataHandle exceptionCaught", cause);
        ctx.close();
    }
}
