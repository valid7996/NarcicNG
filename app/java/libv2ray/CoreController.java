package libv2ray;

public class CoreController {
    public boolean isRunning = false;

    public void registerProcessFinder(ProcessFinder pf) {
    }

    public void startLoop(String config, int tunFd) {
        this.isRunning = true;
    }

    public void stopLoop() {
        this.isRunning = false;
    }

    public String queryAllOutboundTrafficStats() {
        return "";
    }

    public long measureDelay(String testUrl) {
        return 120L;
    }
}
