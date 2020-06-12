package com.sequenceiq.freeipa.converter.cloud;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.CLOUDWATCH_CREATE_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_USAGE_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATE_REQUESTED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.DELETE_REQUESTED;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.REQUESTED;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
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
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.converter.image.ImageConverter;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackAuthentication;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;
import com.sequenceiq.freeipa.service.SecurityRuleService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.image.ImageService;

@Component
public class StackToCloudStackConverter implements Converter<Stack, CloudStack> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackToCloudStackConverter.class);

    @Inject
    private SecurityRuleService securityRuleService;

    @Inject
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Inject
    private ImageService imageService;

    @Inject
    private ImageConverter imageConverter;

    @Value("${freeipa.aws.cloudwatch.enabled:true}")
    private boolean enableCloudwatch;

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Override
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
        Image image = imageConverter.convert(imageService.getByStack(stack));
        Optional<CloudFileSystemView> fileSystemView = buildFileSystemView(stack);
        List<Group> instanceGroups = buildInstanceGroups(Lists.newArrayList(stack.getInstanceGroups()), stack.getStackAuthentication(),
                deleteRequestedInstances, stack.getCloudPlatform(), image.getImageName(), fileSystemView, stack.getEnvironmentCrn());
        Network network = buildNetwork(stack);
        InstanceAuthentication instanceAuthentication = buildInstanceAuthentication(stack.getStackAuthentication());
        Map<String, String> parameters = buildCloudStackParameters(stack.getEnvironmentCrn());

        return new CloudStack(instanceGroups, network, image, parameters,
                getUserDefinedTags(stack), stack.getTemplate(), instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), null);
    }

    private CloudInstance buildInstance(InstanceMetaData instanceMetaData, InstanceGroup instanceGroup,
            StackAuthentication stackAuthentication, Long privateId, InstanceStatus status, String imageName, String environmentCrn) {
        String id = instanceMetaData == null ? null : instanceMetaData.getInstanceId();
        String name = instanceGroup.getGroupName();
        Template template = instanceGroup.getTemplate();
        InstanceTemplate instanceTemplate = buildInstanceTemplate(template, name, privateId, status, imageName);
        InstanceAuthentication instanceAuthentication = buildInstanceAuthentication(stackAuthentication);
        Map<String, Object> parameters = buildCloudInstanceParameters(environmentCrn, instanceMetaData);

        return new CloudInstance(id, instanceTemplate, instanceAuthentication, parameters);
    }

    public List<CloudInstance> buildInstances(Stack stack) {
        ImageEntity imageEntity = imageService.getByStack(stack);
        Optional<CloudFileSystemView> fileSystemView = buildFileSystemView(stack);
        List<Group> groups = buildInstanceGroups(Lists.newArrayList(stack.getInstanceGroups()), stack.getStackAuthentication(), Collections.emptySet(),
                stack.getCloudPlatform(), imageEntity.getImageName(), fileSystemView, stack.getEnvironmentCrn());

        List<CloudInstance> cloudInstances = new ArrayList<>();
        for (Group group : groups) {
            cloudInstances.addAll(group.getInstances());
        }
        return cloudInstances;
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
        for (int i = 0; i < template.getVolumeCount(); i++) {
            //FIXME figure out volume mounting
            Volume volume = new Volume("/mnt/vol" + (i + 1), template.getVolumeType(), template.getVolumeSize());
            volumes.add(volume);
        }
        return new InstanceTemplate(template.getInstanceType(), name, privateId, volumes, status, fields, template.getId(), instanceImageId);
    }

    private Map<String, String> getUserDefinedTags(Stack stack) {
        Map<String, String> result = Maps.newHashMap();
        try {
            if (stack.getTags() != null && isNotBlank(stack.getTags().getValue())) {
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

    private List<Group> buildInstanceGroups(List<InstanceGroup> instanceGroups, StackAuthentication stackAuthentication, Collection<String> deleteRequests,
            String cloudPlatform, String imageName, Optional<CloudFileSystemView> fileSystemView, String environmentCrn) {
        // sort by name to avoid shuffling the different instance groups
        Collections.sort(instanceGroups);
        List<Group> groups = new ArrayList<>();
        for (InstanceGroup instanceGroup : instanceGroups) {
            if (instanceGroup.getTemplate() != null) {
                CloudInstance skeleton = null;
                List<CloudInstance> instances = new ArrayList<>();
                Template template = instanceGroup.getTemplate();
                // existing instances
                for (InstanceMetaData metaData : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
                    InstanceStatus status = getInstanceStatus(metaData, deleteRequests);
                    instances.add(buildInstance(metaData, instanceGroup, stackAuthentication, metaData.getPrivateId(), status,
                            imageName, environmentCrn));
                }
                if (instanceGroup.getNodeCount() == 0) {
                    skeleton = buildInstance(null, instanceGroup, stackAuthentication, 0L,
                            CREATE_REQUESTED, imageName, environmentCrn);
                }
                Json attributes = instanceGroup.getAttributes();
                InstanceAuthentication instanceAuthentication = buildInstanceAuthentication(stackAuthentication);
                Map<String, Object> fields = attributes == null ? Collections.emptyMap() : attributes.getMap();

                Integer rootVolumeSize = instanceGroup.getTemplate().getRootVolumeSize();
                if (Objects.isNull(rootVolumeSize)) {
                    rootVolumeSize = defaultRootVolumeSizeProvider.getForPlatform(cloudPlatform);
                }

                groups.add(
                        new Group(instanceGroup.getGroupName(),
                                InstanceGroupType.GATEWAY,
                                instances,
                                buildSecurity(instanceGroup),
                                skeleton,
                                fields,
                                instanceAuthentication,
                                instanceAuthentication.getLoginUserName(),
                                instanceAuthentication.getPublicKey(),
                                rootVolumeSize, fileSystemView)
                );
            }
        }
        return groups;
    }

    private InstanceAuthentication buildInstanceAuthentication(StackAuthentication stackAuthentication) {
        return new InstanceAuthentication(
                stackAuthentication.getPublicKey(),
                stackAuthentication.getPublicKeyId(),
                stackAuthentication.getLoginUserName());
    }

    private Security buildSecurity(InstanceGroup ig) {
        List<SecurityRule> rules = new ArrayList<>();
        if (ig.getSecurityGroup() == null) {
            return new Security(rules, Collections.emptyList(), true);
        }
        List<com.sequenceiq.freeipa.entity.SecurityRule> securityRules = securityRuleService.findAllBySecurityGroup(ig.getSecurityGroup());
        for (com.sequenceiq.freeipa.entity.SecurityRule securityRule : securityRules) {
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

    private Optional<CloudFileSystemView> buildFileSystemView(Stack stack) {
        Telemetry telemetry = stack.getTelemetry();
        if (telemetry != null && telemetry.getLogging() != null) {
            Logging logging = telemetry.getLogging();
            if (logging.getStorageLocation() != null) {
                if (logging.getS3() != null) {
                    CloudS3View s3View = new CloudS3View(CloudIdentityType.LOG);
                    s3View.setInstanceProfile(logging.getS3().getInstanceProfile());
                    return Optional.of(s3View);
                } else if (logging.getAdlsGen2() != null) {
                    CloudAdlsGen2View adlsGen2View = new CloudAdlsGen2View(CloudIdentityType.LOG);
                    AdlsGen2CloudStorageV1Parameters adlsGen2Params = logging.getAdlsGen2();
                    adlsGen2View.setAccountKey(adlsGen2Params.getAccountKey());
                    adlsGen2View.setAccountName(adlsGen2Params.getAccountName());
                    adlsGen2View.setSecure(adlsGen2Params.isSecure());
                    adlsGen2View.setManagedIdentity(adlsGen2Params.getManagedIdentity());
                    return Optional.of(adlsGen2View);
                } else if (logging.getCloudwatch() != null) {
                    CloudS3View s3View = new CloudS3View(CloudIdentityType.LOG);
                    s3View.setInstanceProfile(logging.getCloudwatch().getInstanceProfile());
                    return Optional.of(s3View);
                }
            }
        }
        return Optional.empty();
    }

    private Network buildNetwork(Stack stack) {
        com.sequenceiq.freeipa.entity.Network stackNetwork = stack.getNetwork();
        Network result = null;
        if (stackNetwork != null) {
            Subnet subnet = new Subnet(null);
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

    public Map<String, Object> buildCloudInstanceParameters(String environmentCrn, InstanceMetaData instanceMetaData) {
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
        Optional<AzureResourceGroup> resourceGroupOptional = getAzureResourceGroup(environmentCrn);
        if (resourceGroupOptional.isPresent() && !ResourceGroupUsage.MULTIPLE.equals(resourceGroupOptional.get().getResourceGroupUsage())) {
            AzureResourceGroup resourceGroup = resourceGroupOptional.get();
            String resourceGroupName = resourceGroup.getName();
            ResourceGroupUsage resourceGroupUsage = resourceGroup.getResourceGroupUsage();
            params.put(RESOURCE_GROUP_NAME_PARAMETER, resourceGroupName);
            params.put(RESOURCE_GROUP_USAGE_PARAMETER, resourceGroupUsage.name());
        }
        return params;
    }

    private Map<String, String> buildCloudStackParameters(String environmentCrn) {
        Map<String, String> params = new HashMap<>();
        params.put(CLOUDWATCH_CREATE_PARAMETER, Boolean.toString(enableCloudwatch));
        Optional<AzureResourceGroup> resourceGroupOptional = getAzureResourceGroup(environmentCrn);
        if (resourceGroupOptional.isPresent() && !ResourceGroupUsage.MULTIPLE.equals(resourceGroupOptional.get().getResourceGroupUsage())) {
            AzureResourceGroup resourceGroup = resourceGroupOptional.get();
            String resourceGroupName = resourceGroup.getName();
            ResourceGroupUsage resourceGroupUsage = resourceGroup.getResourceGroupUsage();
            params.put(RESOURCE_GROUP_NAME_PARAMETER, resourceGroupName);
            params.put(RESOURCE_GROUP_USAGE_PARAMETER, resourceGroupUsage.name());
        }
        return params;
    }

    private Optional<AzureResourceGroup> getAzureResourceGroup(String environmentCrn) {
        DetailedEnvironmentResponse environment = measure(() -> cachedEnvironmentClientService.getByCrn(environmentCrn),
                LOGGER, "Environment properties were queried under {} ms for environment {}", environmentCrn);
        return getResourceGroupFromEnv(environment);
    }

    private Optional<AzureResourceGroup> getResourceGroupFromEnv(DetailedEnvironmentResponse environment) {
        return Optional.ofNullable(environment)
                .map(DetailedEnvironmentResponse::getAzure)
                .map(AzureEnvironmentParameters::getResourceGroup);
    }
}
