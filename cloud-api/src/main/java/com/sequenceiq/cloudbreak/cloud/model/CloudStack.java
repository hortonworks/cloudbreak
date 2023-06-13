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

    public CloudStack(Collection<Group> groups, Network network, Image image, Map<String, String> parameters, Map<String, String> tags, String template,
            InstanceAuthentication instanceAuthentication, String loginUserName, String publicKey, SpiFileSystem fileSystem,
            String gatewayUserData, String coreUserData) {
        this(groups, network, image, parameters, tags, template, instanceAuthentication, loginUserName, publicKey, fileSystem,
                Collections.emptyList(), gatewayUserData, coreUserData);
    }

    public CloudStack(Collection<Group> groups, Network network, Image image, Map<String, String> parameters, Map<String, String> tags, String template,
            InstanceAuthentication instanceAuthentication, String loginUserName, String publicKey, SpiFileSystem fileSystem,
            String gatewayUserData, String coreUserData, boolean multiAz) {
        this(groups, network, image, parameters, tags, template, instanceAuthentication, loginUserName, publicKey, fileSystem,
                Collections.emptyList(), null, gatewayUserData, coreUserData, multiAz);
    }

    public CloudStack(Collection<Group> groups, Network network, Image image, Map<String, String> parameters, Map<String, String> tags,
            String template, InstanceAuthentication instanceAuthentication, String loginUserName, String publicKey, SpiFileSystem fileSystem,
            List<CloudLoadBalancer> loadBalancers, String gatewayUserData, String coreUserData) {
        this(groups, network, image, parameters, tags, template, instanceAuthentication, loginUserName, publicKey, fileSystem,
                loadBalancers, null, gatewayUserData, coreUserData, false);
    }

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
            @JsonProperty("multiAz") boolean multiAz) {
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
    }

    public CloudStack replaceImage(Image newImage) {
        return new CloudStack(groups, network, newImage, parameters, tags, template, instanceAuthentication, loginUserName, publicKey,
                fileSystem.orElse(null), loadBalancers, additionalFileSystem.orElse(null), gatewayUserData, coreUserData, multiAz);
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
                '}';
    }
}
