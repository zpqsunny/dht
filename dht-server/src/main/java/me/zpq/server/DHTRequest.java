package me.zpq.server;

import be.adaxisoft.bencode.BEncodedValue;
import lombok.Builder;
import lombok.Data;

import java.net.InetSocketAddress;

/**
 * @author zpq
 * @date 2021/2/16
 */
@Data
@Builder
public class DHTRequest {

    private BEncodedValue data;

    private InetSocketAddress sender;
}
