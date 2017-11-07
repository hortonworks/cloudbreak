package com.sequenceiq.cloudbreak.cloud.yarn;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.orchestrator.yarn.client.YarnHttpClient;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Artifact;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.Resource;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.core.YarnComponent;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.CreateApplicationRequest;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.response.ResponseContext;
import static com.sequenceiq.cloudbreak.orchestrator.yarn.api.YarnResourceConstants.UNLIMITED;

@Service
public class YarnInstanceConnector implements InstanceConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(YarnInstanceConnector.class);
    private YarnHttpClient yarnHttpClient = new YarnHttpClient("http://yprod001.l42scl.hortonworks.com:9191/");

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext authenticatedContext,
                                             List<CloudResource> resources, List<CloudInstance> vms) {

        CreateApplicationRequest createApplicationRequest = new CreateApplicationRequest();
        createApplicationRequest.setName("CB-" + UUID.randomUUID());
        createApplicationRequest.setQueue("system-tests");
        createApplicationRequest.setLifetime(UNLIMITED);

        // Taken from ApplicationSubmissionHandler
        // Define the artifact (docker image) for the component
        Artifact artifact = new Artifact();
        artifact.setId("registry.eng.hortonworks.com/hortonworks/base-ubuntu16:0.1.0.0-27"); // TODO: Take it from param
        artifact.setType("DOCKER");

        // Define the resources for the component
        Resource resource = new Resource(); // TODO: Take it from param
        resource.setCpus(2);
        resource.setMemory(4096);

        LOGGER.info("Starting VMs: {}", vms);
        List<YarnComponent> components = vms.stream()
                .map(
                    vm -> {
                        YarnComponent component = new YarnComponent();
                        component.setName(vm.getInstanceId());
                        component.setNumberOfContainers(1);
                        component.setLaunchCommand("/bootstrap/privileged-start-services-script"); // TODO: Take it from param
                        component.setArtifact(artifact);
                        component.setDependencies(new ArrayList<>());
                        component.setResource(resource);
                        component.setRunPrivilegedContainer(true);
                        return component;
                    })
                .collect(Collectors.toList());
        createApplicationRequest.setComponents(components);

        ResponseContext response;
        try {
            response = yarnHttpClient.createApplication(createApplicationRequest);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();


        return null;
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext authenticatedContext, List<CloudResource> resources, List<CloudInstance> vms) {
        return null;
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext authenticatedContext, List<CloudInstance> vms) {
        return null;
    }

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        return null;
    }
}