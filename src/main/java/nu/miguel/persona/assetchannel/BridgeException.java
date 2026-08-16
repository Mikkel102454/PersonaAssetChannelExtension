package nu.miguel.persona.assetchannel;

public final class BridgeException extends RuntimeException {
    public BridgeException(String message) { super(message); }
    public BridgeException(String message, Throwable cause) { super(message, cause); }
}
