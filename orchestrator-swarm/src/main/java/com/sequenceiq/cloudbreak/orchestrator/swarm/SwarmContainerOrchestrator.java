package com.sequenceiq.cloudbreak.orchestrator.swarm;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.orchestrator.ContainerBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orchestrator.SimpleContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.AmbariAgentBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.AmbariServerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.AmbariServerDatabaseBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.BaywatchClientBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.BaywatchServerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.ConsulWatchBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.KerberosServerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.LogrotateBootsrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.MunchausenBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.RegistratorBootstrap;

public class SwarmContainerOrchestrator extends SimpleContainerOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwarmContainerOrchestrator.class);
    private static final int READ_TIMEOUT = 180_000;
    private static final String MUNCHAUSEN_WAIT = "3600";

    private static final int MAX_IP_FOR_ONE_REQUEST = 600;


    /**
     * Bootstraps a Swarm based container orchestration cluster with a Consul discovery backend with the Munchausen tool.
     *
     * @param gatewayConfig     Config used to access the gateway instance
     * @param nodes             Nodes that must be added to the Swarm cluster
     * @param consulServerCount Number of Consul servers in the cluster
     * @return The API address of the container orchestrator
     */
    @Override
    public void bootstrap(GatewayConfig gatewayConfig, ContainerConfig config, Set<Node> nodes, int consulServerCount, String consulLogLocation,
            ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try {
            String privateGatewayIp = getPrivateGatewayIp(gatewayConfig.getPublicAddress(), nodes);
            Set<String> privateAddresses = getPrivateAddresses(nodes);
            Set<String> privateAddressesWithoutGateway = getPrivateAddresses(getNodesWithoutGateway(gatewayConfig.getPublicAddress(), nodes));
            Set<String> consulServers = selectConsulServers(privateGatewayIp, privateAddressesWithoutGateway, consulServerCount);
            Set<String> result = prepareDockerAddressInventory(privateAddresses);

            String[] cmd = {"--debug", "bootstrap", "--wait", MUNCHAUSEN_WAIT, "--consulLogLocation", consulLogLocation, "--consulServers",
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
    public void bootstrapNewNodes(GatewayConfig gatewayConfig, ContainerConfig config, Set<Node> nodes, String consulLogLocation, ExitCriteriaModel
            exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try {
            Set<String> privateAddresses = getPrivateAddresses(nodes);
            Set<String> result = prepareDockerAddressInventory(privateAddresses);
            String[] cmd = {"--debug", "add", "--wait", MUNCHAUSEN_WAIT, "--consulLogLocation", consulLogLocation,
                    "--join", getConsulJoinIp(gatewayConfig.getPublicAddress()), concatToString(result)};

            runner(munchausenNewNodeBootstrap(gatewayConfig, imageName(config), cmd),
                    getExitCriteria(), exitCriteriaModel, MDC.getCopyOfContextMap()).call();

        } catch (CloudbreakOrchestratorCancelledException | CloudbreakOrchestratorFailedException coe) {
            throw coe;
        } catch (Exception ex) {
            throw new CloudbreakOrchestratorFailedException(ex);
        }
    }

    @Override
    public void startRegistrator(ContainerOrchestratorCluster cluster, ContainerConfig config, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try {
            Node gateway = getGatewayNode(cluster.getGatewayConfig().getPublicAddress(), cluster.getNodes());
            runner(registratorBootstrap(cluster.getGatewayConfig(), imageName(config), gateway), getExitCriteria(), exitCriteriaModel,
                    MDC.getCopyOfContextMap()).call();
        } catch (CloudbreakOrchestratorCancelledException | CloudbreakOrchestratorFailedException coe) {
            throw coe;
        } catch (Exception ex) {
            throw new CloudbreakOrchestratorFailedException(ex);
        }
    }

    @Override
    public void startAmbariServer(ContainerOrchestratorCluster cluster, ContainerConfig dbConfig, ContainerConfig serverConfig, String platform,
            LogVolumePath logVolumePath, Boolean localAgentRequired, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try {
            Node gateway = getGatewayNode(cluster.getGatewayConfig().getPublicAddress(), cluster.getNodes());
            runner(ambariServerDatabaseBootstrap(cluster.getGatewayConfig(), imageName(dbConfig), gateway, logVolumePath),
                    getExitCriteria(), exitCriteriaModel, MDC.getCopyOfContextMap()).call();
            runner(ambariServerBootstrap(cluster.getGatewayConfig(), imageName(serverConfig), gateway, platform, logVolumePath),
                    getExitCriteria(), exitCriteriaModel, MDC.getCopyOfContextMap()).call();
            if (localAgentRequired) {
                runner(ambariAgentBootstrap(cluster.getGatewayConfig(), imageName(serverConfig), gateway, String.valueOf(new Date().getTime()),
                        platform, logVolumePath), getExitCriteria(), exitCriteriaModel, MDC.getCopyOfContextMap()).call();
            }
        } catch (CloudbreakOrchestratorCancelledException | CloudbreakOrchestratorFailedException coe) {
            throw coe;
        } catch (Exception ex) {
            throw new CloudbreakOrchestratorFailedException(ex);
        }
    }

    @Override
    public void startAmbariAgents(ContainerOrchestratorCluster cluster, ContainerConfig config, String platform, LogVolumePath logVolumePath,
            ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            Set<Node> nodes = getNodesWithoutGateway(cluster.getGatewayConfig().getPublicAddress(), cluster.getNodes());
            int i = 0;
            for (Node node : nodes) {
                String time = String.valueOf(new Date().getTime()) + i++;
                AmbariAgentBootstrap ambariAgentBootstrap =
                        ambariAgentBootstrap(cluster.getGatewayConfig(), imageName(config), node, time, platform, logVolumePath);
                futures.add(getParallelContainerRunner()
                        .submit(runner(ambariAgentBootstrap, getExitCriteria(), exitCriteriaModel, MDC.getCopyOfContextMap())));
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception ex) {
            throw new CloudbreakOrchestratorFailedException(ex);
        }
    }

    @Override
    public void startConsulWatches(ContainerOrchestratorCluster cluster, ContainerConfig config, LogVolumePath logVolumePath,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            int i = 0;
            for (Node node : cluster.getNodes()) {
                String time = String.valueOf(new Date().getTime()) + i++;
                Callable<Boolean> runner = runner(
                        consulWatchBootstrap(cluster.getGatewayConfig(), imageName(config), node, time, logVolumePath), getExitCriteria(),
                        exitCriteriaModel, MDC.getCopyOfContextMap());
                futures.add(getParallelContainerRunner().submit(runner));
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception ex) {
            throw new CloudbreakOrchestratorFailedException(ex);
        }
    }

    @Override
    public void startKerberosServer(ContainerOrchestratorCluster cluster, ContainerConfig config, LogVolumePath logVolumePath,
            KerberosConfiguration kerberosConfiguration, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try {
            Node gateway = getGatewayNode(cluster.getGatewayConfig().getPublicAddress(), cluster.getNodes());
            runner(kerberosServerBootstrap(kerberosConfiguration, cluster.getGatewayConfig(), imageName(config), gateway, logVolumePath),
                    getExitCriteria(), exitCriteriaModel, MDC.getCopyOfContextMap()).call();
        } catch (CloudbreakOrchestratorCancelledException | CloudbreakOrchestratorFailedException coe) {
            throw coe;
        } catch (Exception ex) {
            throw new CloudbreakOrchestratorFailedException(ex);
        }
    }

    @Override
    public void startBaywatchServer(ContainerOrchestratorCluster cluster, ContainerConfig config, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try {
            Node gateway = getGatewayNode(cluster.getGatewayConfig().getPublicAddress(), cluster.getNodes());
            runner(baywatchServerBootstrap(cluster.getGatewayConfig(), imageName(config), gateway),
                    getExitCriteria(), exitCriteriaModel, MDC.getCopyOfContextMap()).call();
        } catch (CloudbreakOrchestratorCancelledException cloudbreakOrchestratorCancelledExceptionException) {
            throw cloudbreakOrchestratorCancelledExceptionException;
        } catch (CloudbreakOrchestratorFailedException cloudbreakOrchestratorFailedException) {
            throw cloudbreakOrchestratorFailedException;
        } catch (Exception ex) {
            throw new CloudbreakOrchestratorFailedException(ex);
        }
    }

    @Override
    public void startBaywatchClients(ContainerOrchestratorCluster cluster, ContainerConfig config, String consulDomain, LogVolumePath logVolumePath,
            String externServerLocation, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            int i = 0;
            for (Node node : cluster.getNodes()) {
                String time = String.valueOf(new Date().getTime()) + i++;
                BaywatchClientBootstrap baywatchClientBootstrap =
                        baywatchClientBootstrap(cluster.getGatewayConfig(), imageName(config), time, node,
                                consulDomain, logVolumePath, externServerLocation);
                futures.add(getParallelContainerRunner().submit(runner(baywatchClientBootstrap, getExitCriteria(), exitCriteriaModel,
                        MDC.getCopyOfContextMap())));
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception ex) {
            throw new CloudbreakOrchestratorFailedException(ex);
        }
    }

    @Override
    public void startLogrotate(ContainerOrchestratorCluster cluster, ContainerConfig config, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException {
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            int i = 0;
            for (Node node : cluster.getNodes()) {
                String time = String.valueOf(new Date().getTime()) + i;
                Callable<Boolean> runner = runner(logrotateBootsrap(cluster.getGatewayConfig(), imageName(config), node, time), getExitCriteria(),
                        exitCriteriaModel,
                        MDC.getCopyOfContextMap());
                futures.add(getParallelContainerRunner().submit(runner));
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception ex) {
            throw new CloudbreakOrchestratorFailedException(ex);
        }
    }

    @Override
    public boolean areAllNodesAvailable(GatewayConfig gatewayConfig, Set<Node> nodes) {
        LOGGER.info("Checking if Swarm manager is available and if the agents are registered.");
        try {
            List<String> allAvailableNode = getAvailableNodes(gatewayConfig, nodes);
            if (allAvailableNode.size() == getPrivateAddresses(nodes).size()) {
                return true;
            }
        } catch (Exception t) {
            LOGGER.info(String.format("Cannot connect to Swarm manager, maybe it hasn't started yet: %s", t.getMessage()));
        }
        return false;
    }

    @Override
    public List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes) {
        LOGGER.info("Checking if Swarm manager is available and if the agents are registered.");
        List<String> privateAddresses = new ArrayList<>();
        try {
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(getSwarmClientConfig(gatewayConfig))
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

    private Node getGatewayNode(String gatewayAddress, Collection<Node> nodes) {
        for (Node node : nodes) {
            if (node.getPublicIp() != null && node.getPublicIp().equals(gatewayAddress)) {
                return node;
            }
        }
        throw new RuntimeException("Gateway not found in cluster");
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

    private DockerClientConfig getSwarmClientConfig(GatewayConfig gatewayConfig) {
        return DockerClientConfig.createDefaultConfigBuilder()
                .withReadTimeout(READ_TIMEOUT)
                .withDockerCertPath(gatewayConfig.getCertificateDir())
                .withVersion("1.18")
                .withUri("https://" + gatewayConfig.getPublicAddress() + "/swarm")
                .build();
    }

    private DockerClientConfig getDockerClientConfig(GatewayConfig gatewayConfig) {
        return DockerClientConfig.createDefaultConfigBuilder()
                .withReadTimeout(READ_TIMEOUT)
                .withDockerCertPath(gatewayConfig.getCertificateDir())
                .withVersion("1.18")
                .withUri("https://" + gatewayConfig.getPublicAddress() + "/docker")
                .build();
    }

    @VisibleForTesting
    DockerClient dockerClient(GatewayConfig gatewayConfig) {
        return DockerClientBuilder.getInstance(getDockerClientConfig(gatewayConfig))
                .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl()).build();
    }

    @VisibleForTesting
    DockerClient swarmClient(GatewayConfig gatewayConfig) {
        return DockerClientBuilder.getInstance(getSwarmClientConfig(gatewayConfig))
                .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl()).build();
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
    RegistratorBootstrap registratorBootstrap(GatewayConfig gatewayConfig, String imageName, Node gateway) {
        DockerClient dockerApiClient = swarmClient(gatewayConfig);
        return new RegistratorBootstrap(dockerApiClient, imageName, gateway.getHostname(), gateway.getPrivateIp());
    }

    @VisibleForTesting
    ConsulWatchBootstrap consulWatchBootstrap(GatewayConfig gatewayConfig, String imageName, Node node, String time, LogVolumePath logVolumePath) {
        DockerClient dockerApiClient = swarmClient(gatewayConfig);
        return new ConsulWatchBootstrap(dockerApiClient, imageName, node, time, logVolumePath);
    }

    @VisibleForTesting
    BaywatchServerBootstrap baywatchServerBootstrap(GatewayConfig gatewayConfig, String imageName, Node gateway) {
        DockerClient dockerApiClient = swarmClient(gatewayConfig);
        return new BaywatchServerBootstrap(dockerApiClient, imageName, gateway.getHostname());
    }

    @VisibleForTesting
    BaywatchClientBootstrap baywatchClientBootstrap(GatewayConfig gatewayConfig, String imageName, String time, Node node,
            String consulDomain, LogVolumePath logVolumePath, String externServerLocation) {
        DockerClient dockerApiClient = swarmClient(gatewayConfig);
        return new BaywatchClientBootstrap(dockerApiClient, gatewayConfig.getPrivateAddress(), imageName, time, node, node.getDataVolumes(),
                consulDomain, externServerLocation, logVolumePath);
    }

    @VisibleForTesting
    LogrotateBootsrap logrotateBootsrap(GatewayConfig gatewayConfig, String imageName, Node node, String time) {
        DockerClient dockerApiClient = swarmClient(gatewayConfig);
        return new LogrotateBootsrap(dockerApiClient, imageName, node.getHostname(), time);
    }

    @VisibleForTesting
    AmbariAgentBootstrap ambariAgentBootstrap(GatewayConfig gatewayConfig, String imageName, Node node,
            String time, String platform, LogVolumePath logVolumePath) {
        DockerClient dockerApiClient = swarmClient(gatewayConfig);
        return new AmbariAgentBootstrap(dockerApiClient, imageName, node.getHostname(), node.getDataVolumes(), time,
                platform, logVolumePath);
    }

    @VisibleForTesting
    AmbariServerDatabaseBootstrap ambariServerDatabaseBootstrap(GatewayConfig gatewayConfig, String dbImageName, Node gateway, LogVolumePath logVolumePath) {
        DockerClient dockerApiClient = swarmClient(gatewayConfig);
        return new AmbariServerDatabaseBootstrap(dockerApiClient, dbImageName, gateway.getHostname(), gateway.getDataVolumes(),
                logVolumePath);
    }

    @VisibleForTesting
    AmbariServerBootstrap ambariServerBootstrap(GatewayConfig gatewayConfig, String serverImageName, Node node, String platform, LogVolumePath logVolumePath) {
        DockerClient dockerApiClient = swarmClient(gatewayConfig);
        return new AmbariServerBootstrap(dockerApiClient, serverImageName, node.getHostname(), node.getDataVolumes(), platform,
                logVolumePath);
    }

    @VisibleForTesting
    KerberosServerBootstrap kerberosServerBootstrap(KerberosConfiguration kerberosConfig, GatewayConfig gatewayConfig, String serverImageName, Node node,
            LogVolumePath logVolumePath) {
        DockerClient dockerApiClient = swarmClient(gatewayConfig);
        return new KerberosServerBootstrap(dockerApiClient, serverImageName, node.getHostname(), logVolumePath, kerberosConfig);
    }

    @VisibleForTesting
    public Callable<Boolean> runner(ContainerBootstrap bootstrap, ExitCriteria exitCriteria, ExitCriteriaModel exitCriteriaModel,
            Map<String, String> mdcMap) {
        return new ContainerBootstrapRunner(bootstrap, exitCriteria, exitCriteriaModel, mdcMap);
    }

    private String getConsulJoinIp(String publicIp) {
        return String.format("consul://%s:8500", publicIp);
    }

    private String imageName(ContainerConfig containerConfig) {
        return containerConfig.getName() + ":" + containerConfig.getVersion();
    }
}
