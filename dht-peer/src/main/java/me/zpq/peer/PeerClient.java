package me.zpq.peer;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;
import lombok.extern.slf4j.Slf4j;
import me.zpq.dht.common.Utils;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static me.zpq.peer.MetadataConstant.*;

@Slf4j
public class PeerClient {

    private static final int CONNECT_TIMEOUT = 10 * 1000;

    private static final int READ_TIMEOUT = 60 * 1000;

    private static final int BLOCK_SIZE = 16384;

    private final String host;

    private final int port;

    private final byte[] infoHash;

    public PeerClient(String host, int port, byte[] infoHash) {
        this.host = host;
        this.port = port;
        this.infoHash = infoHash;
    }

    public byte[] run() {

        try (Socket socket = new Socket()) {
            socket.setSoTimeout(READ_TIMEOUT);
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            log.info("connect server host: {} port: {} hash: {}", host, port, Utils.bytesToHex(this.infoHash));
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT);
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();
            log.info("try to handshake");
            this.handshake(outputStream);
            if (!this.validatorHandshake(inputStream)) {

                return null;
            }
            log.info("try to extHandShake");
            this.extHandShake(outputStream);
            BEncodedValue bEncodedValue = this.validatorExtHandShake(inputStream);
            if (bEncodedValue == null) {

                return null;
            }
            int utMetadata = bEncodedValue.getMap().get(M).getMap().get(UT_METADATA).getInt();
            int metaDataSize = bEncodedValue.getMap().get(METADATA_SIZE).getInt();
            int block = metaDataSize % BLOCK_SIZE > 0 ? metaDataSize / BLOCK_SIZE + 1 : metaDataSize / BLOCK_SIZE;
            log.info("metaDataSize: {} block: {}", metaDataSize, block);
            ByteBuffer metaInfo = ByteBuffer.allocate(metaDataSize);
            for (int i = 0; i < block; i++) {

                this.metadataRequest(outputStream, utMetadata, i);
                log.info("request block index: {} ok", i);
                Map<String, BEncodedValue> m = new HashMap<>(16);
                m.put(MSG_TYPE, new BEncodedValue(1));
                m.put(PIECE, new BEncodedValue(i));
                m.put(TOTAL_SIZE, new BEncodedValue(metaDataSize));
                byte[] data = BEncoder.encode(m).array();
                byte[] length = this.resolveLengthMessage(inputStream, 4);
                byte[] response = this.resolveLengthMessage(inputStream, byte2int(length));
                if (response.length <= (data.length + 2)) {

                    log.info("resolve block index: {} fail", i);
                    return null;
                }
                metaInfo.put(Arrays.copyOfRange(response, data.length + 2, response.length));
                log.info("resolve block index: {} ok", i);
            }
            log.info("validator sha1");
            byte[] info = metaInfo.array();
            byte[] sha1 = DigestUtils.sha1(info);
            if (sha1.length != infoHash.length) {

                log.error("length fail");
                return null;
            }
            for (int i = 0; i < infoHash.length; i++) {

                if (infoHash[i] != sha1[i]) {

                    log.error("info hash not eq");
                    return null;
                }
            }
            log.info("success");
            return info;
        } catch (Exception e) {

            log.error("{} : {}", e.getClass().getName(), e.getMessage());
            return null;
        }

    }

    private void handshake(OutputStream outputStream) throws IOException {

        byte[] extension = new byte[]{0, 0, 0, 0, 0, 16, 0, 0};
        ByteBuffer handshake = ByteBuffer.allocate(68);
        handshake.put((byte) PROTOCOL.length())
                .put(PROTOCOL.getBytes())
                .put(extension)
                .put(infoHash)
                .put(PEER_ID.getBytes());
        outputStream.write(handshake.array());
        outputStream.flush();
    }

    private boolean validatorHandshake(InputStream inputStream) throws IOException {

        byte[] bitTorrent = this.resolveMessage(inputStream);
        if (!PROTOCOL.equals(new String(bitTorrent))) {

            log.error("protocol != BitTorrent, protocol: {}", new String(bitTorrent));
            return false;
        }
        byte[] last = this.resolveLengthMessage(inputStream, 48);
        log.warn("{} {} {} {} {} {} {} {}", last[0], last[1], last[2], last[3], last[4], last[5], last[6], last[7]);
        byte[] infoHash = Arrays.copyOfRange(last, 8, 28);
        if (infoHash.length != this.infoHash.length) {

            log.error("info hash length is diff");
            return false;
        }
        for (int i = 0; i < 20; i++) {

            if (infoHash[i] != this.infoHash[i]) {

                log.error("info hash byte is diff");
                return false;
            }
        }
        return true;
    }

    private void extHandShake(OutputStream outputStream) throws IOException {

        Map<String, BEncodedValue> m = new HashMap<>(16);
        Map<String, BEncodedValue> utMetadata = new HashMap<>(16);
        utMetadata.put(UT_METADATA, new BEncodedValue(1));
        m.put(M, new BEncodedValue(utMetadata));
        outputStream.write(this.packMessage(20, 0, BEncoder.encode(m).array()));
        outputStream.flush();
    }

    private BEncodedValue validatorExtHandShake(InputStream inputStream) throws IOException {

        byte[] prefix = this.resolveLengthMessage(inputStream, 4);
        int length = byte2int(prefix);
        byte[] data = this.resolveLengthMessage(inputStream, length);
        int messageId = data[0];
        int messageType = data[1];
        if (messageId != 20) {

            log.error("want to get messageId 20 but messageId: {}", messageId);
            return null;
        }
        if (messageType != 0) {

            log.error("want to get messageType 0 but messageType: {}", messageType);
            return null;
        }
        byte[] bDecode = Arrays.copyOfRange(data, 2, length);
        BEncodedValue decode = BDecoder.decode(new ByteArrayInputStream(bDecode));
        if (decode.getMap().get(METADATA_SIZE) == null) {

            log.error("metadata_size == null");
            return null;
        }
        if (decode.getMap().get(METADATA_SIZE).getInt() <= 0) {

            log.error("metadata_size <= 0");
            return null;
        }
        if (decode.getMap().get(M) == null) {

            log.error("m == null");
            return null;
        }
        if (decode.getMap().get(M).getMap().get(UT_METADATA) == null) {

            log.error("m.ut_metadata == null");
            return null;
        }
        while (inputStream.available() > 0) {

            int read = inputStream.read();
        }
        return decode;

    }

    private void metadataRequest(OutputStream outputStream, int utMetadata, int piece) throws IOException {

        Map<String, BEncodedValue> d = new HashMap<>(6);
        d.put(MSG_TYPE, new BEncodedValue(0));
        d.put(PIECE, new BEncodedValue(piece));
        outputStream.write(this.packMessage(20, utMetadata, BEncoder.encode(d).array()));
        outputStream.flush();
    }

    private int byte2int(byte[] bytes) {

        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (3 - i) * 8;
            value += (bytes[i] & 0xFF) << shift;
        }
        return value;
    }

    private byte[] resolveMessage(InputStream inputStream) throws IOException {

        int length = inputStream.read();
        if (length <= 0) {

            throw new IOException("end of the stream is reached");
        }
        return this.resolveLengthMessage(inputStream, length);
    }

    private byte[] resolveLengthMessage(InputStream inputStream, int length) throws IOException {

        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {

            int r = inputStream.read();
            if (r == -1) {

                throw new IOException("end of the stream is reached");
            }
            result[i] = (byte) r;
        }
        return result;
    }

    private byte[] packMessage(int messageId, int messageType, byte[] data) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(data.length + 6);
        byteBuffer.putInt(data.length + 2)
                .put((byte) (messageId))
                .put((byte) (messageType))
                .put(data);
        return byteBuffer.array();
    }

}
