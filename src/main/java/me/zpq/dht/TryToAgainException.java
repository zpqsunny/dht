package me.zpq.dht;

/**
 * @author zpq
 * @date 2019-09-09
 */
public class TryToAgainException extends Exception {

    public TryToAgainException() {
        super();
    }

    public TryToAgainException(String message) {
        super(message);
    }
}
