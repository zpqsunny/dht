package me.zpq.server.peer;

/**
 * @author zpq
 * @date 2024/11/29
 */
public interface BaseMetaInfo {

    boolean checkContinue(byte[] hash);

    void run(byte[] info);

}
