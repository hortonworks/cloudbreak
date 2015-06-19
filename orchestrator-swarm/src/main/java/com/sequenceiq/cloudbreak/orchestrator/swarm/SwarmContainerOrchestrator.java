package com.sequenceiq.cloudbreak.orchestrator.swarm;


import static com.sequenceiq.cloudbreak.orchestrator.SimpleContainerBootstrapRunner.simpleContainerBootstrapRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orchestrator.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.orchestrator.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.orchestrator.SimpleContainerBootstrapRunner;
import com.sequenceiq.cloudbreak.orchestrator.SimpleContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.AmbariAgentBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.AmbariServerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.AmbariServerDatabaseBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.BaywatchClientBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.BaywatchServerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.ConsulWatchBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.MunchausenBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.swarm.containers.RegistratorBootstrap;

public class SwarmContainerOrchestrator extends SimpleContainerOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwarmContainerOrchestrator.class);
    private static final int READ_TIMEOUT = 30000;
    private static final String MUNCHAUSEN_WAIT = "3600";
    private static final String MUNCHAUSEN_DOCKER_IMAGE = "sequenceiq/munchausen:0.3";
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
    public void bootstrap(GatewayConfig gatewayConfig, Set<Node> nodes, int consulServerCount, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        try {
            String privateGatewayIp = getPrivateGatewayIp(gatewayConfig.getAddress(), nodes);
            Set<String> privateAddresses = getPrivateAddresses(nodes);
            Set<String> privateAddressesWithoutGateway = getPrivateAddresses(getNodesWithoutGateway(gatewayConfig.getAddress(), nodes));
            Set<String> consulServers = selectConsulServers(privateGatewayIp, privateAddressesWithoutGateway, consulServerCount);
            Set<String> result = prepareDockerAddressInventory(privateAddresses);

            DockerClient dockerApiClient = DockerClientBuilder.getInstance(getDockerClientConfig(gatewayConfig))
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl()).build();
            String[] cmd = {"--debug", "bootstrap", "--wait", MUNCHAUSEN_WAIT, "--consulServers",
                    concatToString(consulServers), concatToString(result)};
            new MunchausenBootstrap(dockerApiClient, MUNCHAUSEN_DOCKER_IMAGE, cmd).call();
        } catch (CloudbreakOrchestratorCancelledException cloudbreakOrchestratorCancelledExceptionException) {
                throw cloudbreakOrchestratorCancelledExceptionException;
        } catch (CloudbreakOrchestratorFailedException cloudbreakOrchestratorFailedException) {
                throw cloudbreakOrchestratorFailedException;
        } catch (Exception ex) {
                throw new CloudbreakOrchestratorFailedException(ex);
        }
    }

    @Override
    public void bootstrapNewNodes(GatewayConfig gatewayConfig, Set<Node> nodes, ExitCriteriaModel exitCriteriaModel)
        throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        try {
            DockerClient dockerApiClient = DockerClientBuilder.getInstance(getDockerClientConfig(gatewayConfig)).build();
            Set<String> privateAddresses = getPrivateAddresses(nodes);
            Set<String> result = prepareDockerAddressInventory(privateAddresses);
            String[] cmd = {"--debug", "add", "--wait", MUNCHAUSEN_WAIT, "--join", getConsulJoinIp(gatewayConfig.getAddress()), concatToString(result)};
            new MunchausenBootstrap(dockerApiClient, MUNCHAUSEN_DOCKER_IMAGE, cmd).call();
        } catch (CloudbreakOrchestratorCancelledException cloudbreakOrchestratorCancelledExceptionException) {
            throw cloudbreakOrchestratorCancelledExceptionException;
        } catch (CloudbreakOrchestratorFailedException cloudbreakOrchestratorFailedException) {
            throw cloudbreakOrchestratorFailedException;
        } catch (Exception ex) {
            throw new CloudbreakOrchestratorFailedException(ex);
        }
    }

    @Override
    public void startRegistrator(ContainerOrchestratorCluster cluster, String imageName, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        try {
            Node gateway = getGatewayNode(cluster.getGatewayConfig().getAddress(), cluster.getNodes());
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(getSwarmClientConfig(cluster.getGatewayConfig()))
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl())
                    .build();
            simpleContainerBootstrapRunner(
                    new RegistratorBootstrap(swarmManagerClient, imageName, gateway.getHostname(), gateway.getPrivateIp()),
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
    public void startAmbariServer(ContainerOrchestratorCluster cluster, String dbImageName, String serverImageName, String platform,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        try {
            Node gateway = getGatewayNode(cluster.getGatewayConfig().getAddress(), cluster.getNodes());
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(getSwarmClientConfig(cluster.getGatewayConfig()))
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl())
                    .build();
            simpleContainerBootstrapRunner(new AmbariServerDatabaseBootstrap(swarmManagerClient, dbImageName, gateway.getHostname(), gateway.getDataVolumes()),
                    getExitCriteria(), exitCriteriaModel, MDC.getCopyOfContextMap()).call();
            simpleContainerBootstrapRunner(new AmbariServerBootstrap(swarmManagerClient, serverImageName,
                            gateway.getHostname(), gateway.getDataVolumes(), platform),
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
    public void startAmbariAgents(ContainerOrchestratorCluster cluster, String imageName, int count, String platform,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        if (count > cluster.getNodes().size()) {
            throw new CloudbreakOrchestratorFailedException("Cannot orchestrate more Ambari agent containers than the available nodes.");
        }
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(getSwarmClientConfig(cluster.getGatewayConfig()))
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl())
                    .build();
            Set<Node> nodes = getNodesWithoutGateway(cluster.getGatewayConfig().getAddress(), cluster.getNodes());
            Iterator<Node> nodeIterator = nodes.iterator();
            for (int i = 0; i < count; i++) {
                Node node = nodeIterator.next();
                String time = String.valueOf(new Date().getTime()) + i;
                AmbariAgentBootstrap runner =
                        new AmbariAgentBootstrap(swarmManagerClient, imageName, node.getHostname(), node.getDataVolumes(), time, platform);
                futures.add(getParallelContainerRunner()
                        .submit(simpleContainerBootstrapRunner(runner, getExitCriteria(), exitCriteriaModel, MDC.getCopyOfContextMap())));
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception exception) {
            if (exception instanceof CloudbreakOrchestratorCancelledException) {
                throw (CloudbreakOrchestratorCancelledException) exception;
            } else if (exception instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) exception;
            } else {
                throw new CloudbreakOrchestratorFailedException(exception);
            }
        }
    }

    @Override
    public void startConsulWatches(ContainerOrchestratorCluster cluster, String imageName, int count, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        if (count > cluster.getNodes().size()) {
            throw new CloudbreakOrchestratorFailedException("Cannot orchestrate more Consul watch containers than the available nodes.");
        }
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(getSwarmClientConfig(cluster.getGatewayConfig()))
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl())
                    .build();
            for (int i = 0; i < count; i++) {
                String time = String.valueOf(new Date().getTime()) + i;
                SimpleContainerBootstrapRunner runner = simpleContainerBootstrapRunner(
                        new ConsulWatchBootstrap(swarmManagerClient, imageName, time),
                        getExitCriteria(), exitCriteriaModel, MDC.getCopyOfContextMap());
                futures.add(getParallelContainerRunner().submit(runner));
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception exception) {
            if (exception instanceof CloudbreakOrchestratorCancelledException) {
                throw (CloudbreakOrchestratorCancelledException) exception;
            } else if (exception instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) exception;
            } else {
                throw new CloudbreakOrchestratorFailedException(exception);
            }
        }
    }

    @Override
    public void startBaywatchServer(ContainerOrchestratorCluster cluster, String imageName, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorCancelledException {
        try {
            Node gateway = getGatewayNode(cluster.getGatewayConfig().getAddress(), cluster.getNodes());
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(getSwarmClientConfig(cluster.getGatewayConfig()))
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl())
                    .build();
            simpleContainerBootstrapRunner(new BaywatchServerBootstrap(swarmManagerClient, imageName, gateway.getHostname()),
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
    public void startBaywatchClients(ContainerOrchestratorCluster cluster, String imageName, String gatewayPrivateIp, int count, String consulDomain,
            String externServerLocation, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorCancelledException {
        if (count > cluster.getNodes().size()) {
            throw new CloudbreakOrchestratorFailedException("Cannot orchestrate more Baywatch client containers than the available nodes.");
        }
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(getSwarmClientConfig(cluster.getGatewayConfig()))
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl())
                    .build();
            Set<Node> nodes = cluster.getNodes();
            Iterator<Node> nodeIterator = nodes.iterator();
            for (int i = 0; i < count; i++) {
                Node node = nodeIterator.next();
                String time = String.valueOf(new Date().getTime()) + i;
                BaywatchClientBootstrap runner =
                        new BaywatchClientBootstrap(swarmManagerClient, gatewayPrivateIp, imageName, time, node, node.getDataVolumes(),
                                consulDomain, externServerLocation);
                futures.add(getParallelContainerRunner().submit(simpleContainerBootstrapRunner(runner, getExitCriteria(), exitCriteriaModel,
                        MDC.getCopyOfContextMap())));
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception exception) {
            if (exception instanceof CloudbreakOrchestratorCancelledException) {
                throw (CloudbreakOrchestratorCancelledException) exception;
            } else if (exception instanceof CloudbreakOrchestratorFailedException) {
                throw (CloudbreakOrchestratorFailedException) exception;
            } else {
                throw new CloudbreakOrchestratorFailedException(exception);
            }
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
        } catch (Throwable t) {
            LOGGER.info(String.format("Cannot connect to Swarm manager, maybe it hasn't started yet: %s", t.getMessage()));
            return false;
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
            LOGGER.warn(String.format("Cannot connect to Swarm manager, maybe it hasn't started yet: %s", e.getMessage()));
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
                .withVersion("1.16")
                .withUri("https://" + gatewayConfig.getAddress() + "/swarm")
                .build();
    }

    private DockerClientConfig getDockerClientConfig(GatewayConfig gatewayConfig) {
        return DockerClientConfig.createDefaultConfigBuilder()
                .withReadTimeout(READ_TIMEOUT)
                .withDockerCertPath(gatewayConfig.getCertificateDir())
                .withVersion("1.16")
                .withUri("https://" + gatewayConfig.getAddress() + "/docker")
                .build();
    }

    private String getConsulJoinIp(String publicIp) {
        return String.format("consul://%s:8500", publicIp);
    }
}
