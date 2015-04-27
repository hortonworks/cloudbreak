package com.sequenceiq.cloudbreak.core.flow;

import static com.sequenceiq.cloudbreak.core.flow.ContainerOrchestratorTool.SWARM;

import java.util.ArrayList;
import java.util.HashSet;
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
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
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

    @Override
    public ContainerOrchestratorClient bootstrap(Long stackId) throws CloudbreakException {
        try {
            Stack stack = stackRepository.findOneWithLists(stackId);
            InstanceGroup gateway = stack.getGatewayInstanceGroup();
            InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
            String consulServers = getConsulServers(gateway, stack.getCoreInstanceGroups(), stack.getConsulServers());
            String dockerAddresses = getDockerAddressInventory(stack.getInstanceGroups());

            DockerClient dockerApiClient = DockerClientBuilder.getInstance(getDockerClientConfig(gatewayInstance.getPublicIp())).build();
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
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(getSwarmClientConfig(gatewayInstance.getPublicIp())).build();
            swarmInfoPollingService.pollWithTimeout(swarmCheckerTask, new SwarmContext(stack, swarmManagerClient, stack.getFullNodeCount()),
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            return new SwarmContainerOrchestratorClient(swarmManagerClient);
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
    }

    @Override
    public void preSetupNewNode(Long stackId, InstanceGroup gateway, Set<String> instanceIds) throws CloudbreakException {
        try {
            Stack stack = stackRepository.findOneWithLists(stackId);
            InstanceMetaData gatewayData = gateway.getInstanceMetaData().iterator().next();
            Set<InstanceMetaData> instanceMetaDatas = new HashSet<>();
            for (String instanceId : instanceIds) {
                InstanceMetaData instanceMetaData = instanceMetaDataRepository.findHostInStackByInstanceId(stackId, instanceId);
                instanceMetaDatas.add(instanceMetaData);
            }
            for (InstanceMetaData instanceMetaData : instanceMetaDatas) {
                DockerClient dockerApiClient = DockerClientBuilder.getInstance(getDockerClientConfig(instanceMetaData.getPublicIp())).build();
                dockerInfoPollingService.pollWithTimeout(dockerCheckerTask,
                        new DockerContext(stack, dockerApiClient, new ArrayList<String>()),
                        POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);

                dockerInfoPollingService.pollWithTimeout(dockerImageCheckerTask,
                        new DockerContext(stack, dockerApiClient, ImmutableList.<String>builder()
                                .add(ambariDockerImageName)
                                .add(munchausenDockerImageName)
                                .add(registratorDockerImageName)
                                .add(consulWatchPlugnDockerImageName)
                                .add(postgresDockerImageName)
                                .build()),
                        POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            }
            DockerClient dockerApiClient = DockerClientBuilder.getInstance(getDockerClientConfig(stack.getAmbariIp())).build();

            String[] cmd = {"--debug", "add", "--join", String.format("consul://%s:8500", stack.getAmbariIp()),
                    prepareNewHostAddressJoin(stackId, instanceMetaDatas)};
            new MunchausenBootstrap(dockerApiClient, munchausenDockerImageName, cmd).call();
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
    }

    @Override
    public void startRegistrator(ContainerOrchestratorClient client, Long stackId) throws CloudbreakException {
        try {
            Stack stack = stackRepository.findOneWithLists(stackId);
            InstanceMetaData gateway = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
            DockerClient swarmManagerClient = ((SwarmContainerOrchestratorClient) client).getDockerClient();
            new RegistratorBootstrap(swarmManagerClient, registratorDockerImageName, getLongNameTag(gateway.getLongName()), gateway.getPrivateIp()).call();
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
    }

    @Override
    public void startAmbariServer(ContainerOrchestratorClient client, Long stackId) throws CloudbreakException {
        try {
            Stack stack = stackRepository.findOneWithLists(stackId);
            InstanceMetaData gateway = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
            DockerClient swarmManagerClient = ((SwarmContainerOrchestratorClient) client).getDockerClient();
            String databaseIp = new AmbariServerDatabaseBootstrap(swarmManagerClient, postgresDockerImageName).call();
            new AmbariServerBootstrap(swarmManagerClient, gateway.getPrivateIp(), databaseIp, ambariDockerImageName).call();
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
    }

    @Override
    public void startAmbariAgents(ContainerOrchestratorClient client, Long stackId) throws CloudbreakException {
        try {
            Stack stack = stackRepository.findOneWithLists(stackId);
            ExecutorService executorService = Executors.newFixedThreadPool(TEN);
            List<Future<Boolean>> futures = new ArrayList<>();
            DockerClient swarmManagerClient = ((SwarmContainerOrchestratorClient) client).getDockerClient();
            for (InstanceGroup instanceGroup : stack.getCoreInstanceGroups()) {
                for (InstanceMetaData data : instanceGroup.getInstanceMetaData()) {
                    futures.add(executorService.submit(
                            new AmbariAgentBootstrap(
                                    swarmManagerClient,
                                    data.getPrivateIp(),
                                    getLongNameTag(data.getLongName()),
                                    ambariDockerImageName,
                                    instanceGroup.getTemplate().getVolumeCount(),
                                    data.getId())));
                }
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
    }

    @Override
    public void startConsulWatches(ContainerOrchestratorClient client, Long stackId) throws CloudbreakException {
        try {
            Stack stack = stackRepository.findOneWithLists(stackId);
            ExecutorService executorService = Executors.newFixedThreadPool(TEN);
            List<Future<Boolean>> futures = new ArrayList<>();
            DockerClient swarmManagerClient = ((SwarmContainerOrchestratorClient) client).getDockerClient();
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                for (InstanceMetaData data : instanceGroup.getInstanceMetaData()) {
                    futures.add(executorService.submit(
                            new ConsulWatchBootstrap(
                                    swarmManagerClient,
                                    consulWatchPlugnDockerImageName,
                                    getLongNameTag(data.getLongName()),
                                    data.getPrivateIp(),
                                    data.getId())));
                }
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            throw new CloudbreakException(e);
        }
    }

    @Override
    public void newHostgroupNodesSetup(Long stackId, Set<String> instanceIds, String hostGroup) throws CloudbreakException {
        try {
            Set<InstanceMetaData> instanceMetaDatas = new HashSet<>();
            for (String instanceId : instanceIds) {
                InstanceMetaData instanceMetaData = instanceMetaDataRepository.findHostInStackByInstanceId(stackId, instanceId);
                instanceMetaDatas.add(instanceMetaData);
            }
            Stack stack = stackRepository.findOneWithLists(stackId);
            HostGroup hostGroupObject = hostGroupRepository.findHostGroupsByInstanceGroupName(stack.getCluster().getId(), hostGroup);

            ExecutorService executorService = Executors.newFixedThreadPool(TEN);
            List<Future<Boolean>> futures = new ArrayList<>();
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(getSwarmClientConfig(stack.getAmbariIp())).build();
            swarmInfoPollingService.pollWithTimeout(swarmCheckerTask, new SwarmContext(stack, swarmManagerClient, stack.getFullNodeCount()),
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            for (InstanceMetaData data : instanceMetaDatas) {
                futures.add(executorService.submit(
                        new ConsulWatchBootstrap(swarmManagerClient, consulWatchPlugnDockerImageName,
                                getLongNameTag(data.getLongName()), data.getPrivateIp(), data.getId())));

            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
            futures = new ArrayList<>();
            for (InstanceMetaData data : instanceMetaDatas) {
                AmbariAgentBootstrap agentCreate = new AmbariAgentBootstrap(swarmManagerClient, data.getPrivateIp(), getLongNameTag(data.getLongName()),
                        ambariDockerImageName,
                        hostGroupObject.getInstanceGroup().getTemplate().getVolumeCount(), data.getId());
                futures.add(executorService.submit(agentCreate));
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception ex) {
            throw new CloudbreakException(ex);
        }
    }

    @Override
    public ContainerOrchestratorTool type() {
        return SWARM;
    }

    private String getLongNameTag(String longName) {
        return longName.split("\\.")[0];
    }

    private String prepareNewHostAddressJoin(Long stackId, Set<InstanceMetaData> metaDatas) throws CloudbreakException {
        StringBuilder sb = new StringBuilder();
        for (InstanceMetaData instanceMetaData : metaDatas) {
            try {
                sb.append(String.format("%s:2376,", instanceMetaData.getPrivateIp()));
            } catch (Exception ex) {
                throw new CloudbreakException(ex);
            }
        }
        return sb.toString().substring(0, sb.toString().length() - 1);
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

    private String getDockerAddressInventory(Set<InstanceGroup> instanceGroups) {
        String result = "";
        for (InstanceGroup instanceGroup : instanceGroups) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getInstanceMetaData()) {
                result += instanceMetaData.getPrivateIp() + ":2376,";
            }
        }
        return result.substring(0, result.length() - 1);
    }

    private String getConsulServers(InstanceGroup gateway, Set<InstanceGroup> coreGroups, int consulServerCount) {
        List<InstanceMetaData> instances = new ArrayList<>();
        instances.addAll(gateway.getInstanceMetaData());
        for (InstanceGroup instanceGroup : coreGroups) {
            instances.addAll(instanceGroup.getInstanceMetaData());
        }
        int consulServers = consulServerCount < instances.size() ? consulServerCount : instances.size();
        String result = "";
        for (int i = 0; i < consulServers; i++) {
            result += instances.get(i).getPrivateIp() + ",";
        }
        return result.substring(0, result.length() - 1);
    }

}
