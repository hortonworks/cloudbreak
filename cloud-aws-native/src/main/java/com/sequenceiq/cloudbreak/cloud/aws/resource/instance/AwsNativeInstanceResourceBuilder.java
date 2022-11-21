package com.sequenceiq.cloudbreak.cloud.aws.resource.instance;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.AwsNativeResourceBuilderOrderConstants.NATIVE_INSTANCE_RESOURCE_BUILDER_ORDER;
import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.VolumeBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
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

@Service
public class AwsNativeInstanceResourceBuilder extends AbstractAwsNativeComputeBuilder {

    public static final String PLACEMENT_GROUP_NAME_PREFIX = "PlacementGroup";

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

    @Override
    public List<CloudResource> create(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        CloudContext cloudContext = auth.getCloudContext();
        String resourceName = getResourceNameService().resourceName(resourceType(), cloudContext.getName(), group.getName(), cloudContext.getId(), privateId);
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
        if (existedOpt.isPresent() && existedOpt.get().getState().getCode() != AWS_INSTANCE_TERMINATED_CODE) {
            instance = existedOpt.get();
            LOGGER.info("Instance exists with name: {} ({}), check the state: {}", cloudResource.getName(), instance.getInstanceId(),
                    instance.getState().getName());
            if (!instanceRunning(instance)) {
                LOGGER.info("Instance is existing but not running, try to start: {}, {}", instance.getInstanceId(), instance.getState());
                amazonEc2Client.startInstances(new StartInstancesRequest().withInstanceIds(instance.getInstanceId()));
            }
        } else {
            LOGGER.info("Create new instance with name: {}", cloudResource.getName());
            TagSpecification tagSpecification = awsTaggingService.prepareEc2TagSpecification(awsCloudStackView.getTags(),
                    com.amazonaws.services.ec2.model.ResourceType.Instance);
            String securityGroupId = getSecurityGroupId(context, group);
            tagSpecification.withTags(
                    new Tag().withKey("Name").withValue(cloudResource.getName()),
                    new Tag().withKey("instanceGroup").withValue(group.getName())
            );
            RunInstancesRequest request = new RunInstancesRequest()
                    .withInstanceType(instanceTemplate.getFlavor())
                    .withImageId(cloudStack.getImage().getImageName())
                    .withSubnetId(cloudInstance.getSubnetId())
                    .withSecurityGroupIds(singletonList(securityGroupId))
                    .withEbsOptimized(isEbsOptimized(instanceTemplate))
                    .withTagSpecifications(tagSpecification)
                    .withIamInstanceProfile(getIamInstanceProfile(group))
                    .withUserData(getUserData(cloudStack, group))
                    .withMinCount(1)
                    .withMaxCount(1)
                    .withBlockDeviceMappings(blocks(group, cloudStack, ac))
                    .withKeyName(cloudStack.getInstanceAuthentication().getPublicKeyId());
            RunInstancesResult instanceResult = amazonEc2Client.createInstance(request);
            instance = instanceResult.getReservation().getInstances().get(0);
            LOGGER.info("Instance creation initiated for resource: {} and instance id: {}", cloudResource, instance.getInstanceId());
        }
        cloudResource.setInstanceId(instance.getInstanceId());
        return buildableResource;
    }

    @Override
    public CloudResource update(AwsContext context, CloudResource cloudResource, CloudInstance instance,
        AuthenticatedContext auth, CloudStack cloudStack) throws Exception {
        AmazonEc2Client amazonEc2Client = context.getAmazonEc2Client();
        Optional<Instance> existedOpt = resourceById(amazonEc2Client, instance.getInstanceId());
        Instance awsInstance;
        if (existedOpt.isPresent() && existedOpt.get().getState().getCode() != AWS_INSTANCE_TERMINATED_CODE) {
            awsInstance = existedOpt.get();
            LOGGER.info("Instance exists with name: {} ({}), check the state: {}", awsInstance.getInstanceId(), instance.getInstanceId(),
                    awsInstance.getState().getName());
            String requestedInstanceType = instance.getTemplate().getFlavor();
            if (!awsInstance.getInstanceType().equals(requestedInstanceType)) {
                ModifyInstanceAttributeRequest modifyInstanceAttributeRequest = new ModifyInstanceAttributeRequest();
                modifyInstanceAttributeRequest.setInstanceId(awsInstance.getInstanceId());
                modifyInstanceAttributeRequest.setInstanceType(instance.getTemplate().getFlavor());
                amazonEc2Client.modifyInstanceAttribute(modifyInstanceAttributeRequest);
            } else {
                LOGGER.info("Instance with name: {} ({}), using the same type what was requested: {}", awsInstance.getInstanceId(),
                        requestedInstanceType);
            }

        }
        return null;
    }

    String getSecurityGroupId(AwsContext context, Group group) {
        List<CloudResource> groupResources = context.getGroupResources(group.getName());
        String securityGroupId = null;
        if (groupResources != null) {
            securityGroupId = groupResources.stream()
                    .filter(g -> g.getType() == ResourceType.AWS_SECURITY_GROUP && group.getName().equals(g.getGroup()))
                    .findFirst()
                    .map(CloudResource::getReference)
                    .orElse(null);
            LOGGER.debug("Selected security group id from CloudResource: {}", securityGroupId);
        }
        if (StringUtils.isEmpty(securityGroupId)) {
            securityGroupId = group.getSecurity().getCloudSecurityId();
            LOGGER.debug("Selected security group id from group's security domain: {}", securityGroupId);
        }
        if (StringUtils.isEmpty(securityGroupId)) {
            LOGGER.debug("Cannot determine the security group, so it will be created in the default sec group");
        }
        LOGGER.info("Security group id: {}", securityGroupId);
        return securityGroupId;
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
            DescribeInstancesResult describeInstancesResult = amazonEc2Client.describeInstances(new DescribeInstancesRequest()
                    .withFilters(new Filter().withName("tag:Name").withValues(name)));
            return describeInstancesResult.getReservations().stream().flatMap(s -> s.getInstances().stream()).findFirst();
        }, Optional.empty());
    }

    private Optional<Instance> resourceById(AmazonEc2Client amazonEc2Client, String id) {
        return awsMethodExecutor.execute(() -> {
            DescribeInstancesResult describeInstancesResult = amazonEc2Client.describeInstances(new DescribeInstancesRequest()
                    .withInstanceIds(id));
            return describeInstancesResult.getReservations().stream().flatMap(s -> s.getInstances().stream()).findFirst();
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
                DescribeInstancesResult describeInstancesResult = context.getAmazonEc2Client().describeInstances(new DescribeInstancesRequest()
                        .withInstanceIds(resource.getInstanceId()));
                if (creation) {
                    finished = describeInstancesResult.getReservations().stream()
                            .flatMap(s -> s.getInstances().stream())
                            .allMatch(this::instanceRunningOrTerminated);
                } else {
                    finished = describeInstancesResult.getReservations().stream()
                            .flatMap(s -> s.getInstances().stream())
                            .allMatch(this::instanceTerminated);
                }
            } catch (AmazonEC2Exception e) {
                if (e.getErrorCode().contains("NotFound") && !creation) {
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
        LOGGER.debug("Check running state of {}. Current: {}", instance.getInstanceId(), instance.getState());
        return instance.getState().getCode() == AWS_INSTANCE_RUNNING_CODE;
    }

    private boolean instanceTerminated(Instance instance) {
        LOGGER.debug("check termination state of {}. Current: {}", instance.getInstanceId(), instance.getState());
        return instance.getState().getCode() == AWS_INSTANCE_TERMINATED_CODE;
    }

    private String getUserData(CloudStack cloudStack, Group group) {
        String userdata = cloudStack.getImage().getUserDataByType(group.getType());
        String base64EncodedUserData = "";
        if (StringUtils.isNotEmpty(userdata)) {
            base64EncodedUserData = Base64.getEncoder().encodeToString(userdata.getBytes());
        }
        return base64EncodedUserData;
    }

    private IamInstanceProfileSpecification getIamInstanceProfile(Group group) {
        return new IamInstanceProfileSpecification().withArn(getInstanceProfile(group));
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        LOGGER.info("Terminate instance with instance id: {}", resource.getInstanceId());
        if (resource.getInstanceId() != null) {
            TerminateInstancesRequest request = new TerminateInstancesRequest().withInstanceIds(resource.getInstanceId());
            TerminateInstancesResult terminateInstancesResult = awsMethodExecutor.execute(() -> context.getAmazonEc2Client().deleteInstance(request), null);
            return terminateInstancesResult == null ? null : resource;
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
            CloudS3View cloudS3View = CloudS3View.class.cast(cloudFileSystemView);
            return cloudS3View.getInstanceProfile();
        }).orElse(null);
    }
}
