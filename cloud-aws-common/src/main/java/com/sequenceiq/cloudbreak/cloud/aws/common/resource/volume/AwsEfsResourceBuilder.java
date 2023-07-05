package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEfsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudEfsAttributes;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.efs.model.CreateFileSystemRequest;
import software.amazon.awssdk.services.efs.model.CreateFileSystemResponse;
import software.amazon.awssdk.services.efs.model.DeleteFileSystemRequest;
import software.amazon.awssdk.services.efs.model.DeleteMountTargetRequest;
import software.amazon.awssdk.services.efs.model.DescribeFileSystemsRequest;
import software.amazon.awssdk.services.efs.model.DescribeFileSystemsResponse;
import software.amazon.awssdk.services.efs.model.DescribeMountTargetsRequest;
import software.amazon.awssdk.services.efs.model.DescribeMountTargetsResponse;
import software.amazon.awssdk.services.efs.model.EfsException;
import software.amazon.awssdk.services.efs.model.FileSystemDescription;
import software.amazon.awssdk.services.efs.model.LifeCycleState;
import software.amazon.awssdk.services.efs.model.MountTargetDescription;
import software.amazon.awssdk.services.efs.model.Tag;

@Component
public class AwsEfsResourceBuilder extends AbstractAwsComputeBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsEfsResourceBuilder.class);

    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private CommonAwsClient awsClient;

    @Inject
    private LifeCycleStateToResourceStatusConverter lifeCycleStateToResourceStatusConverter;

    @Override
    public List<CloudResource> create(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        LOGGER.debug("Create EFS resources");

        // TODO: We should create EFS using CloudFormation. And may create EFS related resources here.
        List<CloudResource> computeResources = context.getComputeResources(privateId);
        Optional<CloudResource> inputEfsSet = computeResources.stream()
                .filter(resource -> ResourceType.AWS_EFS.equals(resource.getType()))
                .findFirst();

        if (inputEfsSet.isPresent()) {
            return List.of(inputEfsSet.get());
        } else {
            return List.of();
        }
    }

    @Override
    public List<CloudResource> build(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        LOGGER.debug("Create EFS on provider" + buildableResource.stream().map(CloudResource::getName).collect(Collectors.toList()));
        AmazonEfsClient client = getAmazonEfsClient(auth);
        Map<String, CloudEfsAttributes> efsSetMap = Collections.synchronizedMap(new HashMap<>());

        List<CloudResource> requestedResources = buildableResource.stream()
                .filter(cloudResource -> CommonStatus.REQUESTED.equals(cloudResource.getStatus()))
                .collect(Collectors.toList());

        LOGGER.debug("Start creating EFS for stack: '{}' group: '{}'", auth.getCloudContext().getName(), group.getName());

        List<Future<?>> futures = requestedResources.stream()
                .map(requestedResource -> creatEfsRequest(requestedResource, cloudStack, efsSetMap))
                .map(request -> intermediateBuilderExecutor.submit(() -> {
                    CreateFileSystemResponse result = client.createFileSystem(request);
                    CloudEfsAttributes resultAttributes = getResponseEfsAttributes(request, result, efsSetMap);
                    efsSetMap.put(resultAttributes.getName(), resultAttributes);
                })).collect(Collectors.toList());

        LOGGER.debug("Waiting for EFS creation requests");
        for (Future<?> future : futures) {
            future.get();
        }
        LOGGER.debug("EFS creation requests sent");

        return buildableResource.stream()
                .peek(resource -> {
                    CloudEfsAttributes resultEfsAttributes = efsSetMap.getOrDefault(resource.getName(), null);
                    if (resultEfsAttributes != null) {
                        CloudEfsAttributes resourceEfsAttributes = resource.getParameter(CloudResource.ATTRIBUTES, CloudEfsAttributes.class);
                        resourceEfsAttributes.setFileSystemId(resultEfsAttributes.getFileSystemId());
                        resourceEfsAttributes.setFileState(resultEfsAttributes.getFileState());
                    }
                })
                .map(copyResourceWithCreatedStatus())
                .collect(Collectors.toList());
    }

    @Override
    public CloudResource update(AwsContext context, CloudResource cloudResource, CloudInstance instance,
        AuthenticatedContext auth, CloudStack cloudStack, Optional<String> targetGroupName) throws Exception {
        return null;
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws InterruptedException {
        //
        //https://docs.aws.amazon.com/efs/latest/ug/wt1-clean-up.html
        //To delete an EFS instance, need the following steps
        //1. Terminate the EC2 instances that mount on this EFS. The caller of this function has to make sure it is done before calling this function
        //2. Delete the mount targets of this EFS.
        //3. (Optional) Delete the security group of each mount target. AWS does not charge for security groups
        //4. (Optional) Delete the security group of the EC2 instances at step 1. The mount target's security group has a rule that references
        //   the EC2 security group. Therefore, we cannot first delete the EC2 instance's security group.
        //5. Actually delete the EFS
        //
        AmazonEfsClient client = getAmazonEfsClient(auth);
        CloudEfsAttributes efsAttributes = resource.getParameter(CloudResource.ATTRIBUTES, CloudEfsAttributes.class);
        String efsId = efsAttributes.getFileSystemId();

        DescribeFileSystemsRequest request = DescribeFileSystemsRequest.builder().fileSystemId(efsId).build();
        DescribeFileSystemsResponse result = client.describeFileSystems(request);
        List<FileSystemDescription> efsDescriptions = result.fileSystems();

        for (FileSystemDescription efsDescription : efsDescriptions) {
            LifeCycleState efsLifeCycleState = efsDescription.lifeCycleState();

            if (LifeCycleState.DELETED.equals(efsLifeCycleState) || LifeCycleState.DELETING.equals(efsLifeCycleState)) {
                LOGGER.debug("The given AWS EFS's [name: {}] lifecycle state was [{}] hence we are going to skip any delete operation over this resource",
                        efsDescription.name(), efsLifeCycleState);
                continue;
            }

            if (efsDescription.numberOfMountTargets() > 0) {
                DescribeMountTargetsRequest mtRequest = DescribeMountTargetsRequest.builder().fileSystemId(efsId).build();
                DescribeMountTargetsResponse mtResponse = client.describeMountTargets(mtRequest);
                List<MountTargetDescription> mountTargetDescriptionList = mtResponse.mountTargets();

                // Only delete the mount targets.
                for (MountTargetDescription mtDescription : mountTargetDescriptionList) {
                    DeleteMountTargetRequest mtDelRequest = DeleteMountTargetRequest.builder().mountTargetId(mtDescription.mountTargetId()).build();
                    LOGGER.debug("About to delete AWS EFS mount target that has the following id: {}", mtDescription.mountTargetId());
                    client.deleteMountTarget(mtDelRequest);
                }

                // TODO: delete the security groups in the future
            }

            DeleteFileSystemRequest efsDelRequest = DeleteFileSystemRequest.builder().fileSystemId(efsId).build();
            client.deleteFileSystem(efsDelRequest);
        }

        return null;
    }

    @Override
    protected List<CloudResourceStatus> checkResources(ResourceType type, AwsContext context, AuthenticatedContext auth, Iterable<CloudResource> resources) {
        AmazonEfsClient client = getAmazonEfsClient(auth);
        List<CloudResource> efsResources = StreamSupport.stream(resources.spliterator(), false)
                .filter(r -> r.getType().equals(resourceType()))
                .collect(Collectors.toList());

        List<String> efsIds = new ArrayList<>();
        List<CloudResourceStatus> cloudResourceStatusList = new ArrayList<>();

        for (CloudResource efsResource : efsResources) {
            CloudEfsAttributes efsAttributes = efsResource.getParameter(CloudResource.ATTRIBUTES, CloudEfsAttributes.class);
            String efsId = efsAttributes.getFileSystemId();
            efsIds.add(efsId);

            DescribeFileSystemsRequest request = DescribeFileSystemsRequest.builder().fileSystemId(efsId).build();
            DescribeFileSystemsResponse result = client.describeFileSystems(request);
            List<CloudResourceStatus> efsStatusList = getResourceStatus(efsResource, efsId, result);
            cloudResourceStatusList.addAll(efsStatusList);
        }

        LOGGER.debug("got EFS status for [{}]", String.join(",", efsIds));

        return cloudResourceStatusList;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_EFS;
    }

    @Override
    public int order() {
        return 1;
    }

    private AmazonEfsClient getAmazonEfsClient(AuthenticatedContext auth) {
        AwsCredentialView credentialView = new AwsCredentialView(auth.getCloudCredential());
        String regionName = auth.getCloudContext().getLocation().getRegion().value();
        return awsClient.createElasticFileSystemClient(credentialView, regionName);
    }

    private CreateFileSystemRequest creatEfsRequest(CloudResource resource, CloudStack cloudStack, Map<String, CloudEfsAttributes> efsSetMap) {

        CloudEfsAttributes efsAttributes = resource.getParameter(CloudResource.ATTRIBUTES, CloudEfsAttributes.class);
        efsSetMap.put(resource.getName(), efsAttributes);
        efsAttributes.setTags(cloudStack.getTags());
        Collection<Tag> awsTags = awsTaggingService.prepareEfsTags(efsAttributes.getTags());

        CreateFileSystemRequest.Builder createFileSystemRequestBuilder = CreateFileSystemRequest.builder()
                .creationToken(efsAttributes.getCreationToken())
                .tags(awsTags)
                .performanceMode(efsAttributes.getPerformanceMode())
                .throughputMode(efsAttributes.getThroughputMode())
                .provisionedThroughputInMibps(efsAttributes.getProvisionedThroughputInMibps())
                .encrypted(efsAttributes.getEncrypted());

        if (!StringUtils.isEmpty(efsAttributes.getKmsKeyId())) {
            createFileSystemRequestBuilder.kmsKeyId(efsAttributes.getKmsKeyId());
        }

        return createFileSystemRequestBuilder.build();
    }

    private Function<CloudResource, CloudResource> copyResourceWithCreatedStatus() {
        return resource -> CloudResource.builder()
                .withPersistent(true)
                .withGroup(resource.getGroup())
                .withType(resource.getType())
                .withStatus(CommonStatus.CREATED)
                .withName(resource.getName())
                .withParameters(resource.getParameters())
                .build();
    }

    private CloudEfsAttributes getResponseEfsAttributes(CreateFileSystemRequest request, CreateFileSystemResponse result, Map<String,
            CloudEfsAttributes> efsSetMap) {
        CloudEfsAttributes oriEfsAttributes = efsSetMap.getOrDefault(result.name(), null);
        CloudEfsAttributes resultEfsAttributes;

        if (oriEfsAttributes != null) {
            resultEfsAttributes = new CloudEfsAttributes(oriEfsAttributes);
        } else {
            Map<String, String> efsTags = awsTaggingService.convertAwsEfsTags(request.tags());
            resultEfsAttributes = new CloudEfsAttributes.Builder()
                    .withName(result.name())
                    .withCreationToken(request.creationToken())
                    .withTags(efsTags)
                    .withPerformanceMode(request.performanceModeAsString())
                    .withThroughputMode(request.throughputModeAsString())
                    .withProvisionedThroughputInMibps(request.provisionedThroughputInMibps())
                    .withEncrypted(request.encrypted())
                    .build();
        }

        resultEfsAttributes.setFileSystemId(result.fileSystemId());
        resultEfsAttributes.setFileState(result.lifeCycleState().name());

        return resultEfsAttributes;
    }

    private List<CloudResourceStatus> getResourceStatus(CloudResource efsResource, String efsId, DescribeFileSystemsResponse result) {
        try {
            return result.fileSystems().stream()
                    .peek(efsDescription -> LOGGER.debug("State of EFS {} is {}", efsDescription.fileSystemId(), efsDescription.lifeCycleState()))
                    .map(FileSystemDescription::lifeCycleState)
                    .map(lifeCycleStateToResourceStatusConverter::convert)
                    .map(efsStatus -> new CloudResourceStatus(efsResource, efsStatus))
                    .collect(Collectors.toList());
        } catch (EfsException e) {
            LOGGER.warn("Cannot get resource status. EFS {} throws exception {}", efsId, e);
            return Collections.emptyList();
        }
    }
}
