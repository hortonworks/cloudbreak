package com.sequenceiq.cloudbreak.converter.spi;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REQUESTED;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_CRN_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_USAGE_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATE_REQUESTED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.DELETE_REQUESTED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.cloudbreak.util.NullUtil.putIfPresent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.GroupSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.PortDefinition;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.converter.InstanceMetadataToImageIdConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeUsageType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.TargetGroupPersistenceService;
import com.sequenceiq.cloudbreak.template.VolumeUtils;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class StackToCloudStackConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackToCloudStackConverter.class);

    @Inject
    private ImageService imageService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Inject
    private InstanceMetadataToImageIdConverter instanceMetadataToImageIdConverter;

    @Inject
    private FileSystemConverter fileSystemConverter;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private CloudFileSystemViewProvider cloudFileSystemViewProvider;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private TargetGroupPersistenceService targetGroupPersistenceService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    public CloudStack convert(Stack stack) {
        return convert(stack, Collections.emptySet());
    }

    public CloudStack convertForDownscale(Stack stack, Set<String> deleteRequestedInstances) {
        return convert(stack, deleteRequestedInstances);
    }

    public CloudStack convert(Stack stack, Collection<String> deleteRequestedInstances) {
        Image image = null;
        String environmentCrn = stack.getEnvironmentCrn();
        DetailedEnvironmentResponse environment = environmentClientService.getByCrnAsInternal(environmentCrn);
        List<Group> instanceGroups = buildInstanceGroups(stack,
                stack.getInstanceGroupsAsList(),
                stack.getStackAuthentication(),
                deleteRequestedInstances,
                environment);
        try {
            image = imageService.getImage(stack.getId());
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.debug(e.getMessage());
        }
        Network network = buildNetwork(stack);
        StackTemplate stackTemplate = componentConfigProviderService.getStackTemplate(stack.getId());
        InstanceAuthentication instanceAuthentication = buildInstanceAuthentication(stack.getStackAuthentication());
        SpiFileSystem cloudFileSystem = buildSpiFileSystem(stack);
        SpiFileSystem additionalCloudFileSystem = buildAdditionalSpiFileSystem(stack);
        String template = null;
        if (stackTemplate != null) {
            template = stackTemplate.getTemplate();
        }
        Map<String, String> parameters = buildCloudStackParameters(stack, environment);
        List<CloudLoadBalancer> cloudLoadBalancers = buildLoadBalancers(stack, instanceGroups);

        return new CloudStack(instanceGroups, network, image, parameters, getUserDefinedTags(stack), template,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(),
                cloudFileSystem, cloudLoadBalancers, additionalCloudFileSystem);
    }

    public List<CloudInstance> buildInstances(Stack stack, DetailedEnvironmentResponse environment) {
        List<Group> groups = buildInstanceGroups(stack, stack.getInstanceGroupsAsList(), stack.getStackAuthentication(),
                Collections.emptySet(), environment);
        List<CloudInstance> cloudInstances = new ArrayList<>();
        for (Group group : groups) {
            cloudInstances.addAll(group.getInstances());
        }
        return cloudInstances;
    }

    public List<CloudInstance> buildInstances(Stack stack) {
        return buildInstances(stack, environmentClientService.getByCrnAsInternal(stack.getEnvironmentCrn()));
    }

    public CloudInstance buildInstance(InstanceMetaData instanceMetaData, InstanceGroup instanceGroup,
            StackAuthentication stackAuthentication, Long privateId, InstanceStatus status, DetailedEnvironmentResponse environment) {
        return buildInstance(instanceMetaData,
                instanceGroup,
                stackAuthentication,
                privateId,
                status,
                environment,
                NetworkScaleDetails.getEmpty());
    }

    public CloudInstance buildInstance(InstanceMetaData instanceMetaData, InstanceGroup instanceGroup, StackAuthentication stackAuthentication, Long privateId,
            InstanceStatus status, DetailedEnvironmentResponse environment, NetworkScaleDetails networkScaleDetails) {
        LOGGER.debug("Instance metadata is {}", instanceMetaData);
        String id = instanceMetaData == null ? null : instanceMetaData.getInstanceId();
        String instanceImageId = instanceMetaData == null ? null : instanceMetadataToImageIdConverter.convert(instanceMetaData);
        String name = instanceGroup.getGroupName();
        Stack stack = instanceGroup.getStack();
        InstanceTemplate instanceTemplate = buildInstanceTemplate(instanceGroup.getTemplate(), name, privateId, status, instanceImageId);
        InstanceAuthentication instanceAuthentication = buildInstanceAuthentication(stackAuthentication);

        Map<String, Object> parameters = buildCloudInstanceParameters(
                environment, instanceMetaData, CloudPlatform.valueOf(stack.getCloudPlatform()));
        return new CloudInstance(
                id,
                instanceTemplate,
                instanceAuthentication,
                instanceMetaData == null ? null : instanceMetaData.getSubnetId(),
                instanceMetaData == null ? null : instanceMetaData.getAvailabilityZone(),
                parameters);
    }

    private String getStackSubnetIdIfExists(Stack stack) {
        return Optional.ofNullable(stack.getNetwork())
                .map(com.sequenceiq.cloudbreak.domain.Network::getAttributes)
                .map(Json::getMap)
                .map(attr -> attr.get("subnetId"))
                .map(Object::toString)
                .orElse(null);
    }

    InstanceTemplate buildInstanceTemplate(Template template, String name, Long privateId, InstanceStatus status, String instanceImageId) {
        Json attributesJson = template.getAttributes();
        Map<String, Object> attributes = Optional.ofNullable(attributesJson).map(Json::getMap).orElseGet(HashMap::new);
        Json fromVault = template.getSecretAttributes() == null ? null : new Json(template.getSecretAttributes());
        Map<String, Object> secretAttributes = Optional.ofNullable(fromVault).map(Json::getMap).orElseGet(HashMap::new);

        Map<String, Object> fields = new HashMap<>();
        fields.putAll(attributes);
        fields.putAll(secretAttributes);

        List<Volume> volumes = new ArrayList<>();
        template.getVolumeTemplates().forEach(volumeModel -> {
            for (int i = 0; i < volumeModel.getVolumeCount(); i++) {
                String mount = volumeModel.getUsageType() == VolumeUsageType.GENERAL ? VolumeUtils.VOLUME_PREFIX + (i + 1) : VolumeUtils.DATABASE_VOLUME;
                Volume volume = new Volume(mount, volumeModel.getVolumeType(), volumeModel.getVolumeSize(), getVolumeUsageType(volumeModel.getUsageType()));
                volumes.add(volume);
            }
        });
        return new InstanceTemplate(template.getInstanceType(), name, privateId, volumes, status, fields, template.getId(), instanceImageId,
                template.getTemporaryStorage());
    }

    private CloudVolumeUsageType getVolumeUsageType(VolumeUsageType volumeUsageType) {
        CloudVolumeUsageType cloudVolumeUsageType;
        switch (volumeUsageType) {
            case DATABASE:
                cloudVolumeUsageType = CloudVolumeUsageType.DATABASE;
                break;
            case GENERAL:
            default:
                cloudVolumeUsageType = CloudVolumeUsageType.GENERAL;
                break;
        }
        return cloudVolumeUsageType;
    }

    private Map<String, String> getUserDefinedTags(Stack stack) {
        Map<String, String> result = Maps.newHashMap();
        try {
            if (stack.getTags() != null) {
                StackTags stackTag = stack.getTags().get(StackTags.class);
                Map<String, String> userDefined = stackTag.getUserDefinedTags();
                Map<String, String> defaultTags = stackTag.getDefaultTags();
                if (userDefined != null) {
                    result.putAll(userDefined);
                }
                if (defaultTags != null) {
                    result.putAll(defaultTags);
                }
            }
        } catch (IOException e) {
            LOGGER.info("Exception during converting user defined tags.", e);
        }
        return result;
    }

    private List<Group> buildInstanceGroups(Stack stack, List<InstanceGroup> instanceGroups,
            StackAuthentication stackAuthentication, Collection<String> deleteRequests, DetailedEnvironmentResponse environment) {
        // sort by name to avoid shuffling the different instance groups
        Collections.sort(instanceGroups);
        List<Group> groups = new ArrayList<>();
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            String blueprintText = cluster.getBlueprint() != null ? cluster.getBlueprint().getBlueprintText() : cluster.getExtendedBlueprintText();
            if (blueprintText != null) {
                CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintText);
                Map<String, Set<String>> componentsByHostGroup = cmTemplateProcessor.getComponentsByHostGroup();
                Map<String, String> userDefinedTags = getUserDefinedTags(stack);
                for (InstanceGroup instanceGroup : instanceGroups) {
                    if (instanceGroup.getTemplate() != null) {
                        InstanceAuthentication instanceAuthentication = buildInstanceAuthentication(stackAuthentication);
                        Optional<CloudFileSystemView> cloudFileSystemView
                                = cloudFileSystemViewProvider.getCloudFileSystemView(cluster.getFileSystem(), componentsByHostGroup, instanceGroup);
                        groups.add(
                                new Group(instanceGroup.getGroupName(),
                                        instanceGroup.getInstanceGroupType(),
                                        buildCloudInstances(environment, stackAuthentication, deleteRequests, instanceGroup),
                                        buildSecurity(instanceGroup),
                                        buildCloudInstanceSkeleton(environment, stackAuthentication, instanceGroup),
                                        getFields(instanceGroup),
                                        instanceAuthentication,
                                        instanceAuthentication.getLoginUserName(),
                                        instanceAuthentication.getPublicKey(),
                                        getRootVolumeSize(instanceGroup),
                                        cloudFileSystemView,
                                        buildDeletedCloudInstances(environment, stackAuthentication, instanceGroup),
                                        buildGroupNetwork(stack.getNetwork(), instanceGroup),
                                        userDefinedTags)
                        );
                    }
                }
            }
        } else {
            LOGGER.warn("Cluster or blueprint is null for stack id:[{}] name:[{}]", stack.getId(), stack.getName());
        }
        return groups;
    }

    private List<CloudLoadBalancer> buildLoadBalancers(Stack stack, List<Group> instanceGroups) {
        List<CloudLoadBalancer> cloudLoadBalancers = new ArrayList<>();
        for (LoadBalancer loadBalancer : loadBalancerPersistenceService.findByStackId(stack.getId())) {
            CloudLoadBalancer cloudLoadBalancer = new CloudLoadBalancer(loadBalancer.getType());
            for (TargetGroup targetGroup : targetGroupPersistenceService.findByLoadBalancerId(loadBalancer.getId())) {
                Set<TargetGroupPortPair> portPairs = loadBalancerConfigService.getTargetGroupPortPairs(targetGroup);
                Set<String> targetInstanceGroupName = instanceGroupService.findByTargetGroupId(targetGroup.getId()).stream()
                        .map(InstanceGroup::getGroupName)
                        .collect(Collectors.toSet());

                for (TargetGroupPortPair portPair : portPairs) {
                    cloudLoadBalancer.addPortToTargetGroupMapping(portPair, instanceGroups.stream()
                            .filter(ig -> targetInstanceGroupName.contains(ig.getName()))
                            .collect(Collectors.toSet()));
                }
            }
            cloudLoadBalancers.add(cloudLoadBalancer);
        }
        return cloudLoadBalancers;
    }

    private InstanceAuthentication buildInstanceAuthentication(StackAuthentication stackAuthentication) {
        return new InstanceAuthentication(
                stackAuthentication.getPublicKey(),
                stackAuthentication.getPublicKeyId(),
                stackAuthentication.getLoginUserName());
    }

    private List<CloudInstance> buildCloudInstances(DetailedEnvironmentResponse environment,
            StackAuthentication stackAuthentication,
            Collection<String> deleteRequests,
            InstanceGroup instanceGroup) {
        List<CloudInstance> instances = new ArrayList<>();
        // existing instances
        for (InstanceMetaData metaData : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
            InstanceStatus status = getInstanceStatus(metaData, deleteRequests);
            instances.add(buildInstance(metaData, instanceGroup, stackAuthentication, metaData.getPrivateId(), status, environment));
        }
        return instances;
    }

    private List<CloudInstance> buildDeletedCloudInstances(DetailedEnvironmentResponse environment,
            StackAuthentication stackAuthentication,
            InstanceGroup instanceGroup) {
        List<CloudInstance> instances = new ArrayList<>();
        for (InstanceMetaData metaData : instanceGroup.getDeletedInstanceMetaDataSet()) {
            instances.add(buildInstance(metaData, instanceGroup, stackAuthentication, metaData.getPrivateId(), TERMINATED, environment));
        }
        return instances;
    }

    private Security buildSecurity(InstanceGroup ig) {
        List<SecurityRule> rules = new ArrayList<>();
        if (ig.getSecurityGroup() == null) {
            return new Security(rules, Collections.emptyList());
        }
        SecurityGroup securityGroup = ig.getSecurityGroup();
        Set<com.sequenceiq.cloudbreak.domain.SecurityRule> securityRules = securityGroup.getSecurityRules();
        for (com.sequenceiq.cloudbreak.domain.SecurityRule securityRule : securityRules) {
            List<PortDefinition> portDefinitions = new ArrayList<>();
            for (String actualPort : securityRule.getPorts()) {
                String[] segments = actualPort.split("-");
                if (segments.length > 1) {
                    portDefinitions.add(new PortDefinition(segments[0], segments[1]));
                } else {
                    portDefinitions.add(new PortDefinition(segments[0], segments[0]));
                }
            }

            rules.add(new SecurityRule(securityRule.getCidr(), portDefinitions.toArray(new PortDefinition[portDefinitions.size()]),
                    securityRule.getProtocol()));
        }
        return new Security(rules, securityGroup.getSecurityGroupIds(), true);
    }

    private CloudInstance buildCloudInstanceSkeleton(DetailedEnvironmentResponse environment, StackAuthentication stackAuthentication,
            InstanceGroup instanceGroup) {
        CloudInstance skeleton = null;
        if (instanceGroup.getNodeCount() == 0) {
            skeleton = buildInstance(null, instanceGroup, stackAuthentication, 0L,
                    CREATE_REQUESTED, environment);
        }
        return skeleton;
    }

    private Map<String, Object> getFields(InstanceGroup instanceGroup) {
        Json attributes = instanceGroup.getAttributes();
        return attributes == null ? Collections.emptyMap() : attributes.getMap();
    }

    private Integer getRootVolumeSize(InstanceGroup instanceGroup) {
        Template template = instanceGroup.getTemplate();
        Integer rootVolumeSize = template.getRootVolumeSize();
        if (Objects.isNull(rootVolumeSize)) {
            rootVolumeSize = defaultRootVolumeSizeProvider.getForPlatform(template.cloudPlatform());
        }
        return rootVolumeSize;
    }

    private SpiFileSystem buildSpiFileSystem(Stack stack) {
        SpiFileSystem spiFileSystem = null;
        if (stack.getCluster() != null) {
            FileSystem fileSystem = stack.getCluster().getFileSystem();
            if (fileSystem != null) {
                spiFileSystem = fileSystemConverter.fileSystemToSpi(fileSystem);
            }
        }
        return spiFileSystem;
    }

    private SpiFileSystem buildAdditionalSpiFileSystem(Stack stack) {
        SpiFileSystem spiFileSystem = null;
        if (stack.getCluster() != null) {
            FileSystem fileSystem = stack.getCluster().getAdditionalFileSystem();
            if (fileSystem != null) {
                spiFileSystem = fileSystemConverter.fileSystemToSpi(fileSystem);
            }
        }
        return spiFileSystem;
    }

    public GroupNetwork buildGroupNetwork(com.sequenceiq.cloudbreak.domain.Network stackNetwork, InstanceGroup instanceGroup) {
        GroupNetwork groupNetwork = null;
        InstanceGroupNetwork instanceGroupNetwork = instanceGroup.getInstanceGroupNetwork();
        if (instanceGroupNetwork != null) {
            Json attributes = instanceGroupNetwork.getAttributes();
            Map<String, Object> params = attributes == null ? Collections.emptyMap() : attributes.getMap();
            Set<GroupSubnet> subnets = new HashSet<>();
            Set<GroupSubnet> endpointGatewaySubnets = new HashSet<>();
            if (params != null) {
                List<String> subnetIds = (List<String>) params.getOrDefault(NetworkConstants.SUBNET_IDS, new ArrayList<>());
                for (String subnetId : subnetIds) {
                    GroupSubnet groupSubnet = new GroupSubnet(subnetId);
                    subnets.add(groupSubnet);
                }
                List<String> endpointGatewaySubnetIds = (List<String>) params.getOrDefault(NetworkConstants.ENDPOINT_GATEWAY_SUBNET_IDS,
                        new ArrayList<>());
                for (String endpointGatewaySubnetId : endpointGatewaySubnetIds) {
                    GroupSubnet groupSubnet = new GroupSubnet(endpointGatewaySubnetId);
                    endpointGatewaySubnets.add(groupSubnet);
                }
            }
            groupNetwork = new GroupNetwork(getOutboundInternetTraffic(stackNetwork), subnets, endpointGatewaySubnets, params);
        }
        return groupNetwork;
    }

    public Network buildNetwork(Stack stack) {
        com.sequenceiq.cloudbreak.domain.Network stackNetwork = stack.getNetwork();
        Network result = null;
        if (stackNetwork != null) {
            Subnet subnet = new Subnet(stackNetwork.getSubnetCIDR());
            Json attributes = stackNetwork.getAttributes();
            Map<String, Object> params = attributes == null ? Collections.emptyMap() : attributes.getMap();
            result = new Network(subnet, stackNetwork.getNetworkCidrs(), stackNetwork.getOutboundInternetTraffic(), params);
        }
        return result;
    }

    private OutboundInternetTraffic getOutboundInternetTraffic(com.sequenceiq.cloudbreak.domain.Network stackNetwork) {
        return stackNetwork == null ? OutboundInternetTraffic.ENABLED : stackNetwork.getOutboundInternetTraffic();
    }

    private InstanceStatus getInstanceStatus(InstanceMetaData metaData, Collection<String> deleteRequests) {
        InstanceStatus status;
        if (deleteRequests.contains(metaData.getInstanceId())) {
            status = DELETE_REQUESTED;
        } else if (metaData.getInstanceStatus() == REQUESTED) {
            status = CREATE_REQUESTED;
        } else {
            status = InstanceStatus.CREATED;
        }
        return status;
    }

    public Map<String, Object> buildCloudInstanceParameters(DetailedEnvironmentResponse environment, InstanceMetaData instanceMetaData,
            CloudPlatform platform) {
        Map<String, Object> params = new HashMap<>();
        putIfPresent(params, CloudInstance.DISCOVERY_NAME, getIfNotNull(instanceMetaData, InstanceMetaData::getShortHostname));
        putIfPresent(params, SUBNET_ID, getIfNotNull(instanceMetaData, InstanceMetaData::getSubnetId));
        putIfPresent(params, CloudInstance.INSTANCE_NAME, getIfNotNull(instanceMetaData, InstanceMetaData::getInstanceName));
        putIfPresent(params, CloudInstance.FQDN, getIfNotNull(instanceMetaData, InstanceMetaData::getDiscoveryFQDN));
        Optional<AzureResourceGroup> resourceGroupOptional = getAzureResourceGroup(environment, platform);
        if (resourceGroupOptional.isPresent() && !ResourceGroupUsage.MULTIPLE.equals(resourceGroupOptional.get().getResourceGroupUsage())) {
            AzureResourceGroup resourceGroup = resourceGroupOptional.get();
            String resourceGroupName = resourceGroup.getName();
            ResourceGroupUsage resourceGroupUsage = resourceGroup.getResourceGroupUsage();
            params.put(RESOURCE_GROUP_NAME_PARAMETER, resourceGroupName);
            params.put(RESOURCE_GROUP_USAGE_PARAMETER, resourceGroupUsage.name());
        }
        return params;
    }

    private Map<String, String> buildCloudStackParameters(Stack stack, DetailedEnvironmentResponse environment) {
        Map<String, String> params = new HashMap<>(stack.getParameters());
        Optional<AzureResourceGroup> resourceGroupOptional = getAzureResourceGroup(environment, CloudPlatform.valueOf(stack.getCloudPlatform()));
        if (resourceGroupOptional.isPresent() && !ResourceGroupUsage.MULTIPLE.equals(resourceGroupOptional.get().getResourceGroupUsage())) {
            AzureResourceGroup resourceGroup = resourceGroupOptional.get();
            String resourceGroupName = resourceGroup.getName();
            ResourceGroupUsage resourceGroupUsage = resourceGroup.getResourceGroupUsage();
            params.put(RESOURCE_GROUP_NAME_PARAMETER, resourceGroupName);
            params.put(RESOURCE_GROUP_USAGE_PARAMETER, resourceGroupUsage.name());
        }
        params.put(RESOURCE_CRN_PARAMETER, stack.getResourceCrn());
        return params;
    }

    private Optional<AzureResourceGroup> getAzureResourceGroup(DetailedEnvironmentResponse environment, CloudPlatform platform) {
        return CloudPlatform.AZURE.equals(platform) ? getResourceGroupFromEnv(environment) : Optional.empty();
    }

    private Optional<AzureResourceGroup> getResourceGroupFromEnv(DetailedEnvironmentResponse environment) {
        return Optional.ofNullable(environment)
                .map(DetailedEnvironmentResponse::getAzure)
                .map(AzureEnvironmentParameters::getResourceGroup);
    }

}
