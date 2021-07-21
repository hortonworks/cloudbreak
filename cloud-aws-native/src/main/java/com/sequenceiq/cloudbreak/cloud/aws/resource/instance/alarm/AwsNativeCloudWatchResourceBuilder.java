package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.alarm;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.AwsNativeResourceBuilderOrderConstants.NATIVE_CLOUD_WATCH_RESOURCE_BUILDER_ORDER;
import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.resource.instance.AbstractAwsNativeComputeBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AwsNativeCloudWatchResourceBuilder extends AbstractAwsNativeComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNativeCloudWatchResourceBuilder.class);

    @Inject
    private AwsNativeCloudWatchService nativeCloudWatchService;

    @Inject
    private PersistenceRetriever persistenceRetriever;

    @Override
    public List<CloudResource> create(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        Optional<CloudResource> instanceResourceOpt = persistenceRetriever.notifyRetrieve(auth.getCloudContext().getId(), String.valueOf(privateId),
                CommonStatus.CREATED, ResourceType.AWS_INSTANCE);
        if (instanceResourceOpt.isEmpty()) {
            return emptyList();
        }
        String resourceName = getResourceNameService().resourceName(resourceType(), context.getName(), auth.getCloudContext().getId(), group.getName(),
                privateId);
        return singletonList(CloudResource.builder()
                .group(group.getName())
                .type(resourceType())
                .status(CREATED)
                .name(resourceName)
                .instanceId(instanceResourceOpt.get().getInstanceId())
                .persistent(true)
                .build());
    }

    @Override
    public List<CloudResource> build(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        String region = context.getLocation().getRegion().getRegionName();
        AwsCredentialView credential = new AwsCredentialView(auth.getCloudCredential());
        nativeCloudWatchService.addCloudWatchAlarmsForSystemFailures(buildableResource.get(0), region, credential);
        CloudResource resource = CloudResource.builder()
                .cloudResource(buildableResource.get(0))
                .build();
        return List.of(resource);
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        String region = context.getLocation().getRegion().getRegionName();
        AwsCredentialView credential = new AwsCredentialView(auth.getCloudCredential());
        String instanceId = resource.getInstanceId() == null ? resource.getReference() : resource.getInstanceId();
        boolean instanceHasAlarm = !nativeCloudWatchService.getMetricAlarmsForInstances(region, credential, Set.of(instanceId)).isEmpty();
        if (instanceHasAlarm) {
            LOGGER.info("About to remove CloudWatch alarm for instance: {}", instanceId);
            nativeCloudWatchService.deleteCloudWatchAlarmsForSystemFailures(region, credential, List.of(instanceId));
            return resource;
        }
        LOGGER.info("No alarm has found for instance: {}", instanceId);
        return null;
    }

    @Override
    protected boolean isFinished(AwsContext context, AuthenticatedContext auth, CloudResource resource) {
        return true;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_CLOUD_WATCH;
    }

    @Override
    public int order() {
        return NATIVE_CLOUD_WATCH_RESOURCE_BUILDER_ORDER;
    }

}
