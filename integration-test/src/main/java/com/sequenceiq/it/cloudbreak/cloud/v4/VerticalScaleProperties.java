package com.sequenceiq.it.cloudbreak.cloud.v4;

public class VerticalScaleProperties {

    private boolean supported;

    private DataLakeProperties datalake;

    private DataHubProperties datahub;

    private FreeIpaProperties freeipa;

    private String volumeType;

    public String getVolumeType() {
        return volumeType;
    }

    public void setVolumeType(String volumeType) {
        this.volumeType = volumeType;
    }

    public FreeIpaProperties getFreeipa() {
        return freeipa;
    }

    public DataHubProperties getDatahub() {
        return datahub;
    }

    public DataLakeProperties getDatalake() {
        return datalake;
    }

    public boolean isSupported() {
        return supported;
    }

    public void setSupported(boolean supported) {
        this.supported = supported;
    }

    public void setDatalake(DataLakeProperties datalake) {
        this.datalake = datalake;
    }

    public void setDatahub(DataHubProperties datahub) {
        this.datahub = datahub;
    }

    public void setFreeipa(FreeIpaProperties freeipa) {
        this.freeipa = freeipa;
    }

    public static class DataHubProperties {
        private String group;

        private String instanceType;

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getInstanceType() {
            return instanceType;
        }

        public void setInstanceType(String instanceType) {
            this.instanceType = instanceType;
        }
    }

    public static class FreeIpaProperties {
        private String group;

        private String instanceType;

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getInstanceType() {
            return instanceType;
        }

        public void setInstanceType(String instanceType) {
            this.instanceType = instanceType;
        }
    }

    public static class DataLakeProperties {
        private String group;

        private String instanceType;

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getInstanceType() {
            return instanceType;
        }

        public void setInstanceType(String instanceType) {
            this.instanceType = instanceType;
        }
    }
}
