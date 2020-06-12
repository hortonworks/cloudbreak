package com.sequenceiq.cloudbreak.converter.spi;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.REQUESTED;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_USAGE_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATE_REQUESTED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.DELETE_REQUESTED;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
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
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.converter.InstanceMetadataToImageIdConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.securityrule.SecurityRuleService;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.cloudbreak.template.VolumeUtils;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class StackToCloudStackConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackToCloudStackConverter.class);

    @Inject
    private SecurityRuleService securityRuleService;

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

    public CloudStack convert(Stack stack) {
        return convert(stack, Collections.emptySet());
    }

    public CloudStack convertForDownscale(Stack stack, Set<String> deleteRequestedInstances) {
        return convert(stack, deleteRequestedInstances);
    }

    public CloudStack convertForTermination(Stack stack, String instanceId) {
        return convert(stack, Collections.singleton(instanceId));
    }

    public CloudStack convert(Stack stack, Collection<String> deleteRequestedInstances) {
        Image image = null;
        List<Group> instanceGroups = buildInstanceGroups(stack, stack.getInstanceGroupsAsList(), stack.getStackAuthentication(), deleteRequestedInstances);
        try {
            image = imageService.getImage(stack.getId());
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.debug(e.getMessage());
        }
        Network network = buildNetwork(stack);
        StackTemplate stackTemplate = componentConfigProviderService.getStackTemplate(stack.getId());
        InstanceAuthentication instanceAuthentication = buildInstanceAuthentication(stack.getStackAuthentication());
        SpiFileSystem cloudFileSystem = buildSpiFileSystem(stack);
        String template = null;
        if (stackTemplate != null) {
            template = stackTemplate.getTemplate();
        }
        Map<String, String> parameters = buildCloudStackParameters(stack);

        return new CloudStack(instanceGroups, network, image, parameters, getUserDefinedTags(stack), template,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), cloudFileSystem);
    }

    public List<CloudInstance> buildInstances(Stack stack) {
        List<Group> groups = buildInstanceGroups(stack, stack.getInstanceGroupsAsList(), stack.getStackAuthentication(), Collections.emptySet());
        List<CloudInstance> cloudInstances = new ArrayList<>();
        for (Group group : groups) {
            cloudInstances.addAll(group.getInstances());
        }
        return cloudInstances;
    }

    public CloudInstance buildInstance(InstanceMetaData instanceMetaData, InstanceGroup instanceGroup,
            StackAuthentication stackAuthentication, Long privateId, InstanceStatus status) {
        String id = instanceMetaData == null ? null : instanceMetaData.getInstanceId();
        String instanceImageId = instanceMetaData == null ? null : instanceMetadataToImageIdConverter.convert(instanceMetaData);
        String name = instanceGroup.getGroupName();
        Stack stack = instanceGroup.getStack();
        Template template = instanceGroup.getTemplate();

        InstanceTemplate instanceTemplate = buildInstanceTemplate(template, name, privateId, status, instanceImageId);
        InstanceAuthentication instanceAuthentication = buildInstanceAuthentication(stackAuthentication);

        Map<String, Object> parameters = buildCloudInstanceParameters(
                stack.getEnvironmentCrn(), instanceMetaData, CloudPlatform.valueOf(stack.getCloudPlatform()));
        return new CloudInstance(id, instanceTemplate, instanceAuthentication, parameters);
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
        template.getVolumeTemplates().stream().forEach(volumeModel -> {
            for (int i = 0; i < volumeModel.getVolumeCount(); i++) {
                Volume volume = new Volume(VolumeUtils.VOLUME_PREFIX + (i + 1), volumeModel.getVolumeType(), volumeModel.getVolumeSize());
                volumes.add(volume);
            }
        });
        return new InstanceTemplate(template.getInstanceType(), name, privateId, volumes, status, fields, template.getId(), instanceImageId);
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
            StackAuthentication stackAuthentication, Collection<String> deleteRequests) {
        // sort by name to avoid shuffling the different instance groups
        Collections.sort(instanceGroups);
        List<Group> groups = new ArrayList<>();
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            String blueprintText = cluster.getBlueprint() != null ? cluster.getBlueprint().getBlueprintText() : cluster.getExtendedBlueprintText();
            if (blueprintText != null) {
                CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintText);
                Map<String, Set<String>> componentsByHostGroup = cmTemplateProcessor.getComponentsByHostGroup();
                for (InstanceGroup instanceGroup : instanceGroups) {
                    if (instanceGroup.getTemplate() != null) {
                        InstanceAuthentication instanceAuthentication = buildInstanceAuthentication(stackAuthentication);
                        Optional<CloudFileSystemView> cloudFileSystemView
                                = cloudFileSystemViewProvider.getCloudFileSystemView(cluster.getFileSystem(), componentsByHostGroup, instanceGroup);
                        groups.add(
                                new Group(instanceGroup.getGroupName(),
                                        instanceGroup.getInstanceGroupType(),
                                        buildCloudInstances(stackAuthentication, deleteRequests, instanceGroup),
                                        buildSecurity(instanceGroup),
                                        buildCloudInstanceSkeleton(stackAuthentication, instanceGroup, stack),
                                        getFields(instanceGroup),
                                        instanceAuthentication,
                                        instanceAuthentication.getLoginUserName(),
                                        instanceAuthentication.getPublicKey(),
                                        getRootVolumeSize(instanceGroup),
                                        cloudFileSystemView)
                        );
                    }
                }
            }
        } else {
            LOGGER.warn("Cluster or blueprint is null for stack id:[{}] name:[{}]", stack.getId(), stack.getName());
        }
        return groups;
    }

    private InstanceAuthentication buildInstanceAuthentication(StackAuthentication stackAuthentication) {
        return new InstanceAuthentication(
                stackAuthentication.getPublicKey(),
                stackAuthentication.getPublicKeyId(),
                stackAuthentication.getLoginUserName());
    }

    private List<CloudInstance> buildCloudInstances(StackAuthentication stackAuthentication, Collection<String> deleteRequests,
            InstanceGroup instanceGroup) {
        List<CloudInstance> instances = new ArrayList<>();
        // existing instances
        for (InstanceMetaData metaData : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
            InstanceStatus status = getInstanceStatus(metaData, deleteRequests);
            instances.add(buildInstance(metaData, instanceGroup, stackAuthentication, metaData.getPrivateId(), status));
        }
        return instances;
    }

    private Security buildSecurity(InstanceGroup ig) {
        List<SecurityRule> rules = new ArrayList<>();
        if (ig.getSecurityGroup() == null) {
            return new Security(rules, Collections.emptyList());
        }
        Long id = ig.getSecurityGroup().getId();
        List<com.sequenceiq.cloudbreak.domain.SecurityRule> securityRules = securityRuleService.findAllBySecurityGroupId(id);
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
        return new Security(rules, ig.getSecurityGroup().getSecurityGroupIds(), true);
    }

    private CloudInstance buildCloudInstanceSkeleton(StackAuthentication stackAuthentication, InstanceGroup instanceGroup, Stack stack) {
        CloudInstance skeleton = null;
        if (instanceGroup.getNodeCount() == 0) {
            skeleton = buildInstance(null, instanceGroup, stackAuthentication, 0L,
                    CREATE_REQUESTED);
        }
        return skeleton;
    }

    private Map<String, Object> getFields(InstanceGroup instanceGroup) {
        Json attributes = instanceGroup.getAttributes();
        return attributes == null ? Collections.emptyMap() : attributes.getMap();
    }

    private Integer getRootVolumeSize(InstanceGroup instanceGroup) {
        Integer rootVolumeSize = instanceGroup.getTemplate().getRootVolumeSize();
        if (Objects.isNull(rootVolumeSize)) {
            rootVolumeSize = defaultRootVolumeSizeProvider.getForPlatform(instanceGroup.getTemplate().cloudPlatform());
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

    private Network buildNetwork(Stack stack) {
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

    public Map<String, Object> buildCloudInstanceParameters(String environmentCrn, InstanceMetaData instanceMetaData, CloudPlatform platform) {
        String hostName = instanceMetaData == null ? null : instanceMetaData.getShortHostname();
        String subnetId = instanceMetaData == null ? null : instanceMetaData.getSubnetId();
        String instanceName = instanceMetaData == null ? null : instanceMetaData.getInstanceName();
        Map<String, Object> params = new HashMap<>();
        if (hostName != null) {
            params.put(CloudInstance.DISCOVERY_NAME, hostName);
        }
        if (subnetId != null) {
            params.put(CloudInstance.SUBNET_ID, subnetId);
        }
        if (instanceName != null) {
            params.put(CloudInstance.INSTANCE_NAME, instanceName);
        }
        Optional<AzureResourceGroup> resourceGroupOptional = getAzureResourceGroup(environmentCrn, platform);
        if (resourceGroupOptional.isPresent() && !ResourceGroupUsage.MULTIPLE.equals(resourceGroupOptional.get().getResourceGroupUsage())) {
            AzureResourceGroup resourceGroup = resourceGroupOptional.get();
            String resourceGroupName = resourceGroup.getName();
            ResourceGroupUsage resourceGroupUsage = resourceGroup.getResourceGroupUsage();
            params.put(RESOURCE_GROUP_NAME_PARAMETER, resourceGroupName);
            params.put(RESOURCE_GROUP_USAGE_PARAMETER, resourceGroupUsage.name());
        }
        return params;
    }

    private Map<String, String> buildCloudStackParameters(Stack stack) {
        Map<String, String> params = new HashMap<>(stack.getParameters());
        Optional<AzureResourceGroup> resourceGroupOptional = getAzureResourceGroup(stack.getEnvironmentCrn(),
                CloudPlatform.valueOf(stack.getCloudPlatform()));
        if (resourceGroupOptional.isPresent() && !ResourceGroupUsage.MULTIPLE.equals(resourceGroupOptional.get().getResourceGroupUsage())) {
            AzureResourceGroup resourceGroup = resourceGroupOptional.get();
            String resourceGroupName = resourceGroup.getName();
            ResourceGroupUsage resourceGroupUsage = resourceGroup.getResourceGroupUsage();
            params.put(RESOURCE_GROUP_NAME_PARAMETER, resourceGroupName);
            params.put(RESOURCE_GROUP_USAGE_PARAMETER, resourceGroupUsage.name());
        }
        return params;
    }

    private Optional<AzureResourceGroup> getAzureResourceGroup(String environmentCrn, CloudPlatform platform) {
        if (Objects.nonNull(environmentCrn) && CloudPlatform.AZURE.equals(platform)) {
            DetailedEnvironmentResponse environment = measure(() -> environmentClientService.getByCrn(environmentCrn),
                    LOGGER, "Environment properties were queried under {} ms for environment {}", environmentCrn);
            return getResourceGroupFromEnv(environment);
        } else {
            return Optional.empty();
        }
    }

    private Optional<AzureResourceGroup> getResourceGroupFromEnv(DetailedEnvironmentResponse environment) {
        return Optional.ofNullable(environment)
                .map(DetailedEnvironmentResponse::getAzure)
                .map(AzureEnvironmentParameters::getResourceGroup);
    }

}
