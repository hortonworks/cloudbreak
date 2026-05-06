package com.sequenceiq.it.cloudbreak.cloud.v4.openstack;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.it.cloudbreak.cloud.v4.VerticalScaleProperties;

@Configuration
@ConfigurationProperties(prefix = "integrationtest.openstack")
public class OpenstackProperties {

    private String availabilityZone;

    private String region;

    private String location;

    private String networkId;

    private String routerId;

    private String publicNetId;

    private Set<String> subnetIds;

    private String publicKeyId;

    private String osType = "redhat9";

    private final Instance instance = new Instance();

    private final Credential credential = new Credential();

    private final Image freeipaImage = new Image();

    private final Image sdxImage = new Image();

    private VerticalScaleProperties verticalScale = new VerticalScaleProperties();

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
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

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getRouterId() {
        return routerId;
    }

    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }

    public String getPublicNetId() {
        return publicNetId;
    }

    public void setPublicNetId(String publicNetId) {
        this.publicNetId = publicNetId;
    }

    public Set<String> getSubnetIds() {
        return subnetIds;
    }

    public void setSubnetIds(Set<String> subnetIds) {
        this.subnetIds = subnetIds;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public void setPublicKeyId(String publicKeyId) {
        this.publicKeyId = publicKeyId;
    }

    public String getOsType() {
        return osType;
    }

    public void setOsType(String osType) {
        this.osType = osType;
    }

    public Instance getInstance() {
        return instance;
    }

    public Credential getCredential() {
        return credential;
    }

    public Image getFreeipaImage() {
        return freeipaImage;
    }

    public Image getSdxImage() {
        return sdxImage;
    }

    public VerticalScaleProperties getVerticalScale() {
        return verticalScale;
    }

    public void setVerticalScale(VerticalScaleProperties verticalScale) {
        this.verticalScale = verticalScale;
    }

    public static class Instance {

        private String type = "m1.large";

        private String idbrokerType = "m1.xlarge";

        private String masterType = "r2.xlarge";

        private Integer volumeSize = 100;

        private Integer volumeCount = 1;

        private String volumeType = "PURE-ISCSI";

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getIdbrokerType() {
            return idbrokerType;
        }

        public void setIdbrokerType(String idbrokerType) {
            this.idbrokerType = idbrokerType;
        }

        public String getMasterType() {
            return masterType;
        }

        public void setMasterType(String masterType) {
            this.masterType = masterType;
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

        private String facing;

        private String userName;

        private String password;

        private String userDomain;

        private String projectDomainName;

        private String projectName;

        private String domainName;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getFacing() {
            return facing;
        }

        public void setFacing(String facing) {
            this.facing = facing;
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

        public String getUserDomain() {
            return userDomain;
        }

        public void setUserDomain(String userDomain) {
            this.userDomain = userDomain;
        }

        public String getProjectDomainName() {
            return projectDomainName;
        }

        public void setProjectDomainName(String projectDomainName) {
            this.projectDomainName = projectDomainName;
        }

        public String getProjectName() {
            return projectName;
        }

        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }

        public String getDomainName() {
            return domainName;
        }

        public void setDomainName(String domainName) {
            this.domainName = domainName;
        }
    }

    public static class Image {

        private String catalog;

        private String url;

        private String id;

        public String getCatalog() {
            return catalog;
        }

        public void setCatalog(String catalog) {
            this.catalog = catalog;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
