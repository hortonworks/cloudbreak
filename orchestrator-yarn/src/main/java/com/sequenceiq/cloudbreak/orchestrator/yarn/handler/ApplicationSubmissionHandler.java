package com.sequenceiq.cloudbreak.orchestrator.yarn.handler;

import static com.sequenceiq.cloudbreak.orchestrator.yarn.api.YarnResourceConstants.NON_AGENT_CPUS;
import static com.sequenceiq.cloudbreak.orchestrator.yarn.api.YarnResourceConstants.NON_AGENT_MEMORY;
import static com.sequenceiq.cloudbreak.orchestrator.yarn.api.YarnResourceConstants.UNLIMITED;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.yarn.api.ComponentType;
import com.sequenceiq.cloudbreak.orchestrator.yarn.api.Entrypoint;
import com.sequenceiq.cloudbreak.orchestrator.yarn.client.YarnClient;
import com.sequenceiq.cloudbreak.orchestrator.yarn.client.YarnHttpClient;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Artifact;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Resource;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.YarnComponent;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.CreateApplicationRequest;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.ApplicationErrorResponse;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.ResponseContext;
import com.sequenceiq.cloudbreak.orchestrator.yarn.util.ApplicationUtils;

@Service
public class ApplicationSubmissionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationSubmissionHandler.class);

    private static final int ONE = 1;

    @Inject
    private ApplicationUtils applicationUtils;

    private String ambariDbHostname;

    private String ambariServerHostname;

    public void submitApplication(ContainerConfig config, OrchestrationCredential cred, ContainerConstraint constraint, int componentNumber)
            throws CloudbreakOrchestratorFailedException {


        // Set Ambari DB hostname, if available
        if (ComponentType.AMBARIDB.equals(applicationUtils.getComponentType(constraint))) {
            ambariDbHostname = applicationUtils.getComponentHostName(constraint, cred, componentNumber);
        }

        // Set Ambari Server hostname, if available
        if (ComponentType.AMBARISERVER.equals(applicationUtils.getComponentType(constraint))) {
            ambariServerHostname = applicationUtils.getComponentHostName(constraint, cred, componentNumber);
        }

        // Application level attributes
        String applicationName = applicationUtils.getApplicationName(constraint, componentNumber);
        CreateApplicationRequest createApplicationRequest = new CreateApplicationRequest();
        createApplicationRequest.setName(applicationName);
        createApplicationRequest.setQueue(config.getQueue());
        createApplicationRequest.setLifetime(UNLIMITED);

        // Define the artifact (docker image) for the component
        Artifact artifact = new Artifact();
        artifact.setId(getDockerImageName(config));
        artifact.setType("DOCKER");

        // Define the resources for the component
        Resource resource = new Resource();
        resource.setCpus(getCpusForContainerType(constraint));
        resource.setMemory(getMemForContainerType(constraint));

        // Add the component
        List<YarnComponent> components = new ArrayList<>();
        YarnComponent component = new YarnComponent();
        component.setName(applicationUtils.getComponentName(constraint, componentNumber));
        component.setNumberOfContainers(ONE);
        component.setLaunchCommand(getFullEntrypoint(constraint, cred, componentNumber));
        component.setArtifact(artifact);
        component.setDependencies(new ArrayList<>());
        component.setResource(resource);
        component.setRunPrivilegedContainer(true);
        components.add(component);
        createApplicationRequest.setComponents(components);

        // Submit the request
        YarnClient yarnHttpClient = new YarnHttpClient(cred.getApiEndpoint());
        try {
            submitCreateApplicationRequest(createApplicationRequest, yarnHttpClient);
        } catch (RuntimeException e) {
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    private void submitCreateApplicationRequest(CreateApplicationRequest createApplicationRequest, YarnClient yarnHttpClient)
            throws CloudbreakOrchestratorFailedException {
        try {
            ResponseContext createAppResponseContext = yarnHttpClient.createApplication(createApplicationRequest);
            if (createAppResponseContext.getResponseError() != null) {
                ApplicationErrorResponse applicationErrorResponse = createAppResponseContext.getResponseError();
                String msg = String.format("ERROR: HTTP Return: %d Error: %s", createAppResponseContext.getStatusCode(),
                        applicationErrorResponse.getDiagnostics());
                LOGGER.debug(msg);
                throw new CloudbreakOrchestratorFailedException(msg);
            }
        } catch (Exception e) {
            throw new CloudbreakOrchestratorFailedException(e);
        }
    }

    private String getDockerImageName(ContainerConfig config) {
        return String.format("%s:%s", config.getName(), config.getVersion());
    }

    private int getCpusForContainerType(ContainerConstraint constraint) throws CloudbreakOrchestratorFailedException {
        if (ComponentType.AMBARIAGENT.equals(applicationUtils.getComponentType(constraint))) {
            return constraint.getCpu().intValue();
        }
        return NON_AGENT_CPUS;
    }

    private int getMemForContainerType(ContainerConstraint constraint) throws CloudbreakOrchestratorFailedException {
        if (ComponentType.AMBARIAGENT.equals(applicationUtils.getComponentType(constraint))) {
            return constraint.getMem().intValue();
        }
        return NON_AGENT_MEMORY;
    }

    private String getFullEntrypoint(ContainerConstraint constraint, OrchestrationCredential cred, int componentNumber)
            throws CloudbreakOrchestratorFailedException {
        Entrypoint entryPoint = Entrypoint.valueOf(applicationUtils.getComponentType(constraint).name());
        String fullEntryPoint = entryPoint.getEntryPoint();
        // Pass the Ambari DB hostname to the Ambari Server entry point
        if (ComponentType.AMBARISERVER.equals(applicationUtils.getComponentType(constraint))) {
            return String.format("%s %s", entryPoint.getEntryPoint(), ambariDbHostname);
        }
        // Pass the Ambari Server hostname to the Ambari Agent entry point
        if (ComponentType.AMBARIAGENT.equals(applicationUtils.getComponentType(constraint))) {
            return String.format("%s %s %s", entryPoint.getEntryPoint(), ambariServerHostname,
                    applicationUtils.getComponentHostName(constraint, cred, componentNumber));
        }
        return fullEntryPoint;
    }
}
