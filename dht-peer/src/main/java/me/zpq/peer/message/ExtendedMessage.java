package me.zpq.peer.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zpq
 * @date 2021/2/22 8:53
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExtendedMessage {

    /**
     * bittorrent message ID, = 20
     */
    private int bittorrentMessageId;

    /**
     * extended message ID. 0 = handshake, >0 = extended message as specified by the handshake.
     */
    private int extendedMessageId;

    private byte[] message;
}
