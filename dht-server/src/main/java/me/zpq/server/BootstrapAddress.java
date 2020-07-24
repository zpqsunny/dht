package me.zpq.server;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author zpq
 * @date 2019-08-27
 */
@Data
@AllArgsConstructor
public class BootstrapAddress {

    private String host;

    private Integer port;

}
