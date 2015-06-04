package com.sequenceiq.cloudbreak.cloud.openstack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.heat.Stack;
import org.openstack4j.model.heat.StackUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformConnectorV2;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Instance;
import com.sequenceiq.cloudbreak.cloud.notification.ResourcePersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceAllocationPersisted;
import com.sequenceiq.cloudbreak.cloud.openstack.status.HeatStackStatus;
import com.sequenceiq.cloudbreak.domain.ResourceType;

import reactor.rx.Promise;

@Service("OpenStackConnectorV2")
public class OpenStackConnector implements CloudPlatformConnectorV2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackConnector.class);

    private static final long OPERATION_TIMEOUT = 60L;

    @Inject
    private OpenStackClient openStackClient;

    @Inject
    private HeatTemplateBuilder heatTemplateBuilder;

    @Override
    public String getCloudPlatform() {
        return OpenStackUtil.OPENSTACK;
    }

    @Override
    public AuthenticatedContext authenticate(StackContext stackContext, CloudCredential cloudCredential) {
        return openStackClient.createAuthenticatedContext(stackContext, cloudCredential);
    }


    @Override
    public List<CloudResourceStatus> launchStack(AuthenticatedContext authenticatedContext, CloudStack stack, ResourcePersistenceNotifier notifier) {
        String stackName = authenticatedContext.getStackContext().getStackName();
        String heatTemplate = heatTemplateBuilder.build(stackName, stack.getGroups(), stack.getNetwork(),
                stack.getSecurity(), stack.getImage());
        Map<String, String> parameters = heatTemplateBuilder.buildParameters(authenticatedContext.getCloudCredential(), stack.getNetwork(), stack.getImage());

        OSClient client = openStackClient.createOSClient(authenticatedContext);

        Stack heatStack = client
                .heat()
                .stacks()
                .create(Builders.stack().name(stackName).template(heatTemplate).disableRollback(false)
                        .parameters(parameters).timeoutMins(OPERATION_TIMEOUT).build());


        CloudResource cloudResource = new CloudResource(ResourceType.HEAT_STACK, stackName, heatStack.getId());
        Promise<ResourceAllocationPersisted> promise = notifier.notifyResourceAllocation(cloudResource);
        try {
            promise.await();
        } catch (Exception e) {
            //Rollback
            terminateStack(authenticatedContext, Arrays.asList(cloudResource));
        }

        List<CloudResourceStatus> resources = checkResourcesState(authenticatedContext, Arrays.asList(cloudResource));
        LOGGER.debug("Launched resources: {}", resources);
        return resources;
    }

    @Override
    public List<CloudResourceStatus> checkResourcesState(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        OSClient client = openStackClient.createOSClient(authenticatedContext);

        for (CloudResource resource : resources) {
            switch (resource.getType()) {
                case HEAT_STACK:
                    String heatStackId = resource.getReference();
                    String stackName = authenticatedContext.getStackContext().getStackName();
                    LOGGER.info("Checking OpenStack Heat stack status of: {}", stackName);
                    Stack heatStack = client.heat().stacks().getDetails(stackName, heatStackId);
                    CloudResourceStatus heatResourceStatus = heatStatus(resource, heatStack);
                    result.add(heatResourceStatus);
                    break;
                default:
                    throw new RuntimeException("Checking of invalid resource");
            }
        }

        return result;
    }

    @Override
    public List<CloudResourceStatus> terminateStack(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {

        for (CloudResource resource : resources) {
            switch (resource.getType()) {
                case HEAT_STACK:
                    String heatStackId = resource.getReference();
                    String stackName = authenticatedContext.getStackContext().getStackName();
                    LOGGER.info("Terminate stack: {}", stackName);
                    OSClient client = openStackClient.createOSClient(authenticatedContext);
                    client.heat().stacks().delete(stackName, heatStackId);
                    break;
                default:
                    throw new RuntimeException("Checking of invalid resource");
            }
        }

        return checkResourcesState(authenticatedContext, resources);
    }

    @Override
    public List<CloudVmInstanceStatus> collectVmMetadata(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        return null;
    }

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext ac, List<CloudResource> resources, List<Instance> vms) {
        return null;
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, List<CloudResource> resources, List<Instance> vms) {
        return null;
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources, int adjustment) {
        String stackName = authenticatedContext.getStackContext().getStackName();
        String heatTemplate = heatTemplateBuilder.build(stackName, stack.getGroups(), stack.getNetwork(),
                stack.getSecurity(), stack.getImage());
        Map<String, String> parameters = heatTemplateBuilder.buildParameters(authenticatedContext.getCloudCredential(), stack.getNetwork(), stack.getImage());
        return updateHeatStack(authenticatedContext, resources, heatTemplate, parameters);

    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources, List<Instance> vms) {
        String stackName = authenticatedContext.getStackContext().getStackName();
        String heatTemplate = heatTemplateBuilder.build(stackName, stack.getGroups(), stack.getNetwork(),
                stack.getSecurity(), stack.getImage());
        Map<String, String> parameters = heatTemplateBuilder.buildParameters(authenticatedContext.getCloudCredential(), stack.getNetwork(), stack.getImage());
        return updateHeatStack(authenticatedContext, resources, heatTemplate, parameters);
    }

    @Override
    public List<CloudResourceStatus> updateResources(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        String stackName = authenticatedContext.getStackContext().getStackName();
        String heatTemplate = heatTemplateBuilder.build(stackName, stack.getGroups(), stack.getNetwork(),
                stack.getSecurity(), stack.getImage());
        Map<String, String> parameters = heatTemplateBuilder.buildParameters(authenticatedContext.getCloudCredential(), stack.getNetwork(), stack.getImage());
        return updateHeatStack(authenticatedContext, resources, heatTemplate, parameters);
    }

    private List<CloudResourceStatus> updateHeatStack(AuthenticatedContext authenticatedContext, List<CloudResource> resources, String heatTemplate, Map<String,
            String> parameters) {


        // TODO Heat resources shall be fetched in a more elegant way
        CloudResource resource = resources.get(0);

        String stackName = resource.getName();
        String heatStackId = resource.getReference();

        OSClient client = openStackClient.createOSClient(authenticatedContext);

        StackUpdate updateRequest = Builders.stackUpdate().template(heatTemplate)
                .parameters(parameters).timeoutMins(OPERATION_TIMEOUT).build();

        client.heat().stacks().update(stackName, heatStackId, updateRequest);


        LOGGER.info("Heat stack update request sent with stack name: '{}' for Heat stack: '{}'", stackName, heatStackId);

        return checkResourcesState(authenticatedContext, resources);
    }


    private CloudResourceStatus heatStatus(CloudResource resource, Stack heatStack) {
        String status = heatStack.getStatus();
        LOGGER.info("Heat stack status of: {}  is: {}", heatStack, status);
        CloudResourceStatus heatResourceStatus = new CloudResourceStatus(resource, HeatStackStatus.mapResourceStatus(status), heatStack.getStackStatusReason());
        LOGGER.debug("Cloudresource status: {}", heatResourceStatus);
        return heatResourceStatus;
    }
}
