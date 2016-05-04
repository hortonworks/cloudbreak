package com.sequenceiq.cloudbreak.orchestrator.swarm;


import static com.github.dockerjava.api.model.RestartPolicy.alwaysRestart;
import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.orchestrator.ContainerBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.SimpleContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.model.port.TcpPortBinding;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.BindsBuilder;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.MunchausenBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.SwarmContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.SwarmContainerDeletion;

@Component
public class SwarmContainerOrchestrator extends SimpleContainerOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwarmContainerOrchestrator.class);
    private static final int READ_TIMEOUT = 180_000;
    private static final String MUNCHAUSEN_WAIT = "3600";
    private static final int MAX_IP_FOR_ONE_REQUEST = 600;
    private static final String ENV_KEY_VALUE_SEPARATOR = "=";


    /**
     * Bootstraps a Swarm based container orchestration cluster with a Consul discovery backend with the Munchausen tool.
     *
     * @param gatewayConfig     Config used to access the gateway instance
     * @param nodes             Nodes that must be added to the Swarm cluster
     * @param consulServerCount Number of Consul servers in the cluster
     * @return The API address of the container orchestrator
     */
    @Override
    public void bootstrap(GatewayConfig gatewayConfig, ContainerConfig config, Set<Node> nodes, int consulServerCount,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        try {
            String privateGatewayIp = getPrivateGatewayIp(gatewayConfig.getPublicAddress(), nodes);
            Set<String> privateAddresses = getPrivateAddresses(nodes);
            Set<String> privateAddressesWithoutGateway = getPrivateAddresses(getNodesWithoutGateway(gatewayConfig.getPublicAddress(), nodes));
            Set<String> consulServers = selectConsulServers(privateGatewayIp, privateAddressesWithoutGateway, consulServerCount);
            Set<String> result = prepareDockerAddressInventory(privateAddresses);

            String[] cmd = {"--debug", "bootstrap", "--wait", MUNCHAUSEN_WAIT, "--consulServers",
                    concatToString(consulServers), concatToString(result)};

            runner(munchausenBootstrap(gatewayConfig, imageName(config), cmd),
                    getExitCriteria(), exitCriteriaModel, MDC.getCopyOfContextMap()).call();

        } catch (CloudbreakOrchestratorCancelledException | CloudbreakOrchestratorFailedException coe) {
            throw coe;
        } catch (Exception ex) {
            throw new CloudbreakOrchestratorFailedException(ex);
        }
    }

    @Override
    public void bootstrapNewNodes(GatewayConfig gatewayConfig, ContainerConfig config, Set<Node> nodes,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        try {
            Set<String> privateAddresses = getPrivateAddresses(nodes);
            Set<String> result = prepareDockerAddressInventory(privateAddresses);
            String[] cmd = {"--debug", "add", "--wait", MUNCHAUSEN_WAIT, "--join", getConsulJoinIp(gatewayConfig.getPrivateAddress()), concatToString(result)};

            runner(munchausenNewNodeBootstrap(gatewayConfig, imageName(config), cmd),
                    getExitCriteria(), exitCriteriaModel, MDC.getCopyOfContextMap()).call();

        } catch (CloudbreakOrchestratorCancelledException | CloudbreakOrchestratorFailedException coe) {
            throw coe;
        } catch (Exception ex) {
            throw new CloudbreakOrchestratorFailedException(ex);
        }
    }

    @Override
    public void validateApiEndpoint(OrchestrationCredential cred) throws CloudbreakOrchestratorException {
        // TODO
        return;
    }

    @Override
    public List<ContainerInfo> runContainer(ContainerConfig config, OrchestrationCredential cred, ContainerConstraint constraint,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        List<ContainerInfo> containerInfos = new ArrayList<>();
        String image = imageName(config);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            int i = 0;
            for (String fqdn : constraint.getHosts()) {
                String nodeName = fqdn.split("\\.")[0];
                DockerClient dockerApiClient = swarmClient(cred);
                String name = createSwarmContainerName(constraint, i++);
                CreateContainerCmd createCmd = decorateCreateContainerCmd(image, constraint, nodeName, dockerApiClient, name);
                ContainerBootstrap bootstrap = new SwarmContainerBootstrap(dockerApiClient, nodeName, createCmd);
                Callable<Boolean> runner = runner(bootstrap, getExitCriteria(), exitCriteriaModel, MDC.getCopyOfContextMap());
                futures.add(getParallelContainerRunner().submit(runner));
                containerInfos.add(new ContainerInfo(name, name, fqdn, image));
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
            return containerInfos;
        } catch (Exception ex) {
            deleteContainer(containerInfos, cred);
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
    public void deleteContainer(List<ContainerInfo> containerInfo, OrchestrationCredential cred) throws CloudbreakOrchestratorException {
        try {
            DockerClient dockerApiClient = swarmClient(cred);
            List<Future<Boolean>> futures = new ArrayList<>();
            for (ContainerInfo info : containerInfo) {
                try {
                    String hostName = info.getHost().split("\\.")[0];
                    SwarmContainerDeletion containerRemover = new SwarmContainerDeletion(dockerApiClient, hostName, info.getName());
                    Callable<Boolean> runner = runner(containerRemover, getExitCriteria(), null, MDC.getCopyOfContextMap());
                    futures.add(getParallelContainerRunner().submit(runner));
                } catch (Exception me) {
                    throw new CloudbreakOrchestratorFailedException(me);
                }
            }

            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception ex) {
            String msg = String.format("Failed to delete containers: '%s'.", Arrays.toString(containerInfo.toArray(new String[containerInfo.size()])));
            throw new CloudbreakOrchestratorFailedException(msg, ex);
        }
    }

    @Override
    public List<String> getMissingNodes(GatewayConfig gatewayConfig, Set<Node> nodes) {
        Set<String> missingNodes = getPrivateAddresses(nodes);
        LOGGER.info("Checking if Swarm manager is available and if the agents are registered.");
        try {
            List<String> allAvailableNodes = getAvailableNodes(gatewayConfig, nodes);
            LOGGER.info("Available swarm nodes: {}/{}", allAvailableNodes.size(), missingNodes.size());
            for (String availableNode : allAvailableNodes) {
                missingNodes.remove(availableNode);
            }

        } catch (Exception t) {
            LOGGER.info(String.format("Cannot connect to Swarm manager, maybe it hasn't started yet: %s", t.getMessage()));
        }
        return Lists.newArrayList(missingNodes);
    }

    @Override
    public List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes) {
        LOGGER.info("Checking if Swarm manager is available and if the agents are registered.");
        List<String> privateAddresses = new ArrayList<>();
        try {
            DockerClientConfig swarmClientConfig = getSwarmClientConfig(gatewayConfig.getGatewayUrl(), gatewayConfig.getCertificateDir());
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(swarmClientConfig)
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl())
                    .build();
            List<Object> driverStatus = swarmManagerClient.infoCmd().exec().getDriverStatuses();
            LOGGER.debug("Swarm manager is available, checking registered agents.");
            for (Object element : driverStatus) {
                try {
                    List objects = (ArrayList) element;
                    for (Node node : nodes) {
                        if (((String) objects.get(1)).split(":")[0].equals(node.getPrivateIp())) {
                            privateAddresses.add(node.getPrivateIp());
                            break;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn(String.format("Docker info returned an unexpected element: %s", element), e);
                }
            }
            return privateAddresses;
        } catch (Exception e) {
            String defaultErrorMessage = "502 Bad Gateway";
            String errorMessage = e.getMessage().contains(defaultErrorMessage) ? defaultErrorMessage : e.getMessage();
            LOGGER.warn(String.format("Cannot connect to Swarm manager, maybe it hasn't started yet: %s", errorMessage));
            return privateAddresses;
        }
    }

    @Override
    public boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig) {
        LOGGER.info("Checking if docker daemon is available.");
        try {
            DockerClient dockerApiClient = DockerClientBuilder.getInstance(getDockerClientConfig(gatewayConfig))
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl()).build();
            dockerApiClient.infoCmd().exec();
            return true;
        } catch (Exception ex) {
            LOGGER.warn(String.format("Docker api not available: %s", ex.getMessage()));
            return false;
        }
    }

    @Override
    public int getMaxBootstrapNodes() {
        return MAX_IP_FOR_ONE_REQUEST;
    }

    @Override
    public String name() {
        return "SWARM";
    }

    private String createSwarmContainerName(ContainerConstraint constraint, int index) {
        String name = constraint.getContainerName().getName();
        if (constraint.getHosts().size() > 1) {
            name = String.format("%s-%s", name, index);
        }
        return name;
    }

    private CreateContainerCmd decorateCreateContainerCmd(String image, ContainerConstraint constraint, String hostname,
            DockerClient dockerApiClient, String name) {
        String[] env = createEnv(constraint, hostname);
        String[] cmd = constraint.getCmd();
        CreateContainerCmd createCmd = dockerApiClient.createContainerCmd(image)
                .withName(name)
                .withRestartPolicy(alwaysRestart())
                .withPrivileged(true)
                .withEnv(env);

        if (cmd != null && cmd.length > 0) {
            createCmd.withCmd(cmd);
        }

        if (!StringUtils.isEmpty(constraint.getNetworkMode())) {
            createCmd.withNetworkMode(constraint.getNetworkMode());
        }

        Bind[] binds = createVolumeBinds(constraint);
        if (binds.length > 0) {
            createCmd.withBinds(binds);
        }

        TcpPortBinding portBinding = constraint.getTcpPortBinding();
        if (portBinding != null) {
            Ports ports = new Ports(ExposedPort.tcp(portBinding.getExposedPort()), new Ports.Binding(portBinding.getHostIp(), portBinding.getHostPort()));
            createCmd.withPortBindings(ports);
        }

        List<Link> links = new ArrayList<>();
        for (Entry<String, String> entry : constraint.getLinks().entrySet()) {
            Link link = new Link(entry.getKey(), entry.getValue());
            links.add(link);
        }
        createCmd.withLinks(links.toArray(new Link[links.size()]));

        return createCmd;
    }

    private String[] createEnv(ContainerConstraint constraint, String hostname) {
        List<String> env = new ArrayList<>();
        for (Entry<String, String> envEntry : constraint.getEnv().entrySet()) {
            String envVariable = envEntry.getKey() + ENV_KEY_VALUE_SEPARATOR + envEntry.getValue();
            env.add(envVariable);
        }
        env.add(format("constraint:node==%s", hostname));
        String[] result = new String[env.size()];
        return env.toArray(result);
    }

    private Bind[] createVolumeBinds(ContainerConstraint constraint) {
        BindsBuilder bindsBuilder = new BindsBuilder();
        for (Entry<String, String> entry : constraint.getVolumeBinds().entrySet()) {
            String hostPath = entry.getKey();
            String containerPath = entry.getValue();
            if (StringUtils.isEmpty(containerPath)) {
                bindsBuilder.add(hostPath);
            } else {
                bindsBuilder.add(hostPath, containerPath);
            }
        }
        return bindsBuilder.build();
    }

    private Set<String> selectConsulServers(String gatewayAddress, Set<String> privateAddresses, int consulServerCount) {
        List<String> privateAddressList = new ArrayList<>(privateAddresses);
        int consulServers = consulServerCount <= privateAddressList.size() + 1 ? consulServerCount : privateAddressList.size();
        Set<String> result = new HashSet<>();
        result.add(gatewayAddress);
        for (int i = 0; i < consulServers - 1; i++) {
            result.add(privateAddressList.get(i));
        }
        return result;
    }

    private String concatToString(Collection<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            sb.append(item + ",");
        }
        return sb.toString().substring(0, sb.toString().length() - 1);
    }

    private Set<String> getPrivateAddresses(Collection<Node> nodes) {
        Set<String> privateAddresses = new HashSet<>();
        for (Node node : nodes) {
            privateAddresses.add(node.getPrivateIp());
        }
        return privateAddresses;
    }

    private String getPrivateGatewayIp(String gatewayAddress, Collection<Node> nodes) {
        for (Node node : nodes) {
            if (node.getPublicIp() != null && node.getPublicIp().equals(gatewayAddress)) {
                return node.getPrivateIp();
            }
        }
        return null;
    }

    private Set<Node> getNodesWithoutGateway(String gatewayAddress, Collection<Node> nodes) {
        Set<Node> coreNodes = new HashSet<>();
        for (Node node : nodes) {
            if (node.getPublicIp() == null || !node.getPublicIp().equals(gatewayAddress)) {
                coreNodes.add(node);
            }
        }
        return coreNodes;
    }

    @VisibleForTesting
    Set<String> prepareDockerAddressInventory(Collection<String> nodeAddresses) {
        Set<String> nodeResult = new HashSet<>();
        for (String nodeAddress : nodeAddresses) {
            nodeResult.add(String.format("%s:2376", nodeAddress));
        }
        return nodeResult;
    }

    private DockerClientConfig getSwarmClientConfig(String gatewayUrl, String certificateDir) {
        return DockerClientConfig.createDefaultConfigBuilder()
                .withDockerCertPath(certificateDir)
                .withVersion("1.18")
                .withUri(gatewayUrl + "/swarm")
                .build();
    }

    private DockerClientConfig getDockerClientConfig(GatewayConfig gatewayConfig) {
        return DockerClientConfig.createDefaultConfigBuilder()
                .withDockerCertPath(gatewayConfig.getCertificateDir())
                .withVersion("1.18")
                .withUri(gatewayConfig.getGatewayUrl() + "/docker")
                .build();
    }

    @VisibleForTesting
    DockerClient dockerClient(GatewayConfig gatewayConfig) {
        return DockerClientBuilder.getInstance(getDockerClientConfig(gatewayConfig))
                .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl().withReadTimeout(READ_TIMEOUT)).build();
    }

    @VisibleForTesting
    DockerClient swarmClient(GatewayConfig gatewayConfig) {
        return DockerClientBuilder.getInstance(getSwarmClientConfig(gatewayConfig.getGatewayUrl(), gatewayConfig.getCertificateDir()))
                .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl().withReadTimeout(READ_TIMEOUT)).build();
    }

    DockerClient swarmClient(OrchestrationCredential cred) {
        String gatewayUrl = "https://" + cred.getApiEndpoint();
        return DockerClientBuilder.getInstance(getSwarmClientConfig(gatewayUrl, (String) cred.getProperties().get("certificateDir")))
                .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl().withReadTimeout(READ_TIMEOUT))
                .build();
    }

    @VisibleForTesting
    MunchausenBootstrap munchausenBootstrap(GatewayConfig gatewayConfig, String imageName, String[] cmd) {
        DockerClient dockerApiClient = dockerClient(gatewayConfig);
        return new MunchausenBootstrap(dockerApiClient, imageName, cmd);
    }

    @VisibleForTesting
    MunchausenBootstrap munchausenNewNodeBootstrap(GatewayConfig gatewayConfig, String imageName, String[] cmd) {
        DockerClient dockerApiClient = swarmClient(gatewayConfig);
        return new MunchausenBootstrap(dockerApiClient, imageName, cmd);
    }

    @VisibleForTesting
    public Callable<Boolean> runner(ContainerBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel,
            Map<String, String> mdcMap) {
        return new ContainerBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, mdcMap);
    }

    private String getConsulJoinIp(String privateIp) {
        return String.format("consul://%s:8500", privateIp);
    }

    private String imageName(ContainerConfig containerConfig) {
        return containerConfig.getName() + ":" + containerConfig.getVersion();
    }
}
