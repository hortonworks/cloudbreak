package com.sequenceiq.common.api.util.versionchecker;

public class ClientVersionUtil {

    private ClientVersionUtil() {
    }

    public static VersionCheckResult checkClientVersion(String apiVersion, String clientVersion) {
        boolean compatible = checkVersion(apiVersion, clientVersion);
        if (compatible) {
            return new VersionCheckResult(true);
        }
        return new VersionCheckResult(false, String.format("The versions are not compatible: [server: '%s', client: '%s']", apiVersion, clientVersion));
    }

    public static boolean checkVersion(String server, String client) {
        try {
            if (isSnapshot(client)) {
                return true;
            }
            String[] serverParts = removeStartingV(server).split("\\.");
            String[] clientParts = removeStartingV(client).split("\\.");
            if (serverParts[0].equals(clientParts[0]) && serverParts[1].equals(clientParts[1])) {
                return true;
            }
        } catch (ArrayIndexOutOfBoundsException | NullPointerException ignore) {
        }
        return false;
    }

    private static boolean isSnapshot(String client) {
        return client.startsWith("snapshot");
    }

    private static String removeStartingV(String s) {
        if (s.startsWith("v")) {
            return s.replaceFirst("v", "");
        }
        return s;
    }

}