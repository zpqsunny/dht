package me.zpq.route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zpq
 * @date 2019-08-26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NodeTable {

    private String id;

    private String ip;

    private int port;

    private long lastChanged;

}
