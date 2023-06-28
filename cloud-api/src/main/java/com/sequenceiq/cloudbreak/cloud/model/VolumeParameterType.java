package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.VmTypeMetaBuilder;

public enum VolumeParameterType {

    MAGNETIC() {
        @Override public VolumeParameterConfig getVolumeParameterByType(VmTypeMeta metaData) {
            return metaData.getMagneticConfig();
        }

        @Override
        public void buildForVmTypeMetaBuilder(VmTypeMetaBuilder builder, int maxDataDiskCount, int maxdiskSize) {
            builder.withMagneticConfig(VolumeParameterType.volumeParameterConfig(this, maxDataDiskCount, maxdiskSize));
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
        public void buildForVmTypeMetaBuilder(VmTypeMetaBuilder builder, int maxDataDiskCount, int maxdiskSize) {
            builder.withSsdConfig(VolumeParameterType.volumeParameterConfig(this, maxDataDiskCount, maxdiskSize));
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

    LOCAL_SSD() {
        @Override public VolumeParameterConfig getVolumeParameterByType(VmTypeMeta metaData) {
            return metaData.getLocalSsdConfig();
        }

        @Override
        public void buildForVmTypeMetaBuilder(VmTypeMetaBuilder builder, Set<Integer> possibleSizeValues, Set<Integer> possibleNumberValue) {
            builder.withLocalSsdConfig(VolumeParameterType.volumeParameterConfig(this, possibleSizeValues, possibleNumberValue));
        }

        @Override
        public void buildForVmTypeMetaBuilder(VmTypeMetaBuilder builder, int maxDataDiskCount) {
            throw new IllegalStateException("This volume type must be built from Possible Size and Number Values instead of Max Disk Count and Size.");
        }

        @Override
        public void buildForVmTypeMetaBuilder(VmTypeMetaBuilder builder, int maxDataDiskCount, int maxdiskSize) {
            throw new IllegalStateException("This volume type must be built from Possible Size and Number Values instead of Max Disk Count and Size.");
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
        public void buildForVmTypeMetaBuilder(VmTypeMetaBuilder builder, int maxDataDiskCount, int maxdiskSize) {
            builder.withEphemeralConfig(VolumeParameterType.volumeParameterConfig(this, maxDataDiskCount, maxdiskSize));
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
        public void buildForVmTypeMetaBuilder(VmTypeMetaBuilder builder, int maxDataDiskCount, int maxdiskSize) {
            builder.withSt1Config(VolumeParameterType.volumeParameterConfig(this, maxDataDiskCount, maxdiskSize));
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
        public void buildForVmTypeMetaBuilder(VmTypeMetaBuilder builder, int maxDataDiskCount, int maxdiskSize) {
            builder.withAutoAttachedConfig(VolumeParameterType.volumeParameterConfig(this, maxDataDiskCount, maxdiskSize));
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

    public abstract void buildForVmTypeMetaBuilder(VmTypeMetaBuilder builder, int maxDataDiskCount, int maxdiskSize);

    public void buildForVmTypeMetaBuilder(VmTypeMetaBuilder builder, Set<Integer> possibleSizeValues, Set<Integer> possibleNumberValue) {
        throw new UnsupportedOperationException("Possible Size and Number Values are not supported with this volume type.");
    }

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

    private static VolumeParameterConfig volumeParameterConfig(VolumeParameterType volumeParameterType, int maxDataDiskCount, int maxdiskSize) {
        return new VolumeParameterConfig(
                volumeParameterType,
                DEFAULT_MINIMUM_VOLUME_SIZE_IN_GIBIBYTES,
                maxdiskSize,
                new HashSet<>(),
                DEFAULT_MINIMUM_VOLUME_COUNT,
                maxDataDiskCount,
                new HashSet<>());
    }

    private static VolumeParameterConfig volumeParameterConfig(VolumeParameterType volumeParameterType,
            Set<Integer> possibleSizeValues, Set<Integer> possibleNumberValue) {
        return new VolumeParameterConfig(
                volumeParameterType,
                DEFAULT_MINIMUM_VOLUME_SIZE_IN_GIBIBYTES,
                DEFAULT_MAXIMUM_VOLUME_SIZE_IN_GIBIBYTES,
                possibleSizeValues,
                DEFAULT_MINIMUM_VOLUME_COUNT,
                DEFAULT_MINIMUM_VOLUME_COUNT,
                possibleNumberValue);
    }

    private static VolumeParameterConfig volumeParameterConfig(VolumeParameterType volumeParameterType, int maxDataDiskCount) {
        return new VolumeParameterConfig(
                volumeParameterType,
                DEFAULT_MINIMUM_VOLUME_SIZE_IN_GIBIBYTES,
                DEFAULT_MAXIMUM_VOLUME_SIZE_IN_GIBIBYTES,
                new HashSet<>(),
                DEFAULT_MINIMUM_VOLUME_COUNT,
                maxDataDiskCount,
                new HashSet<>());
    }

}
