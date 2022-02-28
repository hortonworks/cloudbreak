package com.sequenceiq.cloudbreak.orchestrator.yarn;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.YARN;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.container.SimpleContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.orchestrator.yarn.client.YarnClient;
import com.sequenceiq.cloudbreak.orchestrator.yarn.client.YarnHttpClient;
import com.sequenceiq.cloudbreak.orchestrator.yarn.handler.ApplicationDetailHandler;
import com.sequenceiq.cloudbreak.orchestrator.yarn.handler.ApplicationSubmissionHandler;
import com.sequenceiq.cloudbreak.orchestrator.yarn.model.request.DeleteApplicationRequest;
import com.sequenceiq.cloudbreak.orchestrator.yarn.poller.YarnAppBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.yarn.util.ApplicationUtils;

@Component
public class YarnContainerOrchestrator extends SimpleContainerOrchestrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnContainerOrchestrator.class);

    @Value("${cb.docker.container.yarn.ambari.agent:}")
    private String ambariAgent;

    @Value("${cb.docker.container.yarn.ambari.server:}")
    private String ambariServer;

    @Value("${cb.docker.container.yarn.ambari.db:}")
    private String postgresDockerImageName;

    @Value("${cb.max.yarn.orchestrator.retry:300}")
    private int maxRetry;

    @Value("${cb.max.yarn.orchestrator.retry.onerror:15}")
    private int maxRetryOnError;

    @Value("${cb.max.yarn.orchestrator.sleep:3000}")
    private int sleepTime;

    @Inject
    private ApplicationDetailHandler detailHandler;

    @Inject
    private ApplicationSubmissionHandler submitHandler;

    @Inject
    private ApplicationUtils applicationUtils;

    @Override
    public void validateApiEndpoint(OrchestrationCredential cred) throws CloudbreakOrchestratorException {
        YarnClient yarnHttpClient = new YarnHttpClient(cred.getApiEndpoint());
        try {
            yarnHttpClient.validateApiEndpoint();
        } catch (Exception e) {
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    @Override
    public List<ContainerInfo> runContainer(ContainerConfig config, OrchestrationCredential cred, ContainerConstraint constraint,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {

        // Create an application per component
        List<ContainerInfo> containerInfos = new ArrayList<>();
        for (int componentNumber = 1; componentNumber <= constraint.getInstances(); componentNumber++) {
            try {
                submitHandler.submitApplication(config, cred, constraint, componentNumber);

                String applicationName = applicationUtils.getApplicationName(constraint, componentNumber);

                OrchestratorBootstrap bootstrap = new YarnAppBootstrap(applicationName, cred.getApiEndpoint());

                Callable<Boolean> runner = runner(bootstrap, getExitCriteria(), exitCriteriaModel);
                runner.call();
                containerInfos.add(detailHandler.getContainerInfo(config, cred, constraint, componentNumber));
            } catch (Exception e) {
                throw new CloudbreakOrchestratorFailedException(e);
            }
        }
        return containerInfos;
    }

    @Override
    public void deleteContainer(List<ContainerInfo> containerInfo, OrchestrationCredential cred) throws CloudbreakOrchestratorException {
        for (ContainerInfo container: containerInfo) {
            DeleteApplicationRequest deleteApplicationRequest = new DeleteApplicationRequest();
            deleteApplicationRequest.setName(container.getName());
            YarnClient yarnHttpClient = new YarnHttpClient(cred.getApiEndpoint());
            try {
                yarnHttpClient.deleteApplication(deleteApplicationRequest);
            } catch (Exception e) {
                throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
            }
        }
    }

    @Override
    public String name() {
        return YARN;
    }

    //
    // The remaining methods are all unused, but required by the interface.
    // The default values provided match other implementations where these methods are unused.
    //
    @Override
    public List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return null;
    }

    @Override
    public void bootstrapNewNodes(GatewayConfig gatewayConfig, ContainerConfig containerConfig, Set<Node> nodes, ExitCriteriaModel exitCriteriaModel) {
        assert false;
    }

    @Override
    public void bootstrap(GatewayConfig gatewayConfig, ContainerConfig config, Set<Node> nodes, int consulServerCount, ExitCriteriaModel exitCriteriaModel) {
        assert false;
    }

    private Callable<Boolean> runner(OrchestratorBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel) {
        return new OrchestratorBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, MDC.getCopyOfContextMap(), maxRetry, sleepTime, maxRetryOnError);
    }

    @Override
    public int getMaxBootstrapNodes() {
        return 0;
    }

    @Override
    public List<String> getMissingNodes(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return null;
    }

    @Override
    public void startContainer(List<ContainerInfo> info, OrchestrationCredential cred) {
        assert false;
    }

    @Override
    public void stopContainer(List<ContainerInfo> info, OrchestrationCredential cred) {
        assert false;
    }

    @Override
    public boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig) {
        return false;
    }

    @Override
    public String ambariServerContainer(Optional<String> name) {
        return name.isPresent() ? name.get() : ambariServer;
    }

    @Override
    public String ambariClientContainer(Optional<String> name) {
        return name.isPresent() ? name.get() : ambariAgent;
    }

    @Override
    public String ambariDbContainer(Optional<String> name) {
        return name.isPresent() ? name.get() : postgresDockerImageName;
    }
}
