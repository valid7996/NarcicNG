package libv2ray;

public interface ProcessFinder {
    long findProcessByConnection(String network, String srcIP, long srcPort, String destIP, long destPort);
}
