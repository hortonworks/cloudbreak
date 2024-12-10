package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.common.api.type.InstanceGroupType;

/**
 * Class that describes complete structure of infrastructure that needs to be started on the Cloud Provider
 */
public class CloudStack {

    private final List<Group> groups;

    private final Network network;

    private final Image image;

    private final String template;

    private final String loginUserName;

    private final String publicKey;

    private final Map<String, String> parameters;

    private final Map<String, String> tags;

    private final InstanceAuthentication instanceAuthentication;

    private final Optional<SpiFileSystem> fileSystem;

    private final Optional<SpiFileSystem> additionalFileSystem;

    private final List<CloudLoadBalancer> loadBalancers;

    private final String gatewayUserData;

    private final String coreUserData;

    private final boolean multiAz;

    private final String supportedImdsVersion;

    @JsonCreator
    public CloudStack(
            @JsonProperty("groups") Collection<Group> groups,
            @JsonProperty("network") Network network,
            @JsonProperty("image") Image image,
            @JsonProperty("parameters") Map<String, String> parameters,
            @JsonProperty("tags") Map<String, String> tags,
            @JsonProperty("template") String template,
            @JsonProperty("instanceAuthentication") InstanceAuthentication instanceAuthentication,
            @JsonProperty("loginUserName") String loginUserName,
            @JsonProperty("publicKey") String publicKey,
            @JsonProperty("fileSystem") SpiFileSystem fileSystem,
            @JsonProperty("loadBalancers") List<CloudLoadBalancer> loadBalancers,
            @JsonProperty("additionalFileSystem") SpiFileSystem additionalFileSystem,
            @JsonProperty("gatewayUserData") String gatewayUserData,
            @JsonProperty("coreUserData") String coreUserData,
            @JsonProperty("multiAz") boolean multiAz,
            @JsonProperty("supportedImdsVersion") String supportedImdsVersion) {
        this.groups = ImmutableList.copyOf(groups);
        this.network = network;
        this.image = image;
        this.parameters = ImmutableMap.copyOf(parameters);
        this.tags = ImmutableMap.copyOf(tags);
        this.template = template;
        this.instanceAuthentication = instanceAuthentication;
        this.loginUserName = loginUserName;
        this.publicKey = publicKey;
        this.fileSystem = Optional.ofNullable(fileSystem);
        this.loadBalancers = loadBalancers != null ? loadBalancers : Collections.emptyList();
        this.additionalFileSystem = Optional.ofNullable(additionalFileSystem);
        this.gatewayUserData = gatewayUserData;
        this.coreUserData = coreUserData;
        this.multiAz = multiAz;
        this.supportedImdsVersion = supportedImdsVersion;
    }

    private CloudStack(Builder builder) {
        this.groups = builder.groups;
        this.network = builder.network;
        this.image = builder.image;
        this.template = builder.template;
        this.loginUserName = builder.instanceAuthentication != null ? builder.instanceAuthentication.getLoginUserName() : null;
        this.publicKey = builder.instanceAuthentication != null ? builder.instanceAuthentication.getPublicKey() : null;
        this.parameters = builder.parameters;
        this.tags = builder.tags;
        this.instanceAuthentication = builder.instanceAuthentication;
        this.fileSystem = Optional.ofNullable(builder.fileSystem);
        this.additionalFileSystem = Optional.ofNullable(builder.additionalFileSystem);
        this.loadBalancers = builder.loadBalancers != null ? builder.loadBalancers : Collections.emptyList();
        this.gatewayUserData = builder.gatewayUserData;
        this.coreUserData = builder.coreUserData;
        this.multiAz = builder.multiAz;
        this.supportedImdsVersion = builder.supportedImdsVersion;
    }

    public static CloudStack replaceImage(CloudStack cloudStack, Image newImage) {
        return builder()
                .groups(cloudStack.getGroups())
                .network(cloudStack.getNetwork())
                .image(newImage)
                .template(cloudStack.getTemplate())
                .parameters(cloudStack.getParameters())
                .tags(cloudStack.getTags())
                .instanceAuthentication(cloudStack.getInstanceAuthentication())
                .fileSystem(cloudStack.getFileSystem().orElse(null))
                .additionalFileSystem(cloudStack.getAdditionalFileSystem().orElse(null))
                .loadBalancers(cloudStack.getLoadBalancers())
                .gatewayUserData(cloudStack.getGatewayUserData())
                .coreUserData(cloudStack.getCoreUserData())
                .multiAz(cloudStack.multiAz)
                .supportedImdsVersion(cloudStack.getSupportedImdsVersion())
                .build();
    }

    public static CloudStack replaceParameters(CloudStack cloudStack, Map<String, String> parameters) {
        return builder()
                .groups(cloudStack.getGroups())
                .network(cloudStack.getNetwork())
                .image(cloudStack.getImage())
                .template(cloudStack.getTemplate())
                .parameters(parameters)
                .tags(cloudStack.getTags())
                .instanceAuthentication(cloudStack.getInstanceAuthentication())
                .fileSystem(cloudStack.getFileSystem().orElse(null))
                .additionalFileSystem(cloudStack.getAdditionalFileSystem().orElse(null))
                .loadBalancers(cloudStack.getLoadBalancers())
                .gatewayUserData(cloudStack.getGatewayUserData())
                .coreUserData(cloudStack.getCoreUserData())
                .multiAz(cloudStack.multiAz)
                .supportedImdsVersion(cloudStack.getSupportedImdsVersion())
                .build();
    }

    public List<Group> getGroups() {
        return groups;
    }

    public Network getNetwork() {
        return network;
    }

    public Image getImage() {
        return image;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public Security getCloudSecurity() {
        return groups.get(0).getSecurity();
    }

    public InstanceAuthentication getInstanceAuthentication() {
        return instanceAuthentication;
    }

    public Optional<SpiFileSystem> getFileSystem() {
        return fileSystem;
    }

    public Optional<SpiFileSystem> getAdditionalFileSystem() {
        return additionalFileSystem;
    }

    public String getTemplate() {
        return template;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getLoginUserName() {
        return loginUserName;
    }

    public List<CloudLoadBalancer> getLoadBalancers() {
        return loadBalancers;
    }

    public String getGatewayUserData() {
        return gatewayUserData;
    }

    public String getCoreUserData() {
        return coreUserData;
    }

    public String getUserDataByType(InstanceGroupType key) {
        if (key.equals(InstanceGroupType.CORE)) {
            return coreUserData;
        } else {
            return gatewayUserData;
        }
    }

    public String getSupportedImdsVersion() {
        return supportedImdsVersion;
    }

    public boolean isMultiAz() {
        return multiAz;
    }

    @Override
    public String toString() {
        return "CloudStack{" +
                "groups=" + groups +
                ", network=" + network +
                ", image=" + image +
                ", template='" + template + '\'' +
                ", loginUserName='" + loginUserName + '\'' +
                ", publicKey='" + publicKey + '\'' +
                ", parameters=" + parameters +
                ", tags=" + tags +
                ", instanceAuthentication=" + instanceAuthentication +
                ", fileSystem=" + fileSystem +
                ", additionalFileSystem=" + additionalFileSystem +
                ", multiAz=" + multiAz +
                ", supportedImdsVersion=" + supportedImdsVersion +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<Group> groups = Collections.emptyList();

        private Network network;

        private Image image;

        private String template;

        private Map<String, String> parameters = Collections.emptyMap();

        private Map<String, String> tags = Collections.emptyMap();

        private InstanceAuthentication instanceAuthentication;

        private SpiFileSystem fileSystem;

        private SpiFileSystem additionalFileSystem;

        private List<CloudLoadBalancer> loadBalancers = Collections.emptyList();

        private String gatewayUserData;

        private String coreUserData;

        private boolean multiAz;

        private String supportedImdsVersion;

        public Builder groups(Collection<Group> groups) {
            this.groups = List.copyOf(groups);
            return this;
        }

        public Builder network(Network network) {
            this.network = network;
            return this;
        }

        public Builder image(Image image) {
            this.image = image;
            return this;
        }

        public Builder template(String template) {
            this.template = template;
            return this;
        }

        public Builder parameters(Map<String, String> parameters) {
            this.parameters = Map.copyOf(parameters);
            return this;
        }

        public Builder tags(Map<String, String> tags) {
            this.tags = Map.copyOf(tags);
            return this;
        }

        public Builder instanceAuthentication(InstanceAuthentication instanceAuthentication) {
            this.instanceAuthentication = instanceAuthentication;
            return this;
        }

        public Builder fileSystem(SpiFileSystem fileSystem) {
            this.fileSystem = fileSystem;
            return this;
        }

        public Builder additionalFileSystem(SpiFileSystem additionalFileSystem) {
            this.additionalFileSystem = additionalFileSystem;
            return this;
        }

        public Builder loadBalancers(List<CloudLoadBalancer> loadBalancers) {
            this.loadBalancers = loadBalancers;
            return this;
        }

        public Builder gatewayUserData(String gatewayUserData) {
            this.gatewayUserData = gatewayUserData;
            return this;
        }

        public Builder coreUserData(String coreUserData) {
            this.coreUserData = coreUserData;
            return this;
        }

        public Builder multiAz(boolean multiAz) {
            this.multiAz = multiAz;
            return this;
        }

        public Builder supportedImdsVersion(String supportedImdsVersion) {
            this.supportedImdsVersion = supportedImdsVersion;
            return this;
        }

        public CloudStack build() {
            return new CloudStack(this);
        }
    }
}
