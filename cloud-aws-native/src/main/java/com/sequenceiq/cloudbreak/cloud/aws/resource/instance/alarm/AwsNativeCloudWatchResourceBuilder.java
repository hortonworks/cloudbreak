package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.alarm;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.AwsNativeResourceBuilderOrderConstants.NATIVE_CLOUD_WATCH_RESOURCE_BUILDER_ORDER;
import static com.sequenceiq.common.api.type.CommonStatus.CREATED;
import static java.lang.String.valueOf;
import static java.util.Collections.singletonList;

import java.util.List;
import java.util.Set;

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
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AwsNativeCloudWatchResourceBuilder extends AbstractAwsNativeComputeBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsNativeCloudWatchResourceBuilder.class);

    private AwsNativeCloudWatchService nativeCloudWatchService;

    public AwsNativeCloudWatchResourceBuilder(AwsNativeCloudWatchService nativeCloudWatchService) {
        this.nativeCloudWatchService = nativeCloudWatchService;
    }

    @Override
    public List<CloudResource> create(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group, Image image) {
        String resourceName = getResourceNameService().resourceName(resourceType(), context.getName(), group.getName(), privateId);
        return singletonList(CloudResource.builder()
                .group(group.getName())
                .type(resourceType())
                .status(CREATED)
                .name(resourceName)
                .persistent(true)
                .reference(valueOf(privateId))
                .build());
    }

    @Override
    public List<CloudResource> build(AwsContext context, CloudInstance instance, long privateId, AuthenticatedContext auth, Group group,
            List<CloudResource> buildableResource, CloudStack cloudStack) throws Exception {
        String region = context.getLocation().getRegion().getRegionName();
        AwsCredentialView credential = new AwsCredentialView(auth.getCloudCredential());
        nativeCloudWatchService.addCloudWatchAlarmsForSystemFailures(buildableResource, region, credential);
        CloudResource resource = CloudResource.builder()
                .cloudResource(buildableResource.get(0))
                .reference(valueOf(privateId))
                .build();
        return List.of(resource);
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        String region = context.getLocation().getRegion().getRegionName();
        AwsCredentialView credential = new AwsCredentialView(auth.getCloudCredential());
        boolean instanceHasAlarm = !nativeCloudWatchService.getMetricAlarmsForInstances(region, credential, Set.of(resource.getInstanceId())).isEmpty();
        if (instanceHasAlarm) {
            LOGGER.info("About to remove CloudWatch alarm for instance: {}", resource.getInstanceId());
            nativeCloudWatchService.deleteCloudWatchAlarmsForSystemFailures(region, credential, List.of(resource.getInstanceId()));
            return resource;
        }
        LOGGER.info("No alarm has found for instance: {}", resource.getInstanceId());
        return null;
    }

    @Override
    protected boolean isFinished(AwsContext context, AuthenticatedContext auth, CloudResource resource) {
        return true;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_INSTANCE;
    }

    @Override
    public int order() {
        return NATIVE_CLOUD_WATCH_RESOURCE_BUILDER_ORDER;
    }

}
