package com.sequenceiq.cloudbreak.cloud.yarn;

import static com.sequenceiq.cloudbreak.common.type.ResourceType.YARN_APPLICATION;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Splitter;
import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingDoesNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.yarn.auth.YarnClientUtil;
import com.sequenceiq.cloudbreak.cloud.yarn.status.YarnApplicationStatus;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.yarn.api.YarnResourceConstants;
import com.sequenceiq.cloudbreak.orchestrator.yarn.client.YarnClient;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Artifact;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Resource;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.YarnComponent;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.ApplicationDetailRequest;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.CreateApplicationRequest;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.DeleteApplicationRequest;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.ApplicationDetailResponse;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.ApplicationErrorResponse;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.ResponseContext;

@Service
public class YarnResourceConnector implements ResourceConnector<Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger(YarnResourceConnector.class);

    @Inject
    private YarnClientUtil yarnClientUtil;

    private int maxResourceNameLength;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext authenticatedContext, CloudStack stack, PersistenceNotifier persistenceNotifier,
            AdjustmentType adjustmentType, Long threshold) throws Exception {
        CreateApplicationRequest createApplicationRequest = new CreateApplicationRequest();
        createApplicationRequest.setName(createApplicationName(authenticatedContext));
        createApplicationRequest.setQueue(stack.getParameters().get(YarnConstants.YARN_QUEUE_PARAMETER));
        createApplicationRequest.setLifetime(YarnResourceConstants.UNLIMITED);

        Artifact artifact = new Artifact();
        artifact.setId(stack.getImage().getImageName());
        artifact.setType("DOCKER");

        List<YarnComponent> components = new ArrayList<>();
        for (Group group : stack.getGroups()) {
            YarnComponent component = new YarnComponent();
            component.setName(group.getName());
            component.setNumberOfContainers(group.getInstancesSize());
            // TODO launch command
            component.setLaunchCommand("");
            component.setArtifact(artifact);
            component.setDependencies(new ArrayList<>());
            InstanceTemplate instanceTemplate = group.getReferenceInstanceConfiguration().getTemplate();
            Resource resource = new Resource();
            resource.setCpus(instanceTemplate.getParameter(YarnConstants.YARN_CPUS_PARAMETER, Integer.class));
            resource.setMemory(instanceTemplate.getParameter(YarnConstants.YARN_MEMORY_PARAMETER, Integer.class));
            component.setResource(resource);
            component.setRunPrivilegedContainer(true);
            components.add(component);
        }
        CloudResource yarnApplication = new CloudResource.Builder().type(YARN_APPLICATION).name(createApplicationRequest.getName()).build();
        persistenceNotifier.notifyAllocation(yarnApplication, authenticatedContext.getCloudContext());

        YarnClient yarnClient = yarnClientUtil.createYarnClient(authenticatedContext);
        ResponseContext responseContext = yarnClient.createApplication(createApplicationRequest);
        if (responseContext.getResponseError() != null) {
            ApplicationErrorResponse applicationErrorResponse = responseContext.getResponseError();
            throw new CloudConnectorException(String.format("Yarn Application creation error: HTTP Return: %d Error: %s", responseContext.getStatusCode(),
                    applicationErrorResponse.getDiagnostics()));
        }

        return check(authenticatedContext, Collections.singletonList(yarnApplication));
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        YarnClient yarnClient = yarnClientUtil.createYarnClient(authenticatedContext);
        List<CloudResourceStatus> result = new ArrayList<>();
        for (CloudResource resource : resources) {
            switch (resource.getType()) {
                case YARN_APPLICATION:
                    LOGGER.info("Checking Yarn application status of: {}", resource.getName());
                    try {
                        ApplicationDetailRequest applicationDetailRequest = new ApplicationDetailRequest();
                        applicationDetailRequest.setName(resource.getName());
                        ResponseContext responseContext = yarnClient.getApplicationDetail(applicationDetailRequest);
                        if (responseContext.getStatusCode() == YarnResourceConstants.HTTP_SUCCESS) {
                            ApplicationDetailResponse applicationDetailResponse = (ApplicationDetailResponse) responseContext.getResponseObject();
                            result.add(new CloudResourceStatus(resource, YarnApplicationStatus.mapResourceStatus(applicationDetailResponse.getState())));
                        } else if (responseContext.getResponseError() != null) {
                            throw new CloudConnectorException(String.format("Yarn Application status check failed: HttpStatusCode: %d, Error: %s",
                                    responseContext.getStatusCode(), responseContext.getResponseError().getDiagnostics()));
                        } else {
                            throw new CloudConnectorException(String.format("Yarn Application status check failed: Invalid HttpStatusCode: %d",
                                    responseContext.getStatusCode()));
                        }
                    } catch (MalformedURLException | RuntimeException e) {
                        throw new CloudConnectorException(String.format("Invalid resource exception: %s", e.getMessage()), e);
                    }
                    break;
                default:
                    throw new CloudConnectorException(String.format("Invalid resource type: %s", resource.getType()));
            }
        }
        return result;
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> cloudResources)
            throws Exception {
        for (CloudResource resource : cloudResources) {
            switch (resource.getType()) {
                case YARN_APPLICATION:
                    YarnClient yarnClient = yarnClientUtil.createYarnClient(authenticatedContext);
                    String yarnApplicationName = resource.getName();
                    String stackName = authenticatedContext.getCloudContext().getName();
                    LOGGER.info("Terminate stack: {}", stackName);
                    try {
                        DeleteApplicationRequest deleteApplicationRequest = new DeleteApplicationRequest();
                        deleteApplicationRequest.setName(yarnApplicationName);
                        yarnClient.deleteApplication(deleteApplicationRequest);
                        LOGGER.info("Yarn Applicatin has been deleted");
                    } catch (MalformedURLException | CloudbreakOrchestratorFailedException e) {
                        throw new CloudConnectorException("Stack cannot be deleted", e);
                    }
                    break;
                default:
                    throw new CloudConnectorException(String.format("Invalid resource type: %s", resource.getType()));
            }
        }
        return check(authenticatedContext, cloudResources);
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) throws Exception {
        return null;
    }

    @Override
    public List<CloudResourceStatus> upscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        return null;
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources,
            List<CloudInstance> vms, Object resourcesToRemove) {
        return null;
    }

    @Override
    public Object collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms) {
        return null;
    }

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        // TODO private ip or public ip is used for tls
        return new TlsInfo(false);
    }

    @Override
    public String getStackTemplate() throws TemplatingDoesNotSupportedException {
        throw new TemplatingDoesNotSupportedException();
    }

    private String createApplicationName(AuthenticatedContext ac) {
        return String.format("%s-%s", Splitter.fixedLength(maxResourceNameLength - (ac.getCloudContext().getId().toString().length() + 1))
                .splitToList(ac.getCloudContext().getName()).get(0), ac.getCloudContext().getId());
    }
}
