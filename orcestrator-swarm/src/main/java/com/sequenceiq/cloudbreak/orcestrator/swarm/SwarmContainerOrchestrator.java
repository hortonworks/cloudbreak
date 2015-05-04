package com.sequenceiq.cloudbreak.orcestrator.swarm;

import static com.sequenceiq.cloudbreak.orcestrator.ContainerOrchestratorTool.SWARM;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl;
import com.sequenceiq.cloudbreak.orcestrator.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orcestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orcestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orcestrator.ContainerOrchestratorTool;
import com.sequenceiq.cloudbreak.orcestrator.Node;
import com.sequenceiq.cloudbreak.orcestrator.swarm.containers.AmbariAgentBootstrap;
import com.sequenceiq.cloudbreak.orcestrator.swarm.containers.AmbariServerBootstrap;
import com.sequenceiq.cloudbreak.orcestrator.swarm.containers.AmbariServerDatabaseBootstrap;
import com.sequenceiq.cloudbreak.orcestrator.swarm.containers.ConsulWatchBootstrap;
import com.sequenceiq.cloudbreak.orcestrator.swarm.containers.MunchausenBootstrap;
import com.sequenceiq.cloudbreak.orcestrator.swarm.containers.RegistratorBootstrap;

public class SwarmContainerOrchestrator implements ContainerOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SwarmContainerOrchestrator.class);
    private static final int TEN = 100;
    private static final int READ_TIMEOUT = 30000;
    private static final String MUNCHAUSEN_WAIT = "180";
    private static final String MUNCHAUSEN_DOCKER_IMAGE = "sequenceiq/munchausen:0.2";

    /**
     * Bootstraps a Swarm based container orchestration cluster with a Consul discovery backend with the Munchausen tool.
     *
     * @param gatewayAddress    Public address of the gateway instance
     * @param nodes             Nodes that must be added to the Swarm cluster
     * @param consulServerCount Number of Consul servers in the cluster
     */
    @Override
    public ContainerOrchestratorCluster bootstrap(String gatewayAddress, Set<Node> nodes, int consulServerCount)
            throws CloudbreakOrchestratorException {
        try {
            String privateGatewayIp = getPrivateGatewayIp(gatewayAddress, nodes);
            Set<String> privateAddresses = getPrivateAddresses(nodes);
            Set<String> privateAddressesWithoutGateway = getPrivateAddresses(getNodesWithoutGateway(gatewayAddress, nodes));
            String consulServers = selectConsulServers(privateGatewayIp, privateAddressesWithoutGateway, consulServerCount);
            String dockerAddresses = prepareDockerAddressInventory(privateAddresses);

            DockerClient dockerApiClient = DockerClientBuilder.getInstance(getDockerClientConfig(gatewayAddress))
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl()).build();
            String[] cmd = {"--debug", "bootstrap", "--wait", MUNCHAUSEN_WAIT, "--consulServers", consulServers, dockerAddresses};
            new MunchausenBootstrap(dockerApiClient, MUNCHAUSEN_DOCKER_IMAGE, cmd).call();
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(getSwarmClientConfig(gatewayAddress))
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl())
                    .build();
            return new SwarmCluster(gatewayAddress, nodes, swarmManagerClient);
        } catch (Exception e) {
            throw new CloudbreakOrchestratorException(e);
        }
    }

    @Override
    public ContainerOrchestratorCluster bootstrapNewNodes(String gatewayAddress, Set<Node> nodes) throws CloudbreakOrchestratorException {
        try {
            DockerClient dockerApiClient = DockerClientBuilder.getInstance(getDockerClientConfig(gatewayAddress)).build();
            Set<String> privateAddresses = getPrivateAddresses(nodes);
            String[] cmd = {"--debug", "add", "--wait", MUNCHAUSEN_WAIT, "--join", getConsulJoinIp(gatewayAddress),
                    prepareDockerAddressInventory(privateAddresses)};
            new MunchausenBootstrap(dockerApiClient, MUNCHAUSEN_DOCKER_IMAGE, cmd).call();
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(getSwarmClientConfig(gatewayAddress))
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl())
                    .build();
            return new SwarmCluster(gatewayAddress, nodes, swarmManagerClient);
        } catch (Exception e) {
            throw new CloudbreakOrchestratorException(e);
        }
    }

    @Override
    public void startRegistrator(ContainerOrchestratorCluster cluster, String imageName) throws CloudbreakOrchestratorException {
        try {
            Node gateway = getGatewayNode(cluster.getApiAddress(), cluster.getNodes());
            DockerClient swarmManagerClient = ((SwarmCluster) cluster).getDockerClient();
            new RegistratorBootstrap(swarmManagerClient, imageName, gateway.getHostname(), gateway.getPrivateIp()).call();
        } catch (Exception e) {
            throw new CloudbreakOrchestratorException(e);
        }
    }

    @Override
    public void startAmbariServer(ContainerOrchestratorCluster cluster, String dbImageName, String serverImageName) throws CloudbreakOrchestratorException {
        try {
            DockerClient swarmManagerClient = ((SwarmCluster) cluster).getDockerClient();
            new AmbariServerDatabaseBootstrap(swarmManagerClient, dbImageName).call();
            new AmbariServerBootstrap(swarmManagerClient, serverImageName).call();
        } catch (Exception e) {
            throw new CloudbreakOrchestratorException(e);
        }
    }

    @Override
    public void startAmbariAgents(ContainerOrchestratorCluster cluster, String imageName, int count) throws CloudbreakOrchestratorException {
        if (count > cluster.getNodes().size()) {
            throw new CloudbreakOrchestratorException("Cannot orchestrate more Ambari agent containers than the available nodes.");
        }
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(TEN);
            List<Future<Boolean>> futures = new ArrayList<>();
            DockerClient swarmManagerClient = ((SwarmCluster) cluster).getDockerClient();
            Set<Node> nodes = getNodesWithoutGateway(cluster.getApiAddress(), cluster.getNodes());
            Iterator<Node> nodeIterator = nodes.iterator();
            for (int i = 0; i < count; i++) {
                Node node = nodeIterator.next();
                futures.add(executorService.submit(
                        new AmbariAgentBootstrap(
                                swarmManagerClient,
                                imageName,
                                node.getHostname(),
                                node.getDataVolumes(),
                                String.valueOf(new Date().getTime()) + i)));
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            throw new CloudbreakOrchestratorException(e);
        }
    }

    @Override
    public void startConsulWatches(ContainerOrchestratorCluster cluster, String imageName, int count) throws CloudbreakOrchestratorException {
        if (count > cluster.getNodes().size()) {
            throw new CloudbreakOrchestratorException("Cannot orchestrate more Consul watch containers than the available nodes.");
        }
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(TEN);
            List<Future<Boolean>> futures = new ArrayList<>();
            DockerClient swarmManagerClient = ((SwarmCluster) cluster).getDockerClient();
            for (int i = 0; i < count; i++) {
                futures.add(executorService.submit(
                        new ConsulWatchBootstrap(
                                swarmManagerClient,
                                imageName,
                                String.valueOf(new Date().getTime()) + i
                        )));
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            throw new CloudbreakOrchestratorException(e);
        }
    }

    @Override
    public boolean areAllNodesAvailable(String gatewayAddress, Set<Node> nodes) {
        LOGGER.info("Checking if Swarm manager is available and if the agents are registered.");
        try {
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(getSwarmClientConfig(gatewayAddress))
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl())
                    .build();
            List<Object> driverStatus = swarmManagerClient.infoCmd().exec().getDriverStatuses();
            LOGGER.debug("Swarm manager is available, checking registered agents.");
            int found = 0;
            for (Object element : driverStatus) {
                try {
                    List objects = (ArrayList) element;
                    for (String address : getPrivateAddresses(nodes)) {
                        if (((String) objects.get(1)).split(":")[0].equals(address)) {
                            found++;
                            break;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error(String.format("Docker info returned an unexpected element: %s", element), e);
                }
            }
            if (found == getPrivateAddresses(nodes).size()) {
                return true;
            }
        } catch (Throwable t) {
            return false;
        }
        return false;
    }

    @Override
    public boolean isBootstrapApiAvailable(String gatewayAddress) {
        LOGGER.info("Checking if docker daemon is available.");
        try {
            DockerClient dockerApiClient = DockerClientBuilder.getInstance(getDockerClientConfig(gatewayAddress))
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl()).build();
            dockerApiClient.infoCmd().exec();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public ContainerOrchestratorTool type() {
        return SWARM;
    }

    private String selectConsulServers(String gatewayAddress, Set<String> privateAddresses, int consulServerCount) {
        List<String> privateAddressList = new ArrayList<>(privateAddresses);
        int consulServers = consulServerCount < privateAddressList.size() ? consulServerCount : privateAddressList.size();
        String result = gatewayAddress + ",";
        for (int i = 0; i < consulServers - 1; i++) {
            result += privateAddressList.get(i) + ",";
        }
        return result.substring(0, result.length() - 1);
    }

    private Set<String> getPrivateAddresses(Set<Node> nodes) {
        Set<String> privateAddresses = new HashSet<>();
        for (Node node : nodes) {
            privateAddresses.add(node.getPrivateIp());
        }
        return privateAddresses;
    }

    private String getPrivateGatewayIp(String gatewayAddress, Set<Node> nodes) {
        for (Node node : nodes) {
            if (node.getPublicIp() != null && node.getPublicIp().equals(gatewayAddress)) {
                return node.getPrivateIp();
            }
        }
        return null;
    }

    private Node getGatewayNode(String gatewayAddress, Set<Node> nodes) {
        for (Node node : nodes) {
            if (node.getPublicIp() != null && node.getPublicIp().equals(gatewayAddress)) {
                return node;
            }
        }
        throw new RuntimeException("Gateway not found in cluster");
    }

    private Set<Node> getNodesWithoutGateway(String gatewayAddress, Set<Node> nodes) {
        Set<Node> coreNodes = new HashSet<>();
        for (Node node : nodes) {
            if (node.getPublicIp() == null || !node.getPublicIp().equals(gatewayAddress)) {
                coreNodes.add(node);
            }
        }
        return coreNodes;
    }

    private String prepareDockerAddressInventory(Set<String> nodeAddresses) {
        StringBuilder sb = new StringBuilder();
        for (String nodeAddress : nodeAddresses) {
            sb.append(String.format("%s:2376,", nodeAddress));
        }
        return sb.substring(0, sb.length() - 1);
    }

    private DockerClientConfig getSwarmClientConfig(String ip) {
        return DockerClientConfig.createDefaultConfigBuilder()
                .withReadTimeout(READ_TIMEOUT)
                .withVersion("1.16")
                .withUri("http://" + ip + ":3376")
                .build();
    }

    private DockerClientConfig getDockerClientConfig(String ip) {
        return DockerClientConfig.createDefaultConfigBuilder()
                .withReadTimeout(READ_TIMEOUT)
                .withVersion("1.16")
                .withUri("http://" + ip + ":2376")
                .build();
    }

    private String getConsulJoinIp(String publicIp) {
        return String.format("consul://%s:8500", publicIp);
    }

}
