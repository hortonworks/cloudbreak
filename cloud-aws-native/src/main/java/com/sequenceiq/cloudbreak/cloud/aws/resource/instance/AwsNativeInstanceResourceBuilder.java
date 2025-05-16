package com.sequenceiq.cloudbreak.cloud.aws.resource.instance;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.NOT_FOUND;
import static com.sequenceiq.cloudbreak.cloud.aws.resource.AwsNativeResourceBuilderOrderConstants.NATIVE_INSTANCE_RESOURCE_BUILDER_ORDER;
import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.USERDATA_SECRET_ID;
import static com.sequenceiq.cloudbreak.constant.ImdsConstants.AWS_IMDS_VERSION_V2;
import static com.sequenceiq.common.model.DefaultApplicationTag.RESOURCE_ID;
import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.VolumeBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsImdsUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsStackNameCommonUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util.SecurityGroupBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCloudStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.cloudbreak.cloud.template.init.SshKeyNameGenerator;
import com.sequenceiq.cloudbreak.cloud.util.UserdataSecretsUtil;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AwsDiskType;

import software.amazon.awssdk.services.ec2.model.AttributeValue;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.HttpTokensState;
import software.amazon.awssdk.services.ec2.model.IamInstanceProfileSpecification;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceMetadataOptionsRequest;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceMetadataOptionsRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.StartInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;

@Service
public class AwsNativeInstanceResourceBuilder extends AbstractAwsNativeComputeBuilder {

    public static final int AWS_INSTANCE_RUNNING_CODE = 16;

    public static final int AWS_INSTANCE_TERMINATED_CODE = 48;

    private static final Logger LOGGER = getLogger(AwsNativeInstanceResourceBuilder.class);

    @Value("${cb.aws.vpcendpoints.enabled.gateway.services}")
    private Set<String> enabledGatewayServices;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private AwsMethodExecutor awsMethodExecutor;

    @Inject
    private VolumeBuilderUtil volumeBuilderUtil;

    @Inject
    private AwsStackNameCommonUtil awsStackNameCommonUtil;

    @Inject
    private SecurityGroupBuilderUtil securityGroupBuilderUtil;

    @Inject
    private UserdataSecretsUtil userdataSecretsUtil;

    @Inject
    private SshKeyNameGenerator sshKeyNameGenerator;

    @Override
    public List<CloudResource> create(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        CloudContext cloudContext = auth.getCloudContext();
        String resourceName = getResourceNameService().nativeInstance(cloudContext.getName(), group.getName(), cloudContext.getId(), privateId);
        return singletonList(CloudResource.builder()
                .withGroup(group.getName())
                .withType(resourceType())
                .withStatus(CommonStatus.CREATED)
                .withAvailabilityZone(instance.getAvailabilityZone())
                .withName(resourceName)
                .withPersistent(true)
                .withReference(String.valueOf(privateId))
                .withPrivateId(privateId)
                .build());
    }

    @Override
    public List<CloudResource> build(AwsContext context, CloudInstance cloudInstance, long privateId, AuthenticatedContext ac,
            Group group, List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        if (buildableResource.isEmpty()) {
            throw new CloudConnectorException("Buildable resources cannot be empty!");
        }
        AmazonEc2Client amazonEc2Client = context.getAmazonEc2Client();
        InstanceTemplate instanceTemplate = group.getReferenceInstanceTemplate();
        AwsCloudStackView awsCloudStackView = new AwsCloudStackView(cloudStack);
        CloudResource cloudResource = buildableResource.get(0);
        String instanceName = awsStackNameCommonUtil.getInstanceName(ac, group.getName(), privateId);
        Optional<Instance> existedOpt = resourceByNameAndStackId(amazonEc2Client, instanceName, cloudStack);
        Instance instance;
        if (existedOpt.isPresent()) {
            instance = existedOpt.get();
            LOGGER.info("Instance exists with name: {} ({}), check the state: {}", cloudResource.getName(), instance.instanceId(),
                    instance.state().name());
            if (!instanceRunning(instance)) {
                LOGGER.info("Instance is existing but not running, try to start: {}, {}", instance.instanceId(), instance.state());
                amazonEc2Client.startInstances(StartInstancesRequest.builder().instanceIds(instance.instanceId()).build());
            }
        } else {
            LOGGER.info("Create new instance with name: {}", cloudResource.getName());
            Map<String, String> tags = new HashMap<>(awsCloudStackView.getTags());
            tags.putIfAbsent("Name", instanceName);
            tags.putIfAbsent("instanceGroup", group.getName());
            TagSpecification tagSpecificationOfInstance = awsTaggingService.prepareEc2TagSpecification(tags,
                    software.amazon.awssdk.services.ec2.model.ResourceType.INSTANCE);
            TagSpecification tagSpecificationOfVolume = awsTaggingService.prepareEc2TagSpecification(tags,
                    software.amazon.awssdk.services.ec2.model.ResourceType.VOLUME);
            RunInstancesRequest.Builder builder = RunInstancesRequest.builder()
                    .instanceType(instanceTemplate.getFlavor())
                    .imageId(cloudStack.getImage().getImageName())
                    .subnetId(cloudInstance.getSubnetId())
                    .securityGroupIds(securityGroupBuilderUtil.getSecurityGroupIds(context, group))
                    .ebsOptimized(isEbsOptimized(instanceTemplate))
                    .tagSpecifications(tagSpecificationOfInstance, tagSpecificationOfVolume)
                    .iamInstanceProfile(getIamInstanceProfile(group))
                    .userData(getUserData(cloudStack, group, cloudInstance))
                    .minCount(1)
                    .maxCount(1)
                    .blockDeviceMappings(blocks(group, cloudStack, ac))
                    .keyName(sshKeyNameGenerator.getKeyPairName(ac, cloudStack));
            if (StringUtils.equals(cloudStack.getSupportedImdsVersion(), AWS_IMDS_VERSION_V2)) {
                builder.metadataOptions(InstanceMetadataOptionsRequest.builder()
                        .httpTokens(HttpTokensState.REQUIRED)
                        .build());
            }
            RunInstancesRequest request = builder.build();
            RunInstancesResponse instanceResponse = amazonEc2Client.createInstance(request);
            instance = instanceResponse.instances().get(0);

            LOGGER.info("Instance creation initiated for resource: {} and instance id: {}", cloudResource, instance.instanceId());
        }
        cloudResource.setInstanceId(instance.instanceId());
        cloudInstance.getTemplate().putParameter(CloudResource.ARCHITECTURE, instance.architecture().name());
        return buildableResource;
    }

    @Override
    public CloudResource update(AwsContext context, CloudResource cloudResource, CloudInstance instance,
            AuthenticatedContext auth, CloudStack cloudStack, Optional<String> targetGroup, UpdateType updateType) throws Exception {
        AmazonEc2Client amazonEc2Client = context.getAmazonEc2Client();
        if (!isThisGroupApplicable(instance, targetGroup, updateType)) {
            LOGGER.info("The group is {} which is not same as the requested group {}.",
                    instance.getTemplate().getGroupName(), targetGroup.orElse("unknown"));
            return null;
        }
        Optional<Instance> existedOpt = resourceById(amazonEc2Client, instance.getInstanceId());
        Instance awsInstance;
        if (existedOpt.isPresent() && existedOpt.get().state().code() != AWS_INSTANCE_TERMINATED_CODE) {
            awsInstance = existedOpt.get();
            String requestedInstanceType = instance.getTemplate().getFlavor();
            LOGGER.info("Instance exists with name: {} ({}), check the state: {} and the instance type is: {}. " +
                            "The user requested {} type.",
                    awsInstance.instanceId(),
                    instance.getInstanceId(),
                    awsInstance.state().name(),
                    awsInstance.instanceType().toString(),
                    requestedInstanceType);
            if (isInstanceApplicable(awsInstance, requestedInstanceType)) {
                LOGGER.info("Modify group {}, from instance type {}, to instance type {}.",
                        targetGroup.get(),
                        awsInstance.instanceType().name(),
                        requestedInstanceType);
                ModifyInstanceAttributeRequest modifyInstanceAttributeRequest = ModifyInstanceAttributeRequest.builder()
                        .instanceId(awsInstance.instanceId())
                        .instanceType(AttributeValue.builder().value(requestedInstanceType).build())
                        .build();
                amazonEc2Client.modifyInstanceAttribute(modifyInstanceAttributeRequest);
            } else {
                LOGGER.info("Instance ID {} is using {} type which is the same type what was requested: {}",
                        awsInstance.instanceId(), awsInstance.instanceType().toString(), requestedInstanceType);
            }
            if (AwsImdsUtil.APPLICABLE_UPDATE_TYPES.contains(updateType)) {
                AwsImdsUtil.validateInstanceMetadataUpdate(updateType, cloudStack);
                HttpTokensState requestedHttpTokenState = AwsImdsUtil.getHttpTokensStateByUpdateType(updateType);
                HttpTokensState currentHttpTokenState = awsInstance.metadataOptions() != null && awsInstance.metadataOptions().httpTokens() != null ?
                        awsInstance.metadataOptions().httpTokens() : HttpTokensState.OPTIONAL;
                if (!requestedHttpTokenState.equals(currentHttpTokenState)) {
                    amazonEc2Client.modifyInstanceMetadataOptions(ModifyInstanceMetadataOptionsRequest.builder()
                            .httpTokens(requestedHttpTokenState)
                            .instanceId(awsInstance.instanceId())
                            .build());
                }
            }
        }
        return null;
    }

    private boolean isThisGroupApplicable(CloudInstance instance, Optional<String> targetGroup, UpdateType updateType) {
        return (targetGroup.isPresent() && targetGroup.get().equals(instance.getTemplate().getGroupName()))
                || AwsImdsUtil.APPLICABLE_UPDATE_TYPES.contains(updateType);
    }

    private boolean isInstanceApplicable(Instance awsInstance, String requestedInstanceType) {
        return !awsInstance.instanceType().toString().equals(requestedInstanceType);
    }

    Collection<BlockDeviceMapping> blocks(Group group, CloudStack cloudStack, AuthenticatedContext ac) {
        AwsInstanceView awsInstanceView = new AwsInstanceView(group.getReferenceInstanceTemplate());
        List<BlockDeviceMapping> blocks = new ArrayList<>();
        blocks.add(volumeBuilderUtil.getRootVolume(awsInstanceView, group, cloudStack, ac));
        List<BlockDeviceMapping> ephemeralBockDeviceMappings = volumeBuilderUtil.getEphemeral(awsInstanceView);
        if (CollectionUtils.isNotEmpty(ephemeralBockDeviceMappings)) {
            blocks.addAll(ephemeralBockDeviceMappings);
        }
        return blocks;
    }

    private Optional<Instance> resourceByNameAndStackId(AmazonEc2Client amazonEc2Client, String name, CloudStack cloudStack) {
        return awsMethodExecutor.execute(() -> {
            Set<Filter> filters = new HashSet<>();
            filters.add(tagFilter("Name", name));
            if (cloudStack.getTags().containsKey(RESOURCE_ID.key())) {
                filters.add(tagFilter(RESOURCE_ID.key(), cloudStack.getTags().get(RESOURCE_ID.key())));
            }
            DescribeInstancesResponse describeInstancesResponse = amazonEc2Client.describeInstances(DescribeInstancesRequest.builder()
                    .filters(filters)
                    .build());
            return describeInstancesResponse.reservations().stream()
                    .flatMap(s -> s.instances().stream())
                    .filter(instance -> instance.state().code() != AWS_INSTANCE_TERMINATED_CODE)
                    .findFirst();
        }, Optional.empty());
    }

    private Filter tagFilter(String key, String value) {
        return Filter.builder().name("tag:" + key).values(value).build();
    }

    private Optional<Instance> resourceById(AmazonEc2Client amazonEc2Client, String id) {
        return awsMethodExecutor.execute(() -> {
            DescribeInstancesResponse describeInstancesResponse = amazonEc2Client.describeInstances(DescribeInstancesRequest.builder().instanceIds(id).build());
            return describeInstancesResponse.reservations().stream().flatMap(s -> s.instances().stream()).findFirst();
        }, Optional.empty());
    }

    @Override
    protected CloudResourceStatus getResourceStatus(AwsContext context, AuthenticatedContext auth, CloudResource resource) {
        boolean creation = context.isBuild();
        String operation = creation ? "creation" : "termination";
        if (StringUtils.isNotEmpty(resource.getInstanceId())) {
            try {
                LOGGER.debug("Check instance {} for {}.", operation, resource.getInstanceId());
                DescribeInstancesResponse describeInstancesResponse = context.getAmazonEc2Client().describeInstances(DescribeInstancesRequest.builder()
                        .instanceIds(resource.getInstanceId())
                        .build());
                InstanceState instanceState = describeInstancesResponse.reservations().stream().flatMap(s -> s.instances().stream())
                        .map(instance -> instance.state()).findFirst().get();
                if (creation) {
                    return getResourceStatusForCreation(resource, instanceState);
                } else {
                    return getResourceStatusForDeletion(resource, instanceState);
                }
            } catch (Ec2Exception e) {
                if (e.awsErrorDetails().errorCode().contains(NOT_FOUND) && !creation) {
                    LOGGER.info("Aws resource does not found: {}", e.getMessage());
                    return new CloudResourceStatus(resource, ResourceStatus.DELETED, "AWS resource does not found");
                } else {
                    LOGGER.error("Cannot finished instance {}: {}", operation, e.getMessage(), e);
                    throw e;
                }
            }
        } else {
            LOGGER.warn("The resource with name: '{}' doesn't have instance identifier for operation: '{}'. There is no need to poll the state of the resource",
                    resource.getName(), operation);
            ResourceStatus resourceStatus = creation ? ResourceStatus.CREATED : ResourceStatus.DELETED;
            return new CloudResourceStatus(resource, resourceStatus);
        }
    }

    private CloudResourceStatus getResourceStatusForDeletion(CloudResource resource, InstanceState instanceState) {
        if (instanceState.code() == AWS_INSTANCE_TERMINATED_CODE) {
            LOGGER.debug("Instance {} termination finished", resource.getInstanceId());
            return new CloudResourceStatus(resource, ResourceStatus.DELETED);
        } else {
            return new CloudResourceStatus(resource, ResourceStatus.IN_PROGRESS);
        }
    }

    private CloudResourceStatus getResourceStatusForCreation(CloudResource resource, InstanceState instanceState) {
        if (instanceState.code() == AWS_INSTANCE_RUNNING_CODE) {
            LOGGER.debug("Instance {} creation finished", resource.getInstanceId());
            return new CloudResourceStatus(resource, ResourceStatus.CREATED);
        } else if (instanceState.code() == AWS_INSTANCE_TERMINATED_CODE) {
            String message = String.format("Instance %s creation failed, instance is in terminated state. " +
                    "It may have been terminated by an AWS policy or quota issue.", resource.getInstanceId());
            LOGGER.warn(message);
            return new CloudResourceStatus(resource, ResourceStatus.FAILED, message);
        } else {
            return new CloudResourceStatus(resource, ResourceStatus.IN_PROGRESS);
        }
    }

    private boolean instanceRunning(Instance instance) {
        LOGGER.debug("Check running state of {}. Current: {}", instance.instanceId(), instance.state());
        return instance.state().code() == AWS_INSTANCE_RUNNING_CODE;
    }

    private String getUserData(CloudStack cloudStack, Group group, CloudInstance cloudInstance) {
        String userdata = cloudStack.getUserDataByType(group.getType());
        if (cloudInstance.hasParameter(USERDATA_SECRET_ID)) {
            String secretArn = cloudInstance.getStringParameter(USERDATA_SECRET_ID);
            userdata = userdataSecretsUtil.replaceSecretsWithSecretId(userdata, secretArn);
        }
        String base64EncodedUserData = "";
        if (StringUtils.isNotEmpty(userdata)) {
            base64EncodedUserData = Base64Util.encode(userdata);
        }
        return base64EncodedUserData;
    }

    private IamInstanceProfileSpecification getIamInstanceProfile(Group group) {
        return IamInstanceProfileSpecification.builder().arn(getInstanceProfile(group)).build();
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        String instanceId = resource.getInstanceId();
        LOGGER.info("Terminate instance with instance id: {}", instanceId);
        if (StringUtils.isNotEmpty(instanceId)) {
            TerminateInstancesResponse terminateInstancesResponse = null;
            if (isInstanceExistsOnEc2(context, instanceId)) {
                TerminateInstancesRequest request = TerminateInstancesRequest.builder().instanceIds(instanceId).build();
                terminateInstancesResponse = awsMethodExecutor.execute(() -> context.getAmazonEc2Client().deleteInstance(request), null);
            } else {
                LOGGER.info("The instance could not be found with id:'{}' on EC2, no need for triggering termination", instanceId);
            }
            return terminateInstancesResponse == null ? null : resource;
        } else {
            return resource;
        }
    }

    private boolean isInstanceExistsOnEc2(AwsContext context, String instanceId) {
        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder().instanceIds(instanceId).build();
        DescribeInstancesResponse describeInstancesResponse = awsMethodExecutor.execute(() ->
                context.getAmazonEc2Client().describeInstances(describeInstancesRequest), DescribeInstancesResponse.builder().build());

        return describeInstancesResponse.reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .anyMatch(instance -> instanceId.equals(instance.instanceId()));
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_INSTANCE;
    }

    @Override
    public int order() {
        return NATIVE_INSTANCE_RESOURCE_BUILDER_ORDER;
    }

    private Boolean isEbsOptimized(InstanceTemplate instanceTemplate) {
        Set<String> types = instanceTemplate.getVolumes().stream().map(Volume::getType).collect(Collectors.toSet());
        return types.contains(AwsDiskType.St1.value());
    }

    private String getInstanceProfile(Group group) {
        return group.getIdentity().map(cloudFileSystemView -> {
            CloudS3View cloudS3View = (CloudS3View) cloudFileSystemView;
            return cloudS3View.getInstanceProfile();
        }).orElse(null);
    }
}
