package com.sequenceiq.cloudbreak.service.stack.connector

class VolumeUtils private constructor() {

    init {
        throw IllegalStateException()
    }

    companion object {

        val VOLUME_PREFIX = "/hadoopfs/fs"
        private val LOG_VOLUME_INDEX = 1

        fun buildVolumePathString(volumeCount: Int, directory: String): String {
            val localDirs = StringBuilder("")
            for (i in 1..volumeCount) {
                localDirs.append(getVolumeDir(i, directory))
                if (i != volumeCount) {
                    localDirs.append(",")
                }
            }
            return localDirs.toString()
        }

        fun getLogVolume(directory: String): String {
            return getVolumeDir(LOG_VOLUME_INDEX, directory)
        }

        private fun getVolumeDir(volumeIndex: Int, directory: String): String {
            return VOLUME_PREFIX + volumeIndex + "/" + directory
        }
    }
}
