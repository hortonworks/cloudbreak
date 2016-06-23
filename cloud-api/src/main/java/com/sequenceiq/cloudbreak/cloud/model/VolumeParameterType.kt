package com.sequenceiq.cloudbreak.cloud.model

enum class VolumeParameterType {
    MAGNETIC {
        override fun getVolumeParameterbyType(metaData: VmTypeMeta): VolumeParameterConfig {
            return metaData.magneticConfig
        }
    },
    SSD {
        override fun getVolumeParameterbyType(metaData: VmTypeMeta): VolumeParameterConfig {
            return metaData.ssdConfig
        }
    },
    EPHEMERAL {
        override fun getVolumeParameterbyType(metaData: VmTypeMeta): VolumeParameterConfig {
            return metaData.ephemeralConfig
        }
    },
    ST1 {
        override fun getVolumeParameterbyType(metaData: VmTypeMeta): VolumeParameterConfig {
            return metaData.st1Config
        }
    },
    AUTO_ATTACHED {
        override fun getVolumeParameterbyType(metaData: VmTypeMeta): VolumeParameterConfig {
            return metaData.autoAttachedConfig
        }
    };

    abstract fun getVolumeParameterbyType(metaData: VmTypeMeta): VolumeParameterConfig
}
