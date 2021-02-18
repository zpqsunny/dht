package me.zpq.server;

import be.adaxisoft.bencode.BEncodedValue;
import lombok.Builder;
import lombok.Data;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * @author zpq
 * @date 2021/2/16
 */
@Data
@Builder
public class DHTResponse {

    private Map<String, BEncodedValue> data;

    private InetSocketAddress sender;
}
