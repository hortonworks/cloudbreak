package com.sequenceiq.cloudbreak.core.flow;

import static com.sequenceiq.cloudbreak.core.flow.ContainerOrchestratorTool.SWARM;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl;
import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.containers.AmbariAgentBootstrap;
import com.sequenceiq.cloudbreak.core.flow.containers.AmbariServerBootstrap;
import com.sequenceiq.cloudbreak.core.flow.containers.AmbariServerDatabaseBootstrap;
import com.sequenceiq.cloudbreak.core.flow.containers.ConsulWatchBootstrap;
import com.sequenceiq.cloudbreak.core.flow.containers.MunchausenBootstrap;
import com.sequenceiq.cloudbreak.core.flow.containers.RegistratorBootstrap;
import com.sequenceiq.cloudbreak.core.flow.context.DockerContext;
import com.sequenceiq.cloudbreak.core.flow.context.SwarmContext;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;

@Service
public class SwarmContainerOrchestrator implements ContainerOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartService.class);
    private static final int TEN = 100;
    private static final int POLLING_INTERVAL = 5000;
    private static final int READ_TIMEOUT = 30000;
    private static final int MAX_POLLING_ATTEMPTS = 100;

    @Value("${cb.docker.container.ambari:sequenceiq/ambari:2.0.0-consul}")
    private String ambariDockerImageName;

    @Value("${cb.docker.container.registrator:sequenceiq/registrator:v5.1}")
    private String registratorDockerImageName;

    @Value("${cb.docker.container.munchausen:sequenceiq/munchausen:0.1}")
    private String munchausenDockerImageName;

    @Value("${cb.docker.container.docker.consul.watch.plugn:sequenceiq/docker-consul-watch-plugn:1.7.0-consul}")
    private String consulWatchPlugnDockerImageName;

    @Value("${cb.docker.container.ambari.db:postgres:9.4.1}")
    private String postgresDockerImageName;

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private HostGroupRepository hostGroupRepository;
    @Autowired
    private InstanceMetaDataRepository instanceMetaDataRepository;
    @Autowired
    private PollingService<DockerContext> dockerInfoPollingService;
    @Autowired
    private DockerCheckerTask dockerCheckerTask;
    @Autowired
    private PollingService<SwarmContext> swarmInfoPollingService;
    @Autowired
    private SwarmCheckerTask swarmCheckerTask;
    @Autowired
    private DockerImageCheckerTask dockerImageCheckerTask;

    /**
     * Bootstraps a Swarm based container orchestration cluster with a Consul discovery backend with the Munchausen tool.
     *
     * @param gatewayAddress    Public address of the gateway instance
     * @param nodes             Nodes that must be added to the Swarm cluster
     * @param consulServerCount Number of Consul servers in the cluster
     * @throws CloudbreakException
     */
    @Override
    public ContainerOrchestratorCluster bootstrap(Stack stack, String gatewayAddress, Set<Node> nodes, int consulServerCount) throws CloudbreakException {
        try {
            String privateGatewayIp = getPrivateGatewayIp(gatewayAddress, nodes);
            Set<String> privateAddresses = getPrivateAddresses(nodes);
            Set<String> privateAddressesWithoutGateway = getPrivateAddresses(getNodesWithoutGateway(gatewayAddress, nodes));
            String consulServers = selectConsulServers(privateGatewayIp, privateAddressesWithoutGateway, consulServerCount);
            String dockerAddresses = prepareDockerAddressInventory(privateAddresses);

            DockerClient dockerApiClient = DockerClientBuilder.getInstance(getDockerClientConfig(gatewayAddress))
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl())
                    .build();
            dockerInfoPollingService.pollWithTimeout(dockerCheckerTask, new DockerContext(stack, dockerApiClient, new ArrayList<String>()),
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            dockerInfoPollingService.pollWithTimeout(dockerImageCheckerTask,
                    new DockerContext(stack, dockerApiClient,
                            ImmutableList.<String>builder()
                                    .add(ambariDockerImageName)
                                    .add(munchausenDockerImageName)
                                    .add(registratorDockerImageName)
                                    .add(consulWatchPlugnDockerImageName)
                                    .add(postgresDockerImageName)
                                    .build()
                    ), POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            String[] cmd = {"--debug", "bootstrap", "--consulServers", consulServers, dockerAddresses};
            new MunchausenBootstrap(dockerApiClient, munchausenDockerImageName, cmd).call();
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(getSwarmClientConfig(gatewayAddress))
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl())
                    .build();
            swarmInfoPollingService.pollWithTimeout(swarmCheckerTask,
                    new SwarmContext(stack, swarmManagerClient, privateAddresses),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
            return new SwarmCluster(gatewayAddress, nodes, swarmManagerClient);
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
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

    @Override
    public ContainerOrchestratorCluster bootstrapNewNodes(Stack stack, String gatewayAddress, Set<Node> nodes) throws CloudbreakException {
        try {
            DockerClient dockerApiClient = DockerClientBuilder.getInstance(getDockerClientConfig(gatewayAddress)).build();
            Set<String> privateAddresses = getPrivateAddresses(nodes);
            String[] cmd = {"--debug", "add", "--join", getConsulJoinIp(gatewayAddress), prepareDockerAddressInventory(privateAddresses)};
            new MunchausenBootstrap(dockerApiClient, munchausenDockerImageName, cmd).call();
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(getSwarmClientConfig(gatewayAddress))
                    .withDockerCmdExecFactory(new DockerCmdExecFactoryImpl())
                    .build();
            swarmInfoPollingService.pollWithTimeout(swarmCheckerTask,
                    new SwarmContext(stack, swarmManagerClient, privateAddresses),
                    POLLING_INTERVAL,
                    MAX_POLLING_ATTEMPTS);
            return new SwarmCluster(gatewayAddress, nodes, swarmManagerClient);
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
    }

    private String getConsulJoinIp(String publicIp) {
        return String.format("consul://%s:8500", publicIp);
    }

    @Override
    public void startRegistrator(ContainerOrchestratorCluster cluster) throws CloudbreakException {
        try {
            Node gateway = getGatewayNode(cluster.getApiAddress(), cluster.getNodes());
            DockerClient swarmManagerClient = ((SwarmCluster) cluster).getDockerClient();
            new RegistratorBootstrap(swarmManagerClient, registratorDockerImageName, gateway.getHostname(), gateway.getPrivateIp()).call();
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
    }

    @Override
    public void startAmbariServer(ContainerOrchestratorCluster cluster) throws CloudbreakException {
        try {
            Node gateway = getGatewayNode(cluster.getApiAddress(), cluster.getNodes());
            DockerClient swarmManagerClient = ((SwarmCluster) cluster).getDockerClient();
            String databaseIp = new AmbariServerDatabaseBootstrap(swarmManagerClient, postgresDockerImageName).call();
            new AmbariServerBootstrap(swarmManagerClient, gateway.getPrivateIp(), databaseIp, ambariDockerImageName).call();
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
    }

    @Override
    public void startAmbariAgents(ContainerOrchestratorCluster cluster, int count) throws CloudbreakException {
        if (count > cluster.getNodes().size()) {
            throw new CloudbreakException("Cannot orchestrate more Ambari agent containers than the available nodes.");
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
                                ambariDockerImageName,
                                node.getHostname(),
                                node.getDataVolumes(),
                                String.valueOf(new Date().getTime()) + i)));
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
    }

    @Override
    public void startConsulWatches(ContainerOrchestratorCluster cluster, int count) throws CloudbreakException {
        if (count > cluster.getNodes().size()) {
            throw new CloudbreakException("Cannot orchestrate more Consul watch containers than the available nodes.");
        }
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(TEN);
            List<Future<Boolean>> futures = new ArrayList<>();
            DockerClient swarmManagerClient = ((SwarmCluster) cluster).getDockerClient();
            for (int i = 0; i < count; i++) {
                futures.add(executorService.submit(
                        new ConsulWatchBootstrap(
                                swarmManagerClient,
                                consulWatchPlugnDockerImageName,
                                String.valueOf(new Date().getTime()) + i
                        )));
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
    }

    @Override
    public ContainerOrchestratorTool type() {
        return SWARM;
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

    private String selectConsulServers(String gatewayAddress, Set<String> privateAddresses, int consulServerCount) {
        List<String> privateAddressList = new ArrayList<>(privateAddresses);
        int consulServers = consulServerCount < privateAddressList.size() ? consulServerCount : privateAddressList.size();
        String result = gatewayAddress + ",";
        for (int i = 0; i < consulServers - 1; i++) {
            result += privateAddressList.get(i) + ",";
        }
        return result.substring(0, result.length() - 1);
    }

}
