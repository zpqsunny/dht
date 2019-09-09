package me.zpq.dht;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;
import be.adaxisoft.bencode.BEncoder;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);

    private static final String PROTOCOL = "BitTorrent protocol";

    private String host;

    private int port;

    private String peerId;

    private byte[] infoHash;

    public ClientHandler(String host, int port, String peerId, byte[] infoHash) {
        this.host = host;
        this.port = port;
        this.peerId = peerId;
        this.infoHash = infoHash;
    }

    public void request() throws IOException {

        Socket socket = new Socket();
        LOGGER.info("start connect server host: %s port: %s", host, port);
        socket.connect(new InetSocketAddress(host, port), 60000);
        LOGGER.info("connect success");
        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();
        LOGGER.info("try to handshake");
        this.handshake(outputStream);
        if (!this.validatorHandshake(inputStream)) {

            LOGGER.error("protocol != BitTorrent");
            return;
        }
        LOGGER.info("handshake success");
        LOGGER.info("try to extHandShake");
        this.extHandShake(outputStream);
        BEncodedValue bEncodedValue = this.validatorExtHandShake(inputStream);

        if (bEncodedValue == null) {

            LOGGER.error("validatorExtHandShake false");
            return;
        }
        LOGGER.info("extHandShake success");
        int utMetadata = bEncodedValue.getMap().get("m").getMap().get("ut_metadata").getInt();
        int metaDataSize = bEncodedValue.getMap().get("metadata_size").getInt();
        // metaDataSize / 16384
        int block = metaDataSize % 16384 > 0 ? metaDataSize / 16384 + 1 : metaDataSize / 16384;
        LOGGER.info("metaDataSize: %s block: %s", metaDataSize, block);
        LOGGER.info("start request block");
        for (int i = 0; i < block; i++) {

            this.metadataRequest(outputStream, utMetadata, i);
            LOGGER.info("request block index: %s ok", i);

        }
        LOGGER.info("request block finish");
        ByteBuffer metaInfo = ByteBuffer.allocate(metaDataSize);
        LOGGER.info("start resolve block");
        for (int i = 0; i < block; i++) {

            Map<String, BEncodedValue> m = new HashMap<>(6);
            m.put("msg_type", new BEncodedValue(1));
            m.put("piece", new BEncodedValue(i));
            m.put("total_size", new BEncodedValue(metaDataSize));
            byte[] response = BEncoder.encode(m).array();
            byte[] length = this.resolveLengthMessage(inputStream, 4);
            byte[] result = this.resolveLengthMessage(inputStream, byte2int(length));
            metaInfo.put(Arrays.copyOfRange(result, response.length + 2, result.length));
            LOGGER.info("resolve block index: %s ok", i);
        }
        LOGGER.info("resolve block all finish");
        LOGGER.info("validator sha1");
        byte[] info = metaInfo.array();
        byte[] sha1 = DigestUtils.sha1(info);
        if (sha1.length != infoHash.length) {

            LOGGER.error("length fail");
            return;
        }
        for (int i = 0; i < infoHash.length; i++) {

            if (infoHash[i] != sha1[i]) {

                LOGGER.error("info hash not eq");
                return;
            }
        }
        LOGGER.info("success");
        socket.close();
        MongoMetaInfoImpl mongoMetaInfo = new MongoMetaInfoImpl("");
        mongoMetaInfo.todoSomething(this.infoHash, info);

    }

    private void handshake(OutputStream outputStream) throws IOException {

        byte[] extension = new byte[]{0, 0, 0, 0, 0, 16, 0, 0};
        ByteBuffer handshake = ByteBuffer.allocate(68);
        handshake.put((byte) PROTOCOL.length())
                .put(PROTOCOL.getBytes())
                .put(extension)
                .put(infoHash)
                .put(peerId.getBytes());
        outputStream.write(handshake.array());
        outputStream.flush();
    }

    private boolean validatorHandshake(InputStream inputStream) throws IOException {

        byte[] bitTorrent = this.resolveMessage(inputStream);
        if (!PROTOCOL.equals(new String(bitTorrent))) {

            return false;
        }
        byte[] last = this.resolveLengthMessage(inputStream, 48);
        byte[] infoHash = Arrays.copyOfRange(last, 8, 28);
        if (infoHash.length != this.infoHash.length) {

            return false;
        }
        for (int i = 0; i < 20; i++) {

            if (infoHash[i] != this.infoHash[i]) {

                return false;
            }
        }
        return true;
    }

    private void extHandShake(OutputStream outputStream) throws IOException {

        Map<String, BEncodedValue> m = new HashMap<>(6);
        Map<String, BEncodedValue> utMetadata = new HashMap<>(6);
        utMetadata.put("ut_metadata", new BEncodedValue(1));
        m.put("m", new BEncodedValue(utMetadata));
        outputStream.write(this.packMessage(20, 0, BEncoder.encode(m).array()));
        outputStream.flush();
    }

    private BEncodedValue validatorExtHandShake(InputStream inputStream) throws IOException {

        byte[] prefix = this.resolveLengthMessage(inputStream, 4);
        int length = byte2int(prefix);
        byte[] data = this.resolveLengthMessage(inputStream, length);
        int messageId = (int) data[0];
        int messageType = (int) data[1];
        if (messageId != 20) {

            System.out.println("messageId fail");
            return null;
        }
        if (messageType != 0) {

            System.out.println("messageType fail");
            return null;
        }
        byte[] bDecode = Arrays.copyOfRange(data, 2, length);
        BEncodedValue decode = BDecoder.decode(new ByteArrayInputStream(bDecode));
        if (decode.getMap().get("metadata_size") == null) {

            System.out.println("metadata_size == null");
            return null;
        }
        if (decode.getMap().get("m") == null) {

            System.out.println("m == null");
            return null;
        }
        if (decode.getMap().get("m").getMap().get("ut_metadata") == null) {

            System.out.println("m.ut_metadata == null");
            return null;
        }
        while (inputStream.available() > 0) {

            int read = inputStream.read();
        }
        return decode;

    }

    private void metadataRequest(OutputStream outputStream, int utMetadata, int piece) throws IOException {

        Map<String, BEncodedValue> d = new HashMap<>(6);
        d.put("msg_type", new BEncodedValue(0));
        d.put("piece", new BEncodedValue(piece));
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

        int length;
        while (true) {

            int read = inputStream.read();
            if (read != -1) {

                length = read;
                break;
            }
        }

        return this.resolveLengthMessage(inputStream, length);
    }

    private byte[] resolveLengthMessage(InputStream inputStream, int length) throws IOException {

        byte[] result = new byte[length];
        int index = 0;
        while (index < result.length) {

            int r = inputStream.read();
            if (r != -1) {

                result[index] = (byte) r;
                index++;
            }
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
