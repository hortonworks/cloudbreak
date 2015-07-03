package com.sequenceiq.cloudbreak.service.stack.connector;

public final class VolumeUtils {

    private static final String VOLUME_PREFIX = "/hadoopfs/fs";
    private static final int FIRST_VOLUME_INDEX = 1;

    private VolumeUtils() {
        throw new IllegalStateException();
    }

    public static String buildVolumePathString(int volumeCount, String directory) {
        StringBuilder localDirs = new StringBuilder("");
        for (int i = 1; i <= volumeCount; i++) {
            localDirs.append(getVolumeDir(i, directory));
            if (i != volumeCount) {
                localDirs.append(",");
            }
        }
        return localDirs.toString();
    }

    public static String getFirstVolume(String directory) {
        return getVolumeDir(FIRST_VOLUME_INDEX, directory);
    }

    private static String getVolumeDir(int volumeIndex, String directory) {
        return VOLUME_PREFIX + volumeIndex + "/" + directory;
    }
}
