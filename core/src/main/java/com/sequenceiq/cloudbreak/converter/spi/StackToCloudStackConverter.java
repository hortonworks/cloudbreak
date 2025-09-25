package com.sequenceiq.cloudbreak.converter.spi;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REQUESTED;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ACCEPTANCE_POLICY_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.ENVIRONMENT_RESOURCE_ENCRYPTION_KEY;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_CRN_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_USAGE_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATE_REQUESTED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.DELETE_REQUESTED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.cloudbreak.util.NullUtil.putIfPresent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.cloud.exception.UserdataSecretsException;
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
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.converter.InstanceMetadataToImageIdConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.VolumeUsageType;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.environment.marketplace.AzureMarketplaceTermsClientService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.loadbalancer.TargetGroupPortProvider;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.TargetGroupPersistenceService;
import com.sequenceiq.cloudbreak.template.VolumeUtils;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpResourceEncryptionParameters;
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
    private EnvironmentService environmentClientService;

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private TargetGroupPersistenceService targetGroupPersistenceService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private TargetGroupPortProvider targetGroupPortProvider;

    @Inject
    private AzureMarketplaceTermsClientService azureMarketplaceTermsClientService;

    @Inject
    private UserDataService userDataService;

    @Inject
    private ResourceService resourceService;

    public CloudStack convert(StackDtoDelegate stack) {
        return convert(stack, Collections.emptySet());
    }

    public CloudStack convertForDownscale(StackDtoDelegate stack, Set<String> deleteRequestedInstances) {
        return convert(stack, deleteRequestedInstances);
    }

    public CloudStack convert(StackDtoDelegate stack, Collection<String> deleteRequestedInstances) {
        Image image = null;
        String environmentCrn = stack.getEnvironmentCrn();
        DetailedEnvironmentResponse environment = environmentClientService.getByCrnAsInternal(environmentCrn);
        List<Group> instanceGroups = buildInstanceGroups(stack,
                stack.getInstanceGroupDtos(),
                deleteRequestedInstances,
                environment);
        try {
            image = imageService.getImage(stack.getId());
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.debug(e.getMessage());
        }
        Network network = buildNetwork(stack.getNetwork());
        StackTemplate stackTemplate = componentConfigProviderService.getStackTemplate(stack.getId());
        InstanceAuthentication instanceAuthentication = buildInstanceAuthentication(stack.getStackAuthentication());
        SpiFileSystem cloudFileSystem = buildSpiFileSystem(stack);
        SpiFileSystem additionalCloudFileSystem = buildAdditionalSpiFileSystem(stack);
        String template = null;
        if (stackTemplate != null) {
            template = stackTemplate.getTemplate();
        }
        Map<String, String> parameters = buildCloudStackParameters(stack, environment);
        List<CloudLoadBalancer> cloudLoadBalancers = buildLoadBalancers(stack.getStack(), instanceGroups);

        Map<InstanceGroupType, String> userData = userDataService.getUserData(stack.getId());

        return CloudStack.builder()
                .groups(instanceGroups)
                .network(network)
                .image(image)
                .parameters(parameters)
                .tags(getUserDefinedTags(stack.getStack()))
                .template(template)
                .instanceAuthentication(instanceAuthentication)
                .fileSystem(cloudFileSystem)
                .loadBalancers(cloudLoadBalancers)
                .additionalFileSystem(additionalCloudFileSystem)
                .coreUserData(userData.get(InstanceGroupType.CORE))
                .gatewayUserData(userData.get(InstanceGroupType.GATEWAY))
                .multiAz(stack.getStack().isMultiAz())
                .supportedImdsVersion(stack.getSupportedImdsVersion())
                .build();
    }

    public List<CloudInstance> buildInstances(StackDtoDelegate stack, DetailedEnvironmentResponse environment) {
        List<Group> groups = buildInstanceGroups(stack, stack.getInstanceGroupDtos(), Collections.emptySet(), environment);
        List<CloudInstance> cloudInstances = new ArrayList<>();
        for (Group group : groups) {
            cloudInstances.addAll(group.getInstances());
        }
        return cloudInstances;
    }

    public List<CloudInstance> buildInstances(StackDtoDelegate stack) {
        return buildInstances(stack, environmentClientService.getByCrnAsInternal(stack.getEnvironmentCrn()));
    }

    public CloudInstance buildInstance(InstanceMetadataView instanceMetaData, InstanceGroupView instanceGroup,
            StackView stack, Long privateId, InstanceStatus status,
            DetailedEnvironmentResponse environment) {
        return buildInstance(instanceMetaData,
                instanceGroup,
                stack,
                privateId,
                status,
                environment,
                instanceGroup.getTemplate());
    }

    public CloudInstance buildInstance(InstanceMetadataView instanceMetaData, InstanceGroupView instanceGroup, StackView stack,
            Long privateId, InstanceStatus status, DetailedEnvironmentResponse environment,
            Template template) {
        LOGGER.trace("Instance metadata is {}", instanceMetaData == null ? null : instanceMetaData.getInstanceId());
        String id = instanceMetaData == null ? null : instanceMetaData.getInstanceId();
        String instanceImageId = instanceMetaData == null ? null : instanceMetadataToImageIdConverter.convert(instanceMetaData);
        String name = instanceGroup.getGroupName();
        InstanceTemplate instanceTemplate = buildInstanceTemplate(instanceGroup.getTemplate(), name, privateId, status, instanceImageId);
        InstanceAuthentication instanceAuthentication = buildInstanceAuthentication(stack.getStackAuthentication());

        Map<String, Object> parameters = buildCloudInstanceParameters(
                environment,
                stack,
                instanceMetaData,
                CloudPlatform.valueOf(instanceGroup.getTemplate().getCloudPlatform()),
                template);
        return new CloudInstance(
                id,
                instanceTemplate,
                instanceAuthentication,
                instanceMetaData == null ? null : instanceMetaData.getSubnetId(),
                instanceMetaData == null ? null : instanceMetaData.getAvailabilityZone(),
                parameters);
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

        // Collect all volume templates and sort them by template ID to ensure consistent ordering
        List<VolumeTemplate> allVolumeTemplates = sortVolumeTemplates(template);

        int generalVolumeMountCounter = 1;

        // Process all volume templates in order
        for (VolumeTemplate volumeModel : allVolumeTemplates) {
            for (int i = 0; i < volumeModel.getVolumeCount(); i++) {
                String mount;
                if (volumeModel.getUsageType() == VolumeUsageType.GENERAL) {
                    mount = VolumeUtils.VOLUME_PREFIX + generalVolumeMountCounter;
                    generalVolumeMountCounter++;
                } else {
                    mount = VolumeUtils.DATABASE_VOLUME;
                }

                Volume volume = new Volume(mount, volumeModel.getVolumeType(), volumeModel.getVolumeSize(), getVolumeUsageType(volumeModel.getUsageType()));
                volumes.add(volume);
                LOGGER.debug("The volume config is {}.", volume);
            }
        }

        LOGGER.debug("The volumes are {}.", volumes);
        return new InstanceTemplate(template.getInstanceType(), name, privateId, volumes, status, fields, template.getId(), instanceImageId,
                template.getTemporaryStorage(), Optional.ofNullable(template.getInstanceStorageCount()).orElse(0).longValue());
    }

    private List<VolumeTemplate> sortVolumeTemplates(Template template) {
        return template.getVolumeTemplates()
                .stream()
                .sorted(preserveOrdering())
                .toList();
    }

    private Comparator<VolumeTemplate> preserveOrdering() {
        return Comparator.comparing(VolumeTemplate::getId);
    }

    private CloudVolumeUsageType getVolumeUsageType(VolumeUsageType volumeUsageType) {
        return switch (volumeUsageType) {
            case DATABASE -> CloudVolumeUsageType.DATABASE;
            default -> CloudVolumeUsageType.GENERAL;
        };
    }

    private Map<String, String> getUserDefinedTags(StackView stack) {
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

    private List<Group> buildInstanceGroups(StackDtoDelegate stack, List<InstanceGroupDto> instanceGroups,
            Collection<String> deleteRequests, DetailedEnvironmentResponse environment) {
        StackAuthentication stackAuthentication = stack.getStackAuthentication();
        // sort by name to avoid shuffling the different instance groups
        instanceGroups.sort(Comparator.comparing(o -> o.getInstanceGroup().getGroupName()));
        List<Group> groups = new ArrayList<>();
        ClusterView cluster = stack.getCluster();
        if (cluster != null) {
            String blueprintText = stack.getBlueprint() != null ? stack.getBlueprintJsonText() : cluster.getExtendedBlueprintText();
            if (blueprintText != null) {
                CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintText);
                Map<String, Set<String>> componentsByHostGroup = cmTemplateProcessor.getComponentsByHostGroup();
                Map<String, String> userDefinedTags = getUserDefinedTags(stack.getStack());
                for (InstanceGroupDto instanceGroupDto : instanceGroups) {
                    InstanceGroupView instanceGroup = instanceGroupDto.getInstanceGroup();
                    if (instanceGroup.getTemplate() != null) {
                        InstanceAuthentication instanceAuthentication = buildInstanceAuthentication(stackAuthentication);
                        Optional<CloudFileSystemView> cloudFileSystemView
                                = cloudFileSystemViewProvider.getCloudFileSystemView(cluster.getFileSystem(), componentsByHostGroup, instanceGroup);
                        groups.add(Group.builder()
                                .withName(instanceGroup.getGroupName())
                                .withType(instanceGroup.getInstanceGroupType())
                                .withInstances(buildCloudInstances(environment, stack.getStack(), deleteRequests, instanceGroupDto))
                                .withSecurity(buildSecurity(instanceGroup))
                                .withSkeleton(buildCloudInstanceSkeleton(environment, stack.getStack(), instanceGroupDto))
                                .withParameters(getFields(instanceGroup))
                                .withInstanceAuthentication(instanceAuthentication)
                                .withLoginUserName(instanceAuthentication.getLoginUserName())
                                .withPublicKey(instanceAuthentication.getPublicKey())
                                .withRootVolumeSize(getRootVolumeSize(instanceGroup))
                                .withIdentity(cloudFileSystemView)
                                .withDeletedInstances(buildDeletedCloudInstances(environment, stack.getStack(), deleteRequests, instanceGroupDto))
                                .withNetwork(buildGroupNetwork(stack.getNetwork(), instanceGroup))
                                .withTags(userDefinedTags)
                                .withRootVolumeType(instanceGroup.getTemplate().getRootVolumeType())
                                .build());
                    }
                }
            }
        } else {
            LOGGER.warn("Cluster or blueprint is null for stack id:[{}] name:[{}]", stack.getId(), stack.getName());
        }
        return groups;
    }

    private List<CloudLoadBalancer> buildLoadBalancers(StackView stack, List<Group> instanceGroups) {
        List<CloudLoadBalancer> cloudLoadBalancers = new ArrayList<>();
        for (LoadBalancer loadBalancer : loadBalancerPersistenceService.findByStackId(stack.getId())) {
            boolean stickySessionForLBTargetGroup = false;
            CloudLoadBalancer temporaryLoadBalancer = new CloudLoadBalancer(loadBalancer.getType(), loadBalancer.getSku(), stickySessionForLBTargetGroup);
            for (TargetGroup targetGroup : targetGroupPersistenceService.findByLoadBalancerId(loadBalancer.getId())) {
                Set<TargetGroupPortPair> portPairs = targetGroupPortProvider.getTargetGroupPortPairs(targetGroup);
                Set<String> targetInstanceGroupName = instanceGroupService.findByTargetGroupId(targetGroup.getId()).stream()
                        .map(InstanceGroupView::getGroupName)
                        .collect(Collectors.toSet());

                for (TargetGroupPortPair portPair : portPairs) {
                    temporaryLoadBalancer.addPortToTargetGroupMapping(portPair, instanceGroups.stream()
                            .filter(ig -> targetInstanceGroupName.contains(ig.getName()))
                            .collect(Collectors.toSet()));
                }
                stickySessionForLBTargetGroup = targetGroup.isUseStickySession();
            }
            CloudLoadBalancer cloudLoadBalancer = new CloudLoadBalancer(loadBalancer.getType(), loadBalancer.getSku(), stickySessionForLBTargetGroup);
            cloudLoadBalancer.getPortToTargetGroupMapping().putAll(temporaryLoadBalancer.getPortToTargetGroupMapping());
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
            StackView stack,
            Collection<String> deleteRequests,
            InstanceGroupDto instanceGroupDto) {
        List<CloudInstance> instances = new ArrayList<>();
        // existing instances
        InstanceGroupView instanceGroupView = instanceGroupDto.getInstanceGroup();
        LOGGER.debug("There are {} instances will be added for {}", instanceGroupDto.getNotDeletedInstanceMetaData().size(),
                instanceGroupView.getGroupName());
        for (InstanceMetadataView metaData : instanceGroupDto.getNotDeletedInstanceMetaData()) {
            InstanceStatus status = getInstanceStatus(metaData, deleteRequests);
            instances.add(buildInstance(metaData, instanceGroupView, stack, metaData.getPrivateId(), status, environment));
        }
        return instances;
    }

    private List<CloudInstance> buildDeletedCloudInstances(DetailedEnvironmentResponse environment,
            StackView stack,
            Collection<String> deletedRequests,
            InstanceGroupDto instanceGroupDto) {
        List<CloudInstance> instances = new ArrayList<>();
        InstanceGroupView instanceGroupView = instanceGroupDto.getInstanceGroup();
        // the original set contained the terminated instances, but it can be several thousands and more.
        // We need to investigate how can exist cloud watches for the properly terminated instances (because we use this value only for cloud watch deletion)
        LOGGER.debug("There are {} deleted instances will be added for {}", instanceGroupDto.getDeletedInstanceMetaData().size(),
                instanceGroupView.getGroupName());
        for (InstanceMetadataView metaData : instanceGroupDto.getDeletedInstanceMetaData()) {
            instances.add(buildInstance(metaData, instanceGroupView, stack, metaData.getPrivateId(), TERMINATED, environment));
        }
        return instances;
    }

    private Security buildSecurity(InstanceGroupView ig) {
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

    private CloudInstance buildCloudInstanceSkeleton(DetailedEnvironmentResponse environment, StackView stack,
            InstanceGroupDto instanceGroup) {
        return buildInstance(null, instanceGroup.getInstanceGroup(), stack, 0L, null, environment);
    }

    private Map<String, Object> getFields(InstanceGroupView instanceGroup) {
        Json attributes = instanceGroup.getAttributes();
        return attributes == null ? Collections.emptyMap() : attributes.getMap();
    }

    private Integer getRootVolumeSize(InstanceGroupView instanceGroup) {
        Template template = instanceGroup.getTemplate();
        Integer rootVolumeSize = template.getRootVolumeSize();
        if (Objects.isNull(rootVolumeSize)) {
            rootVolumeSize = defaultRootVolumeSizeProvider.getDefaultRootVolumeForPlatform(template.getCloudPlatform(),
                    InstanceGroupType.isGateway(instanceGroup.getInstanceGroupType()));
        }
        return rootVolumeSize;
    }

    private SpiFileSystem buildSpiFileSystem(StackDtoDelegate stack) {
        SpiFileSystem spiFileSystem = null;
        if (stack.getCluster() != null) {
            FileSystem fileSystem = stack.getCluster().getFileSystem();
            if (fileSystem != null) {
                spiFileSystem = fileSystemConverter.fileSystemToSpi(fileSystem);
            }
        }
        return spiFileSystem;
    }

    private SpiFileSystem buildAdditionalSpiFileSystem(StackDtoDelegate stack) {
        SpiFileSystem spiFileSystem = null;
        if (stack.getCluster() != null) {
            FileSystem fileSystem = stack.getCluster().getAdditionalFileSystem();
            if (fileSystem != null) {
                spiFileSystem = fileSystemConverter.fileSystemToSpi(fileSystem);
            }
        }
        return spiFileSystem;
    }

    public GroupNetwork buildGroupNetwork(com.sequenceiq.cloudbreak.domain.Network stackNetwork, InstanceGroupView instanceGroup) {
        GroupNetwork groupNetwork = null;
        InstanceGroupNetwork instanceGroupNetwork = instanceGroup.getInstanceGroupNetwork();
        if (instanceGroupNetwork != null) {
            Json attributes = instanceGroupNetwork.getAttributes();
            Map<String, Object> params = attributes == null ? Collections.emptyMap() : attributes.getMap();
            Set<GroupSubnet> subnets = new HashSet<>();
            Set<GroupSubnet> endpointGatewaySubnets = new HashSet<>();
            Set<String> availabilityZones = new HashSet<>();
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
                List<String> zoneIds = (List<String>) params.getOrDefault(NetworkConstants.AVAILABILITY_ZONES,
                        new ArrayList<>());
                availabilityZones.addAll(zoneIds);
            }
            groupNetwork = new GroupNetwork(getOutboundInternetTraffic(stackNetwork), subnets, endpointGatewaySubnets, availabilityZones, params);
        }
        return groupNetwork;
    }

    public Network buildNetwork(com.sequenceiq.cloudbreak.domain.Network stackNetwork) {
        Network result = null;
        if (stackNetwork != null) {
            Subnet subnet = new Subnet(stackNetwork.getSubnetCIDR());
            Json attributes = stackNetwork.getAttributes();
            Map<String, Object> params = attributes == null ? Collections.emptyMap() : attributes.getMap();
            result = new Network(subnet, stackNetwork.getNetworkCidrs(), stackNetwork.getOutboundInternetTraffic(), params);
        }
        return result;
    }

    public CloudStack updateWithVerticalScaleRequest(CloudStack cloudStack, StackVerticalScaleV4Request request) {
        InstanceTemplateV4Request template = request.getTemplate();
        if (template != null) {
            cloudStack.getGroups()
                    .stream()
                    .filter(group -> group.getName().equals(request.getGroup()))
                    .findFirst().ifPresent(group -> {
                        group.getReferenceInstanceTemplate().setFlavor(template.getInstanceType());
                        group.getInstances().stream()
                                .filter(instance -> !Strings.isNullOrEmpty(template.getInstanceType()))
                                .forEach(instance -> instance.getTemplate().setFlavor(template.getInstanceType()));
                        if (template.getRootVolume() != null) {
                            group.setRootVolumeSize(template.getRootVolume().getSize());
                        }
                    });
        }
        return cloudStack;
    }

    private OutboundInternetTraffic getOutboundInternetTraffic(com.sequenceiq.cloudbreak.domain.Network stackNetwork) {
        return stackNetwork == null ? OutboundInternetTraffic.ENABLED : stackNetwork.getOutboundInternetTraffic();
    }

    private InstanceStatus getInstanceStatus(InstanceMetadataView metaData, Collection<String> deleteRequests) {
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

    public Map<String, Object> buildCloudInstanceParameters(DetailedEnvironmentResponse environment,
            StackView stackView, InstanceMetadataView instanceMetaData, CloudPlatform platform, Template template) {
        Map<String, Object> params = new HashMap<>();
        putIfPresent(params, CloudInstance.ID, getIfNotNull(instanceMetaData, InstanceMetadataView::getId));
        putIfPresent(params, CloudInstance.DISCOVERY_NAME, getIfNotNull(instanceMetaData, InstanceMetadataView::getShortHostname));
        putIfPresent(params, SUBNET_ID, getIfNotNull(instanceMetaData, InstanceMetadataView::getSubnetId));
        putIfPresent(params, CloudInstance.INSTANCE_NAME, getIfNotNull(instanceMetaData, InstanceMetadataView::getInstanceName));
        putIfPresent(params, CloudInstance.FQDN, getIfNotNull(instanceMetaData, InstanceMetadataView::getDiscoveryFQDN));
        doIfNotNull(getIfNotNull(instanceMetaData, InstanceMetadataView::getUserdataSecretResourceId), resourceId -> {
            Optional<Resource> userdataSecretResource = resourceService.findById(resourceId);
            if (userdataSecretResource.isPresent()) {
                params.put(CloudInstance.USERDATA_SECRET_ID, userdataSecretResource.get().getResourceReference());
            } else {
                throw new UserdataSecretsException(String.format("The secret resource with id '%s', associated with instance '%s'," +
                        " does not exist in the database!", resourceId, instanceMetaData.getInstanceId()));
            }
        });
        Optional<AzureResourceGroup> resourceGroupOptional = getAzureResourceGroup(environment, platform);
        if (environment != null && platform.name().equalsIgnoreCase(CloudPlatform.AZURE.name())) {
            params.put(AzureInstanceTemplate.RESOURCE_DISK_ATTACHED, getResourceDiskAttached(template.getAttributes()));
        }
        if (resourceGroupOptional.isPresent() && !ResourceGroupUsage.MULTIPLE.equals(resourceGroupOptional.get().getResourceGroupUsage())) {
            AzureResourceGroup resourceGroup = resourceGroupOptional.get();
            String resourceGroupName = resourceGroup.getName();
            ResourceGroupUsage resourceGroupUsage = resourceGroup.getResourceGroupUsage();
            params.put(RESOURCE_GROUP_NAME_PARAMETER, resourceGroupName);
            params.put(RESOURCE_GROUP_USAGE_PARAMETER, resourceGroupUsage.name());
        }
        if (CloudPlatform.YARN.equals(platform)) {
            resourceService.findByStackIdAndType(stackView.getId(), ResourceType.YARN_APPLICATION).stream()
                    .findFirst()
                    .ifPresent(yarnApplication -> params.put(CloudInstance.APPLICATION_NAME, yarnApplication.getResourceName()));
        }
        return params;
    }

    private boolean getResourceDiskAttached(Json attributes) {
        boolean resourceDiskAttached;
        if (attributes != null) {
            try {
                AzureInstanceTemplateV4Parameters parameters = attributes.get(AzureInstanceTemplateV4Parameters.class);
                resourceDiskAttached = parameters.getResourceDiskAttached();
            } catch (IOException e) {
                resourceDiskAttached = false;
            }
        } else {
            resourceDiskAttached = false;
        }
        return resourceDiskAttached;
    }

    private Map<String, String> buildCloudStackParameters(StackDtoDelegate stack, DetailedEnvironmentResponse environment) {
        Map<String, String> params = new HashMap<>(stack.getParameters());
        Optional<AzureResourceGroup> resourceGroupOptional = getAzureResourceGroup(environment, CloudPlatform.valueOf(stack.getCloudPlatform()));
        if (resourceGroupOptional.isPresent() && !ResourceGroupUsage.MULTIPLE.equals(resourceGroupOptional.get().getResourceGroupUsage())) {
            AzureResourceGroup resourceGroup = resourceGroupOptional.get();
            String resourceGroupName = resourceGroup.getName();
            ResourceGroupUsage resourceGroupUsage = resourceGroup.getResourceGroupUsage();
            params.put(RESOURCE_GROUP_NAME_PARAMETER, resourceGroupName);
            params.put(RESOURCE_GROUP_USAGE_PARAMETER, resourceGroupUsage.name());
        }
        Optional<Boolean> acceptancePolicy = getAzureMarketplaceTermsAcceptancePolicy(stack.getResourceCrn(), CloudPlatform.valueOf(stack.getCloudPlatform()));
        acceptancePolicy.ifPresent(acceptance -> params.put(ACCEPTANCE_POLICY_PARAMETER, acceptance.toString()));
        params.put(RESOURCE_CRN_PARAMETER, stack.getResourceCrn());
        Optional<String> gcpResourceEncryptionKey = getGcpResourceEncryptionKey(environment);
        gcpResourceEncryptionKey.ifPresent(key -> params.put(ENVIRONMENT_RESOURCE_ENCRYPTION_KEY, key));
        return params;
    }

    private Optional<Boolean> getAzureMarketplaceTermsAcceptancePolicy(String crn, CloudPlatform platform) {
        return CloudPlatform.AZURE.equals(platform) ? Optional.of(azureMarketplaceTermsClientService.getAccepted(crn)) : Optional.empty();
    }

    private Optional<AzureResourceGroup> getAzureResourceGroup(DetailedEnvironmentResponse environment, CloudPlatform platform) {
        return CloudPlatform.AZURE.equals(platform) ? getResourceGroupFromEnv(environment) : Optional.empty();
    }

    private Optional<AzureResourceGroup> getResourceGroupFromEnv(DetailedEnvironmentResponse environment) {
        return Optional.ofNullable(environment)
                .map(DetailedEnvironmentResponse::getAzure)
                .map(AzureEnvironmentParameters::getResourceGroup);
    }

    private Optional<String> getGcpResourceEncryptionKey(DetailedEnvironmentResponse environment) {
        return Optional.ofNullable(environment)
                .map(DetailedEnvironmentResponse::getGcp)
                .map(GcpEnvironmentParameters::getGcpResourceEncryptionParameters)
                .map(GcpResourceEncryptionParameters::getEncryptionKey);
    }
}