package com.sequenceiq.cloudbreak.orchestrator.marathon;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.orchestrator.SimpleContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;
import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.utils.MarathonException;

@Component
public class MarathonContainerOrchestrator extends SimpleContainerOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarathonContainerOrchestrator.class);


    @Override
    public List<ContainerInfo> runContainer(ContainerConfig config, OrchestrationCredential cred, ContainerConstraint constraint,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        List<ContainerInfo> result = new ArrayList<>();
        Marathon client = MarathonClient.getInstance(cred.getApiEndpoint());

        App app = new App();
        app.setId(constraint.getName());
        app.setCpus(constraint.getCpu());
        app.setMem(constraint.getMem());
        app.setInstances(constraint.getInstances());
        for (Integer port : constraint.getPorts()) {
            app.addPort(port);
        }

        try {
            app = client.createApp(app);
        } catch (MarathonException e) {
            String msg = String.format("Marathon container creation failed. From image: '%s', with name: '%s'!", config.getName(), constraint.getName());
            LOGGER.error(msg, e);
            throw new CloudbreakOrchestratorFailedException(msg, e);
        }

        //collect container info from mesos api
        return result;
    }

    @Override
    public void startContainer(List<ContainerInfo> info, OrchestrationCredential cred) throws CloudbreakOrchestratorException {

    }

    @Override
    public void stopContainer(List<ContainerInfo> info, OrchestrationCredential cred) throws CloudbreakOrchestratorException {

    }

    @Override
    public void deleteContainer(List<String> ids, OrchestrationCredential cred) throws CloudbreakOrchestratorException {

    }

    @Override
    public boolean areAllNodesAvailable(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return false;
    }

    @Override
    public List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return null;
    }

    @Override
    public String name() {
        return "MARATHON";
    }

    @Override
    public void bootstrap(GatewayConfig gatewayConfig, ContainerConfig config, Set<Node> nodes, int consulServerCount, String consulLogLocation,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {

    }

    @Override
    public void bootstrapNewNodes(GatewayConfig gatewayConfig, ContainerConfig containerConfig, Set<Node> nodes, String consulLogLocation,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {

    }

    @Override
    public boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig) {
        return false;
    }

    @Override
    public int getMaxBootstrapNodes() {
        return 0;
    }
}
