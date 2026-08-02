package libv2ray;

public class Libv2ray {
    public static void initCoreEnv(String assetPath, String deviceId) {
    }

    public static void reconcileBrowserDialer(String dialerAddr) {
    }

    public static String checkVersionX() {
        return "v5.14.1";
    }

    public static long measureOutboundDelay(String config, String testUrl) {
        return 120L;
    }

    public static CoreController newCoreController(CoreCallbackHandler handler) {
        return new CoreController();
    }

    public static String fetchQuicCertSha256(String jsonRequest) {
        return "";
    }

    public static String fetchTlsCertSha256(String jsonRequest) {
        return "";
    }
}
