package me.zpq.route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author zpq
 * @date 2019-08-26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NodeTable implements Serializable {

    private String id;

    private String ip;

    private int port;

    private long lastChanged;

}
