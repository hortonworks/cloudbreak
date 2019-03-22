package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.VmTypeMetaBuilder;

public enum VolumeParameterType {

    MAGNETIC() {
        @Override public VolumeParameterConfig getVolumeParameterByType(VmTypeMeta metaData) {
            return metaData.getMagneticConfig();
        }

        @Override
        public void buildForVmTypeMetaBuilder(VmTypeMetaBuilder builder, int maxDataDiskCount) {
            builder.withMagneticConfig(VolumeParameterType.volumeParameterConfig(this, maxDataDiskCount));
        }

        @Override
        public boolean in(VolumeParameterType... types) {
            return VolumeParameterType.in(this, types);
        }
    },

    SSD() {
        @Override public VolumeParameterConfig getVolumeParameterByType(VmTypeMeta metaData) {
            return metaData.getSsdConfig();
        }

        @Override
        public void buildForVmTypeMetaBuilder(VmTypeMetaBuilder builder, int maxDataDiskCount) {
            builder.withSsdConfig(VolumeParameterType.volumeParameterConfig(this, maxDataDiskCount));
        }

        @Override
        public boolean in(VolumeParameterType... types) {
            return VolumeParameterType.in(this, types);
        }
    },

    EPHEMERAL() {
        @Override public VolumeParameterConfig getVolumeParameterByType(VmTypeMeta metaData) {
            return metaData.getEphemeralConfig();
        }

        @Override
        public void buildForVmTypeMetaBuilder(VmTypeMetaBuilder builder, int maxDataDiskCount) {
            builder.withEphemeralConfig(VolumeParameterType.volumeParameterConfig(this, maxDataDiskCount));
        }

        @Override
        public boolean in(VolumeParameterType... types) {
            return VolumeParameterType.in(this, types);
        }
    },

    ST1() {
        @Override public VolumeParameterConfig getVolumeParameterByType(VmTypeMeta metaData) {
            return metaData.getSt1Config();
        }

        @Override
        public void buildForVmTypeMetaBuilder(VmTypeMetaBuilder builder, int maxDataDiskCount) {
            builder.withSt1Config(VolumeParameterType.volumeParameterConfig(this, maxDataDiskCount));
        }

        @Override
        public boolean in(VolumeParameterType... types) {
            return VolumeParameterType.in(this, types);
        }
    },

    AUTO_ATTACHED() {
        @Override public VolumeParameterConfig getVolumeParameterByType(VmTypeMeta metaData) {
            return metaData.getAutoAttachedConfig();
        }

        @Override
        public void buildForVmTypeMetaBuilder(VmTypeMetaBuilder builder, int maxDataDiskCount) {
            builder.withAutoAttachedConfig(VolumeParameterType.volumeParameterConfig(this, maxDataDiskCount));
        }

        @Override
        public boolean in(VolumeParameterType... types) {
            return VolumeParameterType.in(this, types);
        }
    };

    private static final int DEFAULT_MINIMUM_VOLUME_SIZE_IN_GIBIBYTES = 10;

    private static final int DEFAULT_MAXIMUM_VOLUME_SIZE_IN_GIBIBYTES = 4095;

    private static final int DEFAULT_MINIMUM_VOLUME_COUNT = 1;

    public abstract VolumeParameterConfig getVolumeParameterByType(VmTypeMeta metaData);

    public abstract void buildForVmTypeMetaBuilder(VmTypeMetaBuilder builder, int maxDataDiskCount);

    public abstract boolean in(VolumeParameterType... types);

    private static boolean in(VolumeParameterType type, VolumeParameterType... subset) {
        if (subset != null) {
            for (VolumeParameterType volumeParameterType : subset) {
                if (type == volumeParameterType) {
                    return true;
                }
            }
        }
        return false;
    }

    private static VolumeParameterConfig volumeParameterConfig(VolumeParameterType volumeParameterType, int maxDataDiskCount) {
        return new VolumeParameterConfig(
                volumeParameterType,
                DEFAULT_MINIMUM_VOLUME_SIZE_IN_GIBIBYTES,
                DEFAULT_MAXIMUM_VOLUME_SIZE_IN_GIBIBYTES,
                DEFAULT_MINIMUM_VOLUME_COUNT,
                maxDataDiskCount);
    }

}
