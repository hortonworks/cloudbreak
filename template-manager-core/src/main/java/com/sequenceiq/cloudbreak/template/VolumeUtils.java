package com.sequenceiq.cloudbreak.template;

public final class VolumeUtils {

    public static final String VOLUME_PREFIX = "/hadoopfs/fs";

    public static final String ROOT_VOLUME_PREFIX = "/hadoopfs/root";

    private static final int FIRST_VOLUME = 1;

    private VolumeUtils() {
        throw new IllegalStateException();
    }

    /**
     * This method construct a directory string delimited by comma using "/hadoopfs/fs" as base.
     *
     * @param volumeCount number of volumes attached
     * @param directory   directory to use as postfix
     * @return returns the full path of the directories delimited by comma. In case of 0 volumes it will return an empty string.
     */
    public static String buildVolumePathString(int volumeCount, String directory) {
        return buildVolumePathString(volumeCount, directory, VOLUME_PREFIX);
    }

    /**
     * This method handles when there are no volumes attached. In case of 0 volumes the base directory will be
     * "/hadoopfs/root1". In case of 1 or multiple disks it will construct a string like "/hadoopfs/fs1/directory,/hadoopfs/fs2/directory".
     * The primary use case of this method is Cloudera Manager (CM) based clusters, because CM can determine incorrect folders to use when
     * there are no attached volumes.
     *
     * @param volumeCount number of volumes attached (can be 0)
     * @param directory   directory to use as postfix
     * @return returns the full path of the directories delimited by comma. In case of 0 volumes it returns "/hadoopfs/root1/directory" where
     * directory is based on the input parameter
     */
    public static String buildVolumePathStringZeroVolumeHandled(int volumeCount, String directory) {
        if (volumeCount == 0) {
            return buildVolumePathString(FIRST_VOLUME, directory, ROOT_VOLUME_PREFIX);
        }
        return buildVolumePathString(volumeCount, directory, VOLUME_PREFIX);
    }

    public static String getLogVolume(String directory) {
        return getVolumeDir(FIRST_VOLUME, VOLUME_PREFIX, directory);
    }

    private static String buildVolumePathString(int volumeCount, String directory, String dirPrefix) {
        StringBuilder localDirs = new StringBuilder();
        for (int i = 1; i <= volumeCount; i++) {
            localDirs.append(getVolumeDir(i, dirPrefix, directory));
            if (i != volumeCount) {
                localDirs.append(',');
            }
        }
        return localDirs.toString();
    }

    private static String getVolumeDir(int volumeIndex, String dirPrefix, String directory) {
        return dirPrefix + volumeIndex + '/' + directory;
    }
}
