package com.sequenceiq.cloudbreak.orchestrator.marathon;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.sequenceiq.cloudbreak.orchestrator.ContainerBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.SimpleContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;
import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.model.v2.Container;
import mesosphere.marathon.client.model.v2.Docker;
import mesosphere.marathon.client.model.v2.Task;
import mesosphere.marathon.client.utils.MarathonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class MarathonContainerOrchestrator extends SimpleContainerOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarathonContainerOrchestrator.class);
    private static final double MIN_CPU = 0.5;
    private static final int MIN_MEM = 1024;
    private static final int MIN_INSTANCES = 1;
    private static final String HOST_NETWORK_MODE = "HOST";
    private static final String DOCKER_CONTAINER_TYPE = "DOCKER";
    private static final String SPACE = " ";


    @Override
    public void validateApiEndpoint(OrchestrationCredential cred) throws CloudbreakOrchestratorException {
        Marathon client = MarathonClient.getInstance(cred.getApiEndpoint());
        try {
            client.getServerInfo();
        } catch (Exception e) {
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public List<ContainerInfo> runContainer(ContainerConfig config, OrchestrationCredential cred, ContainerConstraint constraint,
                                            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        try {
            List<ContainerInfo> result = new ArrayList<>();
            String image = config.getName() + ":" + config.getVersion();
            Marathon client = MarathonClient.getInstance(cred.getApiEndpoint());
            App app = createMarathonApp(config, constraint, image);
            app = postAppToMarathon(config, client, app);

            MarathonAppBootstrap bootstrap = new MarathonAppBootstrap(client, app);
            Callable<Boolean> runner = runner(bootstrap, getExitCriteria(), exitCriteriaModel);
            Future<Boolean> appFuture = getParallelContainerRunner().submit(runner);
            appFuture.get();

            App appResponse = client.getApp(app.getId()).getApp();
            for (Task task : appResponse.getTasks()) {
                result.add(new ContainerInfo(appResponse.getId(), appResponse.getId(), task.getHost(), image));
            }
            return result;
        } catch (Exception ex) {
            throw new CloudbreakOrchestratorFailedException(ex);
        }
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

    private App createMarathonApp(ContainerConfig config, ContainerConstraint constraint, String image) {
        App app = new App();
        String name = constraint.getName().replace("_", "-") + "-" + new Date().getTime();
        app.setId(name);
        app.setCpus(constraint.getCpu() != null ? constraint.getCpu() : MIN_CPU);
        app.setMem(constraint.getMem() != null ? constraint.getMem() : MIN_MEM);
        app.setInstances(constraint.getInstances() != null ? constraint.getInstances() : MIN_INSTANCES);
        app.setEnv(constraint.getEnv());

        String[] arrayOfCmd = constraint.getCmd();
        if (arrayOfCmd != null && arrayOfCmd.length > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("/usr/sbin/init");
            for (String cmd : arrayOfCmd) {
                sb.append(SPACE);
                sb.append(cmd);
            }
            app.setCmd(sb.toString());
        }

        for (Integer port : constraint.getPorts()) {
            app.addPort(port);
        }

        Docker docker = new Docker();
        docker.setPrivileged(true);
        docker.setImage(image);
        docker.setNetwork(HOST_NETWORK_MODE);

        Container container = new Container();
        container.setType(DOCKER_CONTAINER_TYPE);
        container.setDocker(docker);
        app.setContainer(container);
        return app;
    }

    private App postAppToMarathon(ContainerConfig config, Marathon client, App app) throws CloudbreakOrchestratorFailedException {
        try {
            return client.createApp(app);
        } catch (MarathonException e) {
            String msg = String.format("Marathon container creation failed. From image: '%s', with name: '%s'!", config.getName(), app.getId());
            LOGGER.error(msg, e);
            throw new CloudbreakOrchestratorFailedException(msg, e);
        }
    }

    private Callable<Boolean> runner(ContainerBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel) {
        return new ContainerBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap());
    }
}
