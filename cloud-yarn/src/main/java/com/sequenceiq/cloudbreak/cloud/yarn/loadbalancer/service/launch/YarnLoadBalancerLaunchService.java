package com.sequenceiq.cloudbreak.cloud.yarn.loadbalancer.service.launch;

import static com.sequenceiq.common.api.type.ResourceType.YARN_LOAD_BALANCER;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.yarn.ApplicationNameUtil;
import com.sequenceiq.cloudbreak.cloud.yarn.YarnApplicationCreationService;
import com.sequenceiq.cloudbreak.cloud.yarn.client.YarnClient;
import com.sequenceiq.cloudbreak.cloud.yarn.client.api.YarnResourceConstants;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.Container;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.core.YarnComponent;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.ApplicationDetailRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.request.CreateApplicationRequest;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ApplicationDetailResponse;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ApplicationErrorResponse;
import com.sequenceiq.cloudbreak.cloud.yarn.client.model.response.ResponseContext;
import com.sequenceiq.cloudbreak.cloud.yarn.loadbalancer.service.component.YarnLoadBalancerComponentFactoryService;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Service
public class YarnLoadBalancerLaunchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(YarnLoadBalancerLaunchService.class);

    private final ApplicationNameUtil applicationNameUtil;

    private final YarnApplicationCreationService yarnApplicationCreationService;

    private final YarnLoadBalancerComponentFactoryService componentFactory;

    YarnLoadBalancerLaunchService(ApplicationNameUtil applicationNameUtil, YarnApplicationCreationService yarnApplicationCreationService,
            YarnLoadBalancerComponentFactoryService componentFactory) {
        Objects.requireNonNull(applicationNameUtil);
        Objects.requireNonNull(yarnApplicationCreationService);
        Objects.requireNonNull(componentFactory);
        this.applicationNameUtil = applicationNameUtil;
        this.yarnApplicationCreationService = yarnApplicationCreationService;
        this.componentFactory = componentFactory;
    }

    public CloudResource launch(AuthenticatedContext authenticatedContext, CloudStack cloudStack, PersistenceNotifier persistenceNotifier, YarnClient yarnClient)
            throws Exception {
        String applicationName = applicationNameUtil.createLoadBalancerName(authenticatedContext);

        if (!yarnApplicationCreationService.checkApplicationAlreadyCreated(yarnClient, applicationName)) {
            LOGGER.debug("Creating the load balancer application for the Yarn datalake.");
            CreateApplicationRequest createApplicationRequest = createLoadBalancerRequest(cloudStack, applicationName, authenticatedContext, yarnClient);
            yarnApplicationCreationService.createApplication(yarnClient, createApplicationRequest);
            LOGGER.info("Successfully created the Yarn load balancer application.");
        }

        // Create an object for the new loadbalancer application and persist it in the resources table.
        CloudResource loadBalancerApplication = new Builder().type(YARN_LOAD_BALANCER).name(applicationName).build();
        LOGGER.debug("Persisting the new Yarn load balancer resource in the resources table.");
        persistenceNotifier.notifyAllocation(loadBalancerApplication, authenticatedContext.getCloudContext());
        return loadBalancerApplication;
    }

    /**
     * Creates a load balancer application request, specifically by:
     *      - Getting the IP addresses of the gateway nodes that are already running
     *      - Creating YarnComponent objects for each (if more than one) loadbalancer and pointing them to the gateway IPs.
     */
    private CreateApplicationRequest createLoadBalancerRequest(CloudStack cloudStack, String applicationName, AuthenticatedContext authenticatedContext,
            YarnClient yarnClient) {
        CreateApplicationRequest createApplicationRequest = yarnApplicationCreationService.initializeRequest(cloudStack, applicationName);
        List<String> gatewayIPs = getGatewayIPs(authenticatedContext, yarnClient, cloudStack);
        List<YarnComponent> loadBalancerComponents = componentFactory.create(cloudStack, gatewayIPs, applicationName);
        createApplicationRequest.setComponents(loadBalancerComponents);
        LOGGER.debug("Successfully created the Yarn laod balancer application request: " + createApplicationRequest);
        return createApplicationRequest;
    }

    /**
     * Uses the provided YarnClient to obtain information about the existing Yarn application. Takes advantage of the fact
     * that the applicationNameUtil has the exact same logic for creating the name of the original application.
     *
     * Returns the extracted IPs of only the gateway nodes in the Yarn application.
     */
    private List<String> getGatewayIPs(AuthenticatedContext authenticatedContext, YarnClient yarnClient, CloudStack cloudStack) {
        LOGGER.debug("Getting the IPs of the existing gateway Yarn containers for the loadbalancer.");
        String applicationName = applicationNameUtil.createApplicationName(authenticatedContext);
        Iterable<Container> foundContainers = getContainers(applicationName, yarnClient);
        Set<String> gatewayGroupNames = getGatewayGroupNames(cloudStack);

        List<String> gatewayIPs = Lists.newArrayList();
        foundContainers.forEach(container -> {
            if (gatewayGroupNames.contains(container.getComponentName())) {
                gatewayIPs.add(container.getIp() + ":443");
            }
        });
        LOGGER.info("Successfully found the following gateway IPs for the load balancer: " + gatewayIPs + ".");
        return gatewayIPs;
    }

    /**
     * Gets all of the containers currently running in the YCloud ecosystem within the application
     * given by the application name provided.
     */
    public Iterable<Container> getContainers(String applicationName, YarnClient yarnClient) {
        LOGGER.debug("Getting the Yarn containers for application " + applicationName + ".");
        ApplicationDetailRequest applicationDetailRequest = new ApplicationDetailRequest();
        applicationDetailRequest.setName(applicationName);
        ResponseContext responseContext;

        try {
            responseContext = yarnClient.getApplicationDetail(applicationDetailRequest);
            LOGGER.debug("Successfully for a response for Yarn application " + applicationName + " from the Yarn client.");
        } catch (MalformedURLException ex) {
            LOGGER.warn("Failed to get information for the Yarn loadbalancer! Application name: " + applicationName + " Error: " + ex.getMessage());
            throw new CloudConnectorException("Failed to get information for the Yarn loadbalancer.", ex);
        }

        if (responseContext.getStatusCode() == YarnResourceConstants.HTTP_SUCCESS) {
            LOGGER.info("Successfully retrieved container information for the Yarn application " + applicationName + ".");
            ApplicationDetailResponse applicationDetailResponse = (ApplicationDetailResponse) responseContext.getResponseObject();
            return applicationDetailResponse.getContainers();
        } else {
            LOGGER.warn("Failed to get yarn container! Application name: " + applicationName);
            ApplicationErrorResponse errorResponse = responseContext.getResponseError();
            throw new CloudConnectorException(String.format("Failed to get yarn container details: HTTP Return: %d Error: %s",
                    responseContext.getStatusCode(), errorResponse == null ? "unknown" : errorResponse.getDiagnostics()));
        }
    }

    /**
     * Gets the names of the instance groups that are Gateway types, the names can then be compared against
     * Yarn container names to check whether a container is a gateway container or not.
     */
    private Set<String> getGatewayGroupNames(CloudStack cloudStack) {
        return cloudStack.getGroups().stream().filter(group -> InstanceGroupType.isGateway(group.getType()))
                .map(Group::getName).collect(Collectors.toSet());
    }
}
