package com.sequenceiq.cloudbreak.cloud.aws.resource.instance;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util.SecurityGroupBuilderUtil.SECURITY_GROUP_ID;
import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.aws.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCloudStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudS3View;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsNativeInstanceResourceBuilder extends AbstractAwsNativeComputeBuilder {

    public static final String PLACEMENT_GROUP_NAME_PREFIX = "PlacementGroup";

    public static final int AWS_INSTANCE_RUNNING_CODE = 16;

    public static final int AWS_INSTANCE_TERMINATED_CODE = 48;

    private static final Logger LOGGER = getLogger(AwsNativeInstanceResourceBuilder.class);

    private static final int ORDER = 3;

    @Value("${cb.aws.vpcendpoints.enabled.gateway.services}")
    private Set<String> enabledGatewayServices;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private AwsMethodExecutor awsMethodExecutor;

    @Override
    public List<CloudResource> create(AwsContext context, long privateId, AuthenticatedContext auth, Group group, Image image) {
        CloudContext cloudContext = auth.getCloudContext();
        String resourceName = getResourceNameService().resourceName(resourceType(), cloudContext.getName(), group.getName(), privateId);
        return singletonList(CloudResource.builder()
                .group(group.getName())
                .type(resourceType())
                .status(CommonStatus.CREATED)
                .name(resourceName)
                .persistent(true)
                .build());
    }

    @Override
    public List<CloudResource> build(AwsContext context, long privateId, AuthenticatedContext ac, Group group, List<CloudResource> buildableResource,
            CloudStack cloudStack) throws Exception {
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
                amazonEc2Client.startInstances(new StartInstancesRequest().withInstanceIds(instance.getInstanceId()));
            }
        } else {
            TagSpecification tagSpecification = awsTaggingService.prepareEc2TagSpecification(awsCloudStackView.getTags(),
                    com.amazonaws.services.ec2.model.ResourceType.Instance);
            tagSpecification.withTags(new Tag().withKey("Name").withValue(cloudResource.getName()));
            RunInstancesRequest request = new RunInstancesRequest()
                    .withInstanceType(instanceTemplate.getFlavor())
                    .withImageId(cloudStack.getImage().getImageName())
                    .withSubnetId(awsCloudStackView.network().getExistingSubnet())
                    .withSecurityGroupIds(singletonList(context.getStringParameter(SECURITY_GROUP_ID)))
                    .withEbsOptimized(isEbsOptimized(instanceTemplate))
                    .withTagSpecifications(tagSpecification)
                    .withIamInstanceProfile(getIamInstanceProfile(group))
                    .withUserData(getUserData())
                    .withMinCount(1)
                    .withMaxCount(1)
                    .withKeyName(cloudStack.getInstanceAuthentication().getPublicKeyId());
            RunInstancesResult instanceResult = amazonEc2Client.createInstance(request);
            instance = instanceResult.getReservation().getInstances().get(0);
        }
        cloudResource.setInstanceId(instance.getInstanceId());
        return buildableResource;
    }

    private Optional<Instance> resourceByName(AmazonEc2Client amazonEc2Client, String name) {
        return awsMethodExecutor.execute(() -> {
            DescribeInstancesResult describeInstancesResult = amazonEc2Client.describeInstances(new DescribeInstancesRequest()
                    .withFilters(new Filter().withName("tag:Name").withValues(name)));
            return describeInstancesResult.getReservations().stream().flatMap(s -> s.getInstances().stream()).findFirst();
        }, Optional.empty());
    }

    @Override
    protected boolean isFinished(AwsContext context, AuthenticatedContext auth, CloudResource resource) {
        boolean creation =  context.isBuild();
        boolean finished;
        try {
            DescribeInstancesResult describeInstancesResult = context.getAmazonEc2Client().describeInstances(new DescribeInstancesRequest()
                    .withInstanceIds(resource.getInstanceId()));
            if (creation) {
                finished = describeInstancesResult.getReservations().stream()
                        .flatMap(s -> s.getInstances().stream())
                        .allMatch(this::instanceRunning);
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
                String operation = context.isBuild() ? "creation" : "termination";
                LOGGER.error("Cannot finished instance {}: {}", operation, e.getMessage(), e);
                throw e;
            }
        }
        return finished;
    }

    private boolean instanceRunning(Instance instance) {
        return instance.getState().getCode() == AWS_INSTANCE_RUNNING_CODE;
    }

    private boolean instanceTerminated(Instance instance) {
        return instance.getState().getCode() == AWS_INSTANCE_TERMINATED_CODE;
    }

    private String getUserData() {
        return "";
    }

    private IamInstanceProfileSpecification getIamInstanceProfile(Group group) {
        return new IamInstanceProfileSpecification().withArn(getInstanceProfile(group));
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        TerminateInstancesRequest request = new TerminateInstancesRequest()
                .withInstanceIds(resource.getInstanceId());
        context.getAmazonEc2Client().deleteInstance(request);
        return resource;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_INSTANCE;
    }

    @Override
    public int order() {
        return ORDER;
    }

    private Boolean isEbsOptimized(InstanceTemplate instanceTemplate) {
        Set<String> types = instanceTemplate.getVolumes().stream().map(Volume::getType).collect(Collectors.toSet());
        return types.contains(AwsPlatformParameters.AwsDiskType.St1.value());
    }

    private String getInstanceProfile(Group group) {
        return group.getIdentity().map(cloudFileSystemView -> {
            CloudS3View cloudS3View = CloudS3View.class.cast(cloudFileSystemView);
            return cloudS3View.getInstanceProfile();
        }).orElse(null);
    }
}
