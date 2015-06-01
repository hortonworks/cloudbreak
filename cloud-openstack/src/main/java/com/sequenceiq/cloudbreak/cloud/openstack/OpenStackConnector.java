package com.sequenceiq.cloudbreak.cloud.openstack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.heat.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformConnectorV2;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.domain.ResourceType;

@Service
public class OpenStackConnector implements CloudPlatformConnectorV2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackConnector.class);

    private static final int POLLING_INTERVAL = 10000;
    private static final int MAX_POLLING_ATTEMPTS = 1000;
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
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(stackContext, cloudCredential);
        OSClient client = openStackClient.createOSClient(cloudCredential);
        authenticatedContext.putParameters(OSClient.class, client);
        return authenticatedContext;
    }

    @Override
    public List<CloudResourceStatus> launchStack(AuthenticatedContext authenticatedContext, List<Group> groups, Network network, Security security, Image image) {
        OSClient client = authenticatedContext.getParameter(OSClient.class);

        String stackName = authenticatedContext.getStackContext().getStackName();

        String heatTemplate = heatTemplateBuilder.build(stackName, groups, network,
                security, image);

        Map<String, String> parameters = heatTemplateBuilder.buildParameters(authenticatedContext.getCloudCredential(), network, image);

        Stack openStackStack = client
                .heat()
                .stacks()
                .create(Builders.stack().name(stackName).template(heatTemplate).disableRollback(false)
                        .parameters(parameters).timeoutMins(OPERATION_TIMEOUT).build());

        List<CloudResourceStatus> resources = new ArrayList<>();
        CloudResource heatResource = new CloudResource(ResourceType.HEAT_STACK, stackName, openStackStack.getId());
        CloudResourceStatus heatResourceStatus = new CloudResourceStatus(heatResource, ResourceStatus.IN_PROGRESS);
        resources.add(heatResourceStatus);

        return resources;

    }

    @Override
    public List<CloudResourceStatus> checkResourcesState(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        List<CloudResourceStatus> result = new ArrayList<>();
        OSClient client = authenticatedContext.getParameter(OSClient.class);

        for (CloudResource resource : resources) {
            switch (resource.getType()) {
                case HEAT_STACK:
                    String heatStackId = resource.getReference();
                    String stackName = authenticatedContext.getStackContext().getStackName();
                    LOGGER.info("Checking OpenStack Heat stack status of: {}", stackName);
                    Stack heatStack = client.heat().stacks().getDetails(stackName, heatStackId);
                    String status = heatStack.getStatus();
                    LOGGER.info("Heat stack status of: {}  is: {}", heatStack, status);
                    CloudResourceStatus heatResourceStatus = new CloudResourceStatus(resource, ResourceStatus.IN_PROGRESS);
                    result.add(heatResourceStatus);
                    break;
                default:

            }
        }

        return result;
    }
}
