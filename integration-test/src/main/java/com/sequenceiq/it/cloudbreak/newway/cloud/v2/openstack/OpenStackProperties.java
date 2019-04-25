package com.sequenceiq.it.cloudbreak.newway.cloud.v2.openstack;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "integrationtest.openstack")
public class OpenStackProperties {

    private String defaultBlueprintName;

    private String availabilityZone;

    private String region;

    private String location;

    private String publicNetId;

    private String networkingOption;

    private final Instance instance = new Instance();

    private final Credential credential = new Credential();

    private final Prewarmed prewarmed = new Prewarmed();

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public String getDefaultBlueprintName() {
        return defaultBlueprintName;
    }

    public void setDefaultBlueprintName(String defaultBlueprintName) {
        this.defaultBlueprintName = defaultBlueprintName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPublicNetId() {
        return publicNetId;
    }

    public void setPublicNetId(String publicNetId) {
        this.publicNetId = publicNetId;
    }

    public String getNetworkingOption() {
        return networkingOption;
    }

    public void setNetworkingOption(String networkingOption) {
        this.networkingOption = networkingOption;
    }

    public Instance getInstance() {
        return instance;
    }

    public Credential getCredential() {
        return credential;
    }

    public Prewarmed getPrewarmed() {
        return prewarmed;
    }

    public static class Instance {
        private String type;

        private Integer volumeSize;

        private Integer volumeCount;

        private String volumeType;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getVolumeSize() {
            return volumeSize;
        }

        public void setVolumeSize(Integer volumeSize) {
            this.volumeSize = volumeSize;
        }

        public Integer getVolumeCount() {
            return volumeCount;
        }

        public void setVolumeCount(Integer volumeCount) {
            this.volumeCount = volumeCount;
        }

        public String getVolumeType() {
            return volumeType;
        }

        public void setVolumeType(String volumeType) {
            this.volumeType = volumeType;
        }
    }

    public static class Credential {
        private String endpoint;

        private String tenant;

        private String userName;

        private String password;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getTenant() {
            return tenant;
        }

        public void setTenant(String tenant) {
            this.tenant = tenant;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Prewarmed {
        private final Hdp hdp = new Hdp();

        private final Hdf hdf = new Hdf();

        public Hdp getHdp() {
            return hdp;
        }

        public Hdf getHdf() {
            return hdf;
        }

        public static class Hdp {
            private String version;

            private Boolean enabled;

            private List<String> hostGroups;

            private String scaleGroup;

            public String getVersion() {
                return version;
            }

            public void setVersion(String version) {
                this.version = version;
            }

            public Boolean getEnabled() {
                return enabled;
            }

            public void setEnabled(Boolean enabled) {
                this.enabled = enabled;
            }

            public List<String> getHostGroups() {
                return hostGroups;
            }

            public void setHostGroups(List<String> hostGroups) {
                this.hostGroups = hostGroups;
            }

            public String getScaleGroup() {
                return scaleGroup;
            }

            public void setScaleGroup(String scaleGroup) {
                this.scaleGroup = scaleGroup;
            }
        }

        public static class Hdf {
            private List<String> blueprintNames;

            private Boolean enabled;

            private List<String> hostGroups;

            private String scaleGroup;

            public List<String> getBlueprintNames() {
                return blueprintNames;
            }

            public void setBlueprintNames(List<String> blueprintNames) {
                this.blueprintNames = blueprintNames;
            }

            public Boolean getEnabled() {
                return enabled;
            }

            public void setEnabled(Boolean enabled) {
                this.enabled = enabled;
            }

            public List<String> getHostGroups() {
                return hostGroups;
            }

            public void setHostGroups(List<String> hostGroups) {
                this.hostGroups = hostGroups;
            }

            public String getScaleGroup() {
                return scaleGroup;
            }

            public void setScaleGroup(String scaleGroup) {
                this.scaleGroup = scaleGroup;
            }
        }
    }
}
