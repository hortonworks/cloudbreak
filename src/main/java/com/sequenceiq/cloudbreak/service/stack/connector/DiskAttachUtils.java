package com.sequenceiq.cloudbreak.service.stack.connector;

public final class DiskAttachUtils {

    private DiskAttachUtils() {
        throw new IllegalStateException();
    }

    public static String buildDiskPathString(int volumeCount) {
        StringBuilder localDirs = new StringBuilder("");
        for (int i = 1; i <= volumeCount; i++) {
            localDirs.append("/mnt/fs").append(i);
            if (i != volumeCount) {
                localDirs.append(",");
            }
        }
        return localDirs.toString();
    }
}
