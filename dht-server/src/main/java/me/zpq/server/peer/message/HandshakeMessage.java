package me.zpq.server.peer.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zpq
 * @date 2021/2/22 8:35
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HandshakeMessage {

    private String protocol;

    private byte[] extension;

    private byte[] infoHash;

    private String peerId;
}
