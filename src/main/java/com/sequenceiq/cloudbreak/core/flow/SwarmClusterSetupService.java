package com.sequenceiq.cloudbreak.core.flow;

import static com.sequenceiq.cloudbreak.core.flow.ClusterSetupTool.SWARM;

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
import com.sequenceiq.cloudbreak.core.flow.context.FlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.context.SwarmContext;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;

@Service
public class SwarmClusterSetupService implements ClusterSetupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartService.class);
    private static final int TEN = 100;
    private static final int POLLING_INTERVAL = 5000;
    private static final int READ_TIMEOUT = 5000;
    private static final int MAX_POLLING_ATTEMPTS = 100;

    @Value("${cb.docker.container.ambari:sequenceiq/ambari:2.0.0-consul}")
    private String ambariDockerContainer;

    @Value("${cb.docker.container.registrator:sequenceiq/registrator:v5.1}")
    private String registratorDockerContainer;

    @Value("${cb.docker.container.munchausen:sequenceiq/munchausen:0.1}")
    private String munchausenDockerContainer;

    @Value("${cb.docker.container.docker.consul.watch.plugn:sequenceiq/docker-consul-watch-plugn:1.7.0-consul}")
    private String dockerConsulWatchPlugnDockerContainer;

    @Value("${cb.docker.container.ambari.db:postgres:9.4.1}")
    private String ambariDbDockerContainer;

    @Autowired
    private StackRepository stackRepository;
    @Autowired
    private ClusterRepository clusterRepository;
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
    public void preSetup(Long stackId, InstanceGroup gateway, Set<InstanceGroup> hostGroupTypeGroups) throws CloudbreakException {
        try {
            Stack stack = stackRepository.findOneWithLists(stackId);
            InstanceMetaData gatewayData = gateway.getInstanceMetaData().iterator().next();
            String consulServers = getConsulServers(gatewayData, hostGroupTypeGroups, stack.getConsulServers());
            String consulJoinIps = getConsulJoinIps(stack.getInstanceGroups());
            for (InstanceMetaData instanceMetaData : stack.getAllInstanceMetaData()) {
                DockerClient dockerApiClient = DockerClientBuilder.getInstance(getDockerClientConfig(instanceMetaData.getPublicIp())).build();
                dockerInfoPollingService.pollWithTimeout(dockerCheckerTask,
                        new DockerContext(stack, dockerApiClient, new ArrayList<String>()),
                        POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
                dockerInfoPollingService.pollWithTimeout(dockerImageCheckerTask,
                        new DockerContext(stack, dockerApiClient,
                                ImmutableList.<String>builder()
                                        .add(ambariDockerContainer)
                                        .add(munchausenDockerContainer)
                                        .add(registratorDockerContainer)
                                        .add(dockerConsulWatchPlugnDockerContainer)
                                        .add(ambariDbDockerContainer)
                                        .build()
                        ),
                        POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            }
            DockerClient dockerApiClient = DockerClientBuilder.getInstance(getDockerClientConfig(stack.getAmbariIp())).build();
            String[] cmd = {"--debug", "bootstrap", "--consulServers", consulServers, consulJoinIps};
            new MunchausenBootstrap(dockerApiClient, munchausenDockerContainer, gatewayData.getPrivateIp(), cmd).call();
        } catch (Exception ex) {
            throw new CloudbreakException(ex);
        }
    }

    @Override
    public void preSetupNewNode(Long stackId, InstanceGroup gateway, Set<String> instanceIds) throws CloudbreakException {
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
                            .add(ambariDockerContainer)
                            .add(munchausenDockerContainer)
                            .add(registratorDockerContainer)
                            .add(dockerConsulWatchPlugnDockerContainer)
                            .add(ambariDbDockerContainer)
                            .build()),
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        }
        DockerClient dockerApiClient = DockerClientBuilder.getInstance(getDockerClientConfig(stack.getAmbariIp())).build();
        String[] cmd = {"--debug", "add", "--join", String.format("consul://%s:8500", stack.getAmbariIp()),
                prepareNewHostAddressJoin(stackId, instanceMetaDatas)};
        new MunchausenBootstrap(dockerApiClient, munchausenDockerContainer, gatewayData.getPrivateIp(), cmd).call();
    }

    @Override
    public void gatewaySetup(Long stackId, InstanceGroup gateway) throws CloudbreakException {
        try {
            Stack stack = stackRepository.findOneWithLists(stackId);
            InstanceMetaData gatewayData = gateway.getInstanceMetaData().iterator().next();
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(getSwarmClientConfig(stack.getAmbariIp())).build();
            swarmInfoPollingService.pollWithTimeout(swarmCheckerTask, new SwarmContext(stack, swarmManagerClient, stack.getFullNodeCount()),
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            ExecutorService executorService = Executors.newFixedThreadPool(TEN);
            List<Future<Boolean>> futures = new ArrayList<>();
            futures.add(executorService.submit(new RegistratorBootstrap(swarmManagerClient, registratorDockerContainer,
                    getLongNameTag(gatewayData.getLongName()), gatewayData.getPrivateIp())));
            futures.add(executorService.submit(new ConsulWatchBootstrap(swarmManagerClient, dockerConsulWatchPlugnDockerContainer,
                    getLongNameTag(gatewayData.getLongName()), gatewayData.getPrivateIp(), gatewayData.getId())));
            for (Future<Boolean> future : futures) {
                future.get();
            }
            String databaseIp = new AmbariServerDatabaseBootstrap(swarmManagerClient, ambariDbDockerContainer).call();
            new AmbariServerBootstrap(swarmManagerClient, gatewayData.getPrivateIp(), databaseIp, ambariDockerContainer).call();
        } catch (Exception ex) {
            throw new CloudbreakException(ex);
        }
    }

    @Override
    public void hostgroupsSetup(Long stackId, Set<InstanceGroup> instanceGroups) throws CloudbreakException {
        try {
            Stack stack = stackRepository.findOneWithLists(stackId);
            ExecutorService executorService = Executors.newFixedThreadPool(TEN);
            List<Future<Boolean>> futures = new ArrayList<>();
            DockerClient swarmManagerClient = DockerClientBuilder.getInstance(getSwarmClientConfig(stack.getAmbariIp())).build();
            swarmInfoPollingService.pollWithTimeout(swarmCheckerTask, new SwarmContext(stack, swarmManagerClient, stack.getFullNodeCount()),
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
            for (InstanceGroup instanceGroup : instanceGroups) {
                for (InstanceMetaData data : instanceGroup.getInstanceMetaData()) {
                    futures.add(executorService.submit(
                            new ConsulWatchBootstrap(swarmManagerClient, dockerConsulWatchPlugnDockerContainer,
                                    getLongNameTag(data.getLongName()), data.getPrivateIp(), data.getId())));
                }
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
            futures = new ArrayList<>();
            for (InstanceGroup instanceGroup : instanceGroups) {
                for (InstanceMetaData data : instanceGroup.getInstanceMetaData()) {
                    AmbariAgentBootstrap agentCreate =
                            new AmbariAgentBootstrap(swarmManagerClient, data.getPrivateIp(), getLongNameTag(data.getLongName()),
                                    ambariDockerContainer,
                                    instanceGroup.getTemplate().getVolumeCount(), data.getId());
                    futures.add(executorService.submit(agentCreate));
                }
            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
        } catch (Exception ex) {
            throw new CloudbreakException(ex);
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
                        new ConsulWatchBootstrap(swarmManagerClient, dockerConsulWatchPlugnDockerContainer,
                                getLongNameTag(data.getLongName()), data.getPrivateIp(), data.getId())));

            }
            for (Future<Boolean> future : futures) {
                future.get();
            }
            futures = new ArrayList<>();
            for (InstanceMetaData data : instanceMetaDatas) {
                AmbariAgentBootstrap agentCreate = new AmbariAgentBootstrap(swarmManagerClient, data.getPrivateIp(), getLongNameTag(data.getLongName()),
                        ambariDockerContainer,
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
    public FlowContext postSetup(Long stackId) throws CloudbreakException {
        Stack stack = stackRepository.findOneWithLists(stackId);
        return new ProvisioningContext.Builder()
                .setAmbariIp(stack.getAmbariIp())
                .setDefaultParams(stackId, stack.cloudPlatform())
                .build();
    }

    @Override
    public ClusterSetupTool clusterSetupTool() {
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

    private String getConsulJoinIps(Set<InstanceGroup> instanceGroups) {
        String result = "";
        for (InstanceGroup instanceGroup : instanceGroups) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getInstanceMetaData()) {
                result += instanceMetaData.getPrivateIp() + ":2376,";
            }
        }
        return result.substring(0, result.length() - 1);
    }

    private String getConsulServers(InstanceMetaData gateWayTypeGroup, Set<InstanceGroup> hostGroupTypeGroups, int consulServerCount) {
        String result = "";
        int collected = 0;
        result += gateWayTypeGroup.getPrivateIp() + ",";
        if (collected != consulServerCount) {
            for (InstanceGroup instanceGroup : hostGroupTypeGroups) {
                for (InstanceMetaData instanceMetaData : instanceGroup.getInstanceMetaData()) {
                    result += instanceMetaData.getPrivateIp() + ",";
                    collected++;
                    if (collected == consulServerCount) {
                        break;
                    }
                }
                if (collected == consulServerCount) {
                    break;
                }
            }
        }
        return result.substring(0, result.length() - 1);
    }


}
