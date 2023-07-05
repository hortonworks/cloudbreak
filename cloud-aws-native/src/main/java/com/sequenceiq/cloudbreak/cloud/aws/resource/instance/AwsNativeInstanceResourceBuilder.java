package com.sequenceiq.cloudbreak.cloud.aws.resource.instance;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.AwsNativeResourceBuilderOrderConstants.NATIVE_INSTANCE_RESOURCE_BUILDER_ORDER;
import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.VolumeBuilderUtil;
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
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AwsDiskType;

import software.amazon.awssdk.services.ec2.model.AttributeValue;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.IamInstanceProfileSpecification;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeRequest;
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
        Optional<Instance> existedOpt = resourceByName(amazonEc2Client, cloudResource.getName());
        Instance instance;
        if (existedOpt.isPresent() && existedOpt.get().state().code() != AWS_INSTANCE_TERMINATED_CODE) {
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
            tags.putIfAbsent("Name", awsStackNameCommonUtil.getInstanceName(ac, group.getName(), privateId));
            tags.putIfAbsent("instanceGroup", group.getName());
            TagSpecification tagSpecification = awsTaggingService.prepareEc2TagSpecification(tags,
                    software.amazon.awssdk.services.ec2.model.ResourceType.INSTANCE);
            RunInstancesRequest request = RunInstancesRequest.builder()
                    .instanceType(instanceTemplate.getFlavor())
                    .imageId(cloudStack.getImage().getImageName())
                    .subnetId(cloudInstance.getSubnetId())
                    .securityGroupIds(securityGroupBuilderUtil.getSecurityGroupIds(context, group))
                    .ebsOptimized(isEbsOptimized(instanceTemplate))
                    .tagSpecifications(tagSpecification)
                    .iamInstanceProfile(getIamInstanceProfile(group))
                    .userData(getUserData(cloudStack, group))
                    .minCount(1)
                    .maxCount(1)
                    .blockDeviceMappings(blocks(group, cloudStack, ac))
                    .keyName(cloudStack.getInstanceAuthentication().getPublicKeyId())
                    .build();
            RunInstancesResponse instanceResponse = amazonEc2Client.createInstance(request);
            instance = instanceResponse.instances().get(0);

            LOGGER.info("Instance creation initiated for resource: {} and instance id: {}", cloudResource, instance.instanceId());
        }
        cloudResource.setInstanceId(instance.instanceId());
        return buildableResource;
    }

    @Override
    public CloudResource update(AwsContext context, CloudResource cloudResource, CloudInstance instance,
            AuthenticatedContext auth, CloudStack cloudStack, Optional<String> targetGroup) throws Exception {
        AmazonEc2Client amazonEc2Client = context.getAmazonEc2Client();
        Optional<Instance> existedOpt = resourceById(amazonEc2Client, instance.getInstanceId());
        Instance awsInstance;
        if (existedOpt.isPresent() && existedOpt.get().state().code() != AWS_INSTANCE_TERMINATED_CODE) {
            awsInstance = existedOpt.get();
            LOGGER.info("Instance exists with name: {} ({}), check the state: {}", awsInstance.instanceId(), instance.getInstanceId(),
                    awsInstance.state().name());
            String requestedInstanceType = instance.getTemplate().getFlavor();
            if (isThisGroupApplicable(instance, targetGroup, awsInstance, requestedInstanceType)) {
                LOGGER.info("Modify group {}, from instance type {}, to instance type {}.",
                        targetGroup.get(),
                        awsInstance.instanceType().name(),
                        instance.getTemplate().getFlavor());
                ModifyInstanceAttributeRequest modifyInstanceAttributeRequest = ModifyInstanceAttributeRequest.builder()
                        .instanceId(awsInstance.instanceId())
                        .instanceType(AttributeValue.builder().value(instance.getTemplate().getFlavor()).build())
                        .build();
                amazonEc2Client.modifyInstanceAttribute(modifyInstanceAttributeRequest);
            } else {
                LOGGER.info("Instance ID {} is using the same type what was requested: {}", awsInstance.instanceId(), requestedInstanceType);
            }

        }
        return null;
    }

    private boolean isThisGroupApplicable(CloudInstance instance, Optional<String> targetGroup, Instance awsInstance, String requestedInstanceType) {
        return !targetGroup.isEmpty() && targetGroup.equals(instance.getTemplate().getGroupName())
                && !awsInstance.instanceType().toString().equals(requestedInstanceType);
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

    private Optional<Instance> resourceByName(AmazonEc2Client amazonEc2Client, String name) {
        return awsMethodExecutor.execute(() -> {
            DescribeInstancesResponse describeInstancesResponse = amazonEc2Client.describeInstances(DescribeInstancesRequest.builder()
                    .filters(Filter.builder().name("tag:Name").values(name).build())
                    .build());
            return describeInstancesResponse.reservations().stream().flatMap(s -> s.instances().stream()).findFirst();
        }, Optional.empty());
    }

    private Optional<Instance> resourceById(AmazonEc2Client amazonEc2Client, String id) {
        return awsMethodExecutor.execute(() -> {
            DescribeInstancesResponse describeInstancesResponse = amazonEc2Client.describeInstances(DescribeInstancesRequest.builder().instanceIds(id).build());
            return describeInstancesResponse.reservations().stream().flatMap(s -> s.instances().stream()).findFirst();
        }, Optional.empty());
    }

    @Override
    protected boolean isFinished(AwsContext context, AuthenticatedContext auth, CloudResource resource) {
        boolean creation = context.isBuild();
        String operation = creation ? "creation" : "termination";
        boolean finished;
        if (StringUtils.isNotEmpty(resource.getInstanceId())) {
            try {
                LOGGER.debug("Check instance {} for {}.", operation, resource.getInstanceId());
                DescribeInstancesResponse describeInstancesResponse = context.getAmazonEc2Client().describeInstances(DescribeInstancesRequest.builder()
                        .instanceIds(resource.getInstanceId())
                        .build());
                if (creation) {
                    finished = describeInstancesResponse.reservations().stream()
                            .flatMap(s -> s.instances().stream())
                            .allMatch(this::instanceRunningOrTerminated);
                } else {
                    finished = describeInstancesResponse.reservations().stream()
                            .flatMap(s -> s.instances().stream())
                            .allMatch(this::instanceTerminated);
                }
            } catch (Ec2Exception e) {
                if (e.awsErrorDetails().errorCode().contains("NotFound") && !creation) {
                    LOGGER.info("Aws resource does not found: {}", e.getMessage());
                    finished = true;
                } else {
                    LOGGER.error("Cannot finished instance {}: {}", operation, e.getMessage(), e);
                    throw e;
                }
            }
        } else {
            LOGGER.warn("The resource with name: '{}' doesn't have instance identifier for operation: '{}'. There is no need to poll the state of the resource",
                    resource.getName(), operation);
            finished = true;
        }
        return finished;
    }

    private boolean instanceRunningOrTerminated(Instance instance) {
        return instanceRunning(instance) || instanceTerminated(instance);
    }

    private boolean instanceRunning(Instance instance) {
        LOGGER.debug("Check running state of {}. Current: {}", instance.instanceId(), instance.state());
        return instance.state().code() == AWS_INSTANCE_RUNNING_CODE;
    }

    private boolean instanceTerminated(Instance instance) {
        LOGGER.debug("check termination state of {}. Current: {}", instance.instanceId(), instance.state());
        return instance.state().code() == AWS_INSTANCE_TERMINATED_CODE;
    }

    private String getUserData(CloudStack cloudStack, Group group) {
        String userdata = cloudStack.getUserDataByType(group.getType());
        String base64EncodedUserData = "";
        if (StringUtils.isNotEmpty(userdata)) {
            base64EncodedUserData = Base64.getEncoder().encodeToString(userdata.getBytes());
        }
        return base64EncodedUserData;
    }

    private IamInstanceProfileSpecification getIamInstanceProfile(Group group) {
        return IamInstanceProfileSpecification.builder().arn(getInstanceProfile(group)).build();
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        LOGGER.info("Terminate instance with instance id: {}", resource.getInstanceId());
        if (resource.getInstanceId() != null) {
            TerminateInstancesRequest request = TerminateInstancesRequest.builder().instanceIds(resource.getInstanceId()).build();
            TerminateInstancesResponse terminateInstancesResponse = awsMethodExecutor.execute(() -> context.getAmazonEc2Client().deleteInstance(request), null);
            return terminateInstancesResponse == null ? null : resource;
        } else {
            return resource;
        }
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
