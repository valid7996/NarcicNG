package libv2ray;

public interface CoreCallbackHandler {
    long startup();
    long shutdown();
    long onEmitStatus(long l, String s);
}
