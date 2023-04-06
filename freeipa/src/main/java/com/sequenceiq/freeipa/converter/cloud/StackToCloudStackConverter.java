package com.sequenceiq.freeipa.converter.cloud;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.CLOUD_STACK_TYPE_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.FREEIPA_STACK_TYPE;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_NAME_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.RESOURCE_GROUP_USAGE_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.CREATE_REQUESTED;
import static com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.DELETE_REQUESTED;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.cloudbreak.util.NullUtil.putIfPresent;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.REQUESTED;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
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
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsGen2View;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.Logging;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.model.Backup;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.converter.image.ImageConverter;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
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

    @Inject
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Override
    public CloudStack convert(Stack stack) {
        return convert(stack, Collections.emptySet());
    }

    public CloudStack convert(Stack stack, Collection<String> deleteRequestedInstances) {
        Image image = imageConverter.convert(imageService.getByStack(stack));
        Optional<CloudFileSystemView> fileSystemView = buildFileSystemView(stack);
        List<Group> instanceGroups = buildInstanceGroups(
                stack,
                Lists.newArrayList(stack.getInstanceGroups()),
                deleteRequestedInstances,
                image.getImageName(),
                fileSystemView);
        Network network = buildNetwork(stack);
        InstanceAuthentication instanceAuthentication = buildInstanceAuthentication(stack.getStackAuthentication());
        Map<String, String> parameters = buildCloudStackParameters(stack.getEnvironmentCrn());

        return new CloudStack(instanceGroups, network, image, parameters,
                getUserDefinedTags(stack), stack.getTemplate(), instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), null,
                image.getUserdata().get(InstanceGroupType.GATEWAY), null);
    }

    public CloudInstance buildInstance(Stack stack, InstanceMetaData instanceMetaData, InstanceGroup instanceGroup,
            StackAuthentication stackAuthentication, Long privateId, InstanceStatus status) {
        ImageEntity imageEntity = imageService.getByStack(stack);
        return buildInstance(stack,
                instanceMetaData,
                instanceGroup,
                stackAuthentication,
                privateId,
                status,
                imageEntity.getImageName());
    }

    private CloudInstance buildInstance(Stack stack, InstanceMetaData instanceMetaData, InstanceGroup instanceGroup,
            StackAuthentication stackAuthentication, Long privateId, InstanceStatus status, String imageName) {
        LOGGER.debug("Instance metadata is {}", instanceMetaData);
        String id = instanceMetaData == null ? null : instanceMetaData.getInstanceId();
        String name = instanceGroup.getGroupName();
        Template template = instanceGroup.getTemplate();
        InstanceTemplate instanceTemplate = buildInstanceTemplate(template, name, privateId, status, imageName);
        InstanceAuthentication instanceAuthentication = buildInstanceAuthentication(stackAuthentication);
        Map<String, Object> parameters = buildCloudInstanceParameters(stack.getEnvironmentCrn(), instanceMetaData);

        return new CloudInstance(
                id,
                instanceTemplate,
                instanceAuthentication,
                instanceMetaData != null ? instanceMetaData.getSubnetId() : null,
                instanceMetaData != null ? instanceMetaData.getAvailabilityZone() : null,
                parameters);
    }

    public List<CloudInstance> buildInstances(Stack stack) {
        ImageEntity imageEntity = imageService.getByStack(stack);
        Optional<CloudFileSystemView> fileSystemView = buildFileSystemView(stack);
        List<Group> groups = buildInstanceGroups(
                stack,
                Lists.newArrayList(stack.getInstanceGroups()),
                Collections.emptySet(),
                imageEntity.getImageName(),
                fileSystemView);

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
            Volume volume = new Volume("/mnt/vol" + (i + 1), template.getVolumeType(), template.getVolumeSize(), CloudVolumeUsageType.GENERAL);
            volumes.add(volume);
        }
        return new InstanceTemplate(template.getInstanceType(), name, privateId, volumes, status, fields, template.getId(), instanceImageId,
                TemporaryStorage.ATTACHED_VOLUMES, 0L);
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

    private List<Group> buildInstanceGroups(Stack stack, List<InstanceGroup> instanceGroups, Collection<String> deleteRequests,
        String imageName, Optional<CloudFileSystemView> fileSystemView) {
        String cloudPlatform = stack.getCloudPlatform();
        // sort by name to avoid shuffling the different instance groups
        Collections.sort(instanceGroups);
        List<Group> groups = new ArrayList<>();
        InstanceAuthentication instanceAuthentication = buildInstanceAuthentication(stack.getStackAuthentication());
        for (InstanceGroup instanceGroup : instanceGroups) {
            if (instanceGroup.getTemplate() != null) {
                CloudInstance skeleton = null;
                List<CloudInstance> instances = new ArrayList<>();
                Template template = instanceGroup.getTemplate();
                // existing instances
                for (InstanceMetaData metaData : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
                    InstanceStatus status = getInstanceStatus(metaData, deleteRequests);
                    instances.add(buildInstance(stack, metaData, instanceGroup, stack.getStackAuthentication(), metaData.getPrivateId(), status,
                            imageName));
                }
                if (instanceGroup.getNodeCount() == 0) {
                    skeleton = buildInstance(stack, null, instanceGroup, stack.getStackAuthentication(), 0L,
                            CREATE_REQUESTED, imageName);
                }
                Json attributes = instanceGroup.getAttributes();
                Map<String, Object> fields = attributes == null ? Collections.emptyMap() : attributes.getMap();

                Integer rootVolumeSize = instanceGroup.getTemplate().getRootVolumeSize();
                if (Objects.isNull(rootVolumeSize)) {
                    rootVolumeSize = defaultRootVolumeSizeProvider.getForPlatform(cloudPlatform);
                }

                GroupNetwork groupNetwork = buildGroupNetwork(stack.getNetwork(), instanceGroup);

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
                                rootVolumeSize,
                                fileSystemView,
                                groupNetwork,
                                getUserDefinedTags(stack))
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

    private GroupNetwork buildGroupNetwork(com.sequenceiq.freeipa.entity.Network network, InstanceGroup instanceGroup) {
        GroupNetwork groupNetwork = null;
        InstanceGroupNetwork instanceGroupNetwork = instanceGroup.getInstanceGroupNetwork();
        if (instanceGroupNetwork != null) {
            Json attributes = instanceGroupNetwork.getAttributes();
            Map<String, Object> params = attributes == null ? Collections.emptyMap() : attributes.getMap();
            Set<GroupSubnet> subnets = new HashSet<>();
            if (params != null) {
                List<String> subnetIds = (List<String>) params.getOrDefault(NetworkConstants.SUBNET_IDS, new ArrayList<>());
                for (String subnetId : subnetIds) {
                    GroupSubnet groupSubnet = new GroupSubnet(subnetId);
                    subnets.add(groupSubnet);
                }
            }
            groupNetwork = new GroupNetwork(network.getOutboundInternetTraffic(), subnets, params);
        }
        return groupNetwork;
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

    public CloudStack updateWithVerticalScaleRequest(CloudStack cloudStack, VerticalScaleRequest request) {
        InstanceTemplateRequest template = request.getTemplate();
        if (template != null) {
            cloudStack.getGroups()
                .stream()
                .filter(group -> group.getName().equals(request.getGroup()))
                .flatMap(group -> group.getInstances().stream())
                .filter(instance -> !Strings.isNullOrEmpty(template.getInstanceType()))
                .forEach(instance -> instance.getTemplate().setFlavor(template.getInstanceType()));
        }
        return cloudStack;
    }

    public Optional<CloudFileSystemView> buildFileSystemView(Stack stack) {
        Telemetry telemetry = stack.getTelemetry();
        Backup backup = stack.getBackup();
        Optional<CloudFileSystemView> fileSystemView = Optional.empty();
        if (telemetry != null && telemetry.getLogging() != null && backup != null) {
            checkLoggingAndBackupFileSystemConflicting(telemetry.getLogging(), backup);
        }
        if (telemetry != null && telemetry.getLogging() != null) {
            fileSystemView = buildFileSystemViewFromTelemetry(telemetry.getLogging());
        }
        if (fileSystemView.isEmpty() && backup != null) {
            fileSystemView = buildFileSystemViewFromBackup(backup);
        }
        return fileSystemView;
    }

    private void checkLoggingAndBackupFileSystemConflicting(Logging logging, Backup backup) {
        if (logging.getS3() != null && backup.getS3() != null && !logging.getS3().equals(backup.getS3())) {
                throw new BadRequestException(
                        String.format("Logging [%s] and backup [%s] instance profiles are conflicting. " +
                        "Please use the same for both or define only one of them",
                        logging.getS3(), backup.getS3()));
        }
        if (logging.getAdlsGen2() != null && backup.getAdlsGen2() != null && !logging.getAdlsGen2().equals(backup.getAdlsGen2())) {
                throw new BadRequestException(
                        String.format("Logging [%s] and backup [%s] managed identities are conflicting. " +
                        "Please use the same for both or define only one of them",
                        logging.getAdlsGen2(), backup.getAdlsGen2()));
        }
        if (logging.getGcs() != null && backup.getGcs() != null && !logging.getGcs().equals(backup.getGcs())) {
                throw new BadRequestException(
                        String.format("Logging [%s] and backup [%s] service account emails are conflicting. " +
                        "Please use the same for both or define only one of them",
                        logging.getGcs(), backup.getGcs()));
        }
    }

    private Optional<CloudFileSystemView> buildFileSystemViewFromTelemetry(Logging logging) {
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
            } else if (logging.getGcs() != null) {
                CloudGcsView cloudGcsView = new CloudGcsView(CloudIdentityType.LOG);
                cloudGcsView.setServiceAccountEmail(logging.getGcs().getServiceAccountEmail());
                return Optional.of(cloudGcsView);
            } else if (logging.getCloudwatch() != null) {
                CloudS3View s3View = new CloudS3View(CloudIdentityType.LOG);
                s3View.setInstanceProfile(logging.getCloudwatch().getInstanceProfile());
                return Optional.of(s3View);
            }
        }
        return Optional.empty();
    }

    private Optional<CloudFileSystemView> buildFileSystemViewFromBackup(Backup backup) {
        if (backup.getStorageLocation() != null) {
            if (backup.getS3() != null) {
                CloudS3View s3View = new CloudS3View(CloudIdentityType.LOG);
                s3View.setInstanceProfile(backup.getS3().getInstanceProfile());
                return Optional.of(s3View);
            } else if (backup.getAdlsGen2() != null) {
                CloudAdlsGen2View adlsGen2View = new CloudAdlsGen2View(CloudIdentityType.LOG);
                AdlsGen2CloudStorageV1Parameters adlsGen2Params = backup.getAdlsGen2();
                adlsGen2View.setAccountKey(adlsGen2Params.getAccountKey());
                adlsGen2View.setAccountName(adlsGen2Params.getAccountName());
                adlsGen2View.setSecure(adlsGen2Params.isSecure());
                adlsGen2View.setManagedIdentity(adlsGen2Params.getManagedIdentity());
                return Optional.of(adlsGen2View);
            } else if (backup.getGcs() != null) {
                CloudGcsView cloudGcsView = new CloudGcsView(CloudIdentityType.LOG);
                cloudGcsView.setServiceAccountEmail(backup.getGcs().getServiceAccountEmail());
                return Optional.of(cloudGcsView);
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
        String hostName = instanceMetaData == null ? null : instanceMetaData.getDiscoveryFQDN();
        String subnetId = instanceMetaData == null ? null : instanceMetaData.getSubnetId();
        String instanceName = instanceMetaData == null ? null : instanceMetaData.getInstanceName();
        Map<String, Object> params = new HashMap<>();
        putIfPresent(params, CloudInstance.ID, getIfNotNull(instanceMetaData, InstanceMetaData::getId));
        if (hostName != null) {
            hostName = getFreeIpaHostname(instanceMetaData, hostName);
            LOGGER.debug("Setting FreeIPA hostname to {}", hostName);
            params.put(CloudInstance.DISCOVERY_NAME, hostName);
        }
        if (subnetId != null) {
            params.put(SUBNET_ID, subnetId);
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

    private String getFreeIpaHostname(InstanceMetaData instanceMetaData, String hostName) {
        if (hostName.contains("%d.")) {
            hostName = String.format(hostName, instanceMetaData.getPrivateId());
        }
        return hostName;
    }

    private Map<String, String> buildCloudStackParameters(String environmentCrn) {
        Map<String, String> params = new HashMap<>();
        params.put(CLOUD_STACK_TYPE_PARAMETER, FREEIPA_STACK_TYPE);
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
