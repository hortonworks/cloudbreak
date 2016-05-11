package com.sequenceiq.cloudbreak.core.bootstrap.service.container;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.SWARM;
import static com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedExitCriteriaModel;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_AGENT;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_DB;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_SERVER;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.CONSUL_WATCH;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.HAVEGED;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.KERBEROS;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.LDAP;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.LOGROTATE;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.REGISTRATOR;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.SHIPYARD;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.SHIPYARD_DB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ContainerConfigService;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ContainerService;

@Component
public class ClusterContainerRunner {

    private static final String NONE = "none";

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private ContainerConfigService containerConfigService;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Inject
    private ContainerService containerService;

    @Inject
    private ConversionService conversionService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private ContainerConstraintFactory constraintFactory;

    public Map<String, List<Container>> runClusterContainers(ProvisioningContext context) throws CloudbreakException {
        try {
            Stack stack = stackRepository.findOneWithLists(context.getStackId());
            String cloudPlatform = context.getCloudPlatform() != null ? context.getCloudPlatform().value() : NONE;
            return initializeClusterContainers(stack, cloudPlatform);
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    public Map<String, List<Container>> addClusterContainers(Long stackId, String cloudPlatform, String hostGroupName, Integer scalingAdjustment)
            throws CloudbreakException {
        try {
            Stack stack = stackRepository.findOneWithLists(stackId);
            return addClusterContainers(stack, cloudPlatform != null ? cloudPlatform : NONE, hostGroupName, scalingAdjustment);
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    private Map<String, List<Container>> initializeClusterContainers(Stack stack, String cloudPlatform)
            throws CloudbreakException, CloudbreakOrchestratorException {

        Orchestrator orchestrator = stack.getOrchestrator();
        Map<String, Object> map = new HashMap<>();
        map.putAll(orchestrator.getAttributes().getMap());
        map.put("certificateDir", tlsSecurityService.prepareCertDir(stack.getId()));
        OrchestrationCredential credential = new OrchestrationCredential(orchestrator.getApiEndpoint(), map);
        ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get(orchestrator.getType());
        Map<String, List<ContainerInfo>> containers = new HashMap<>();
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());

        String gatewayHostname = getGatewayHostName(stack);

        try {
            if (SWARM.equals(orchestrator.getType())) {
                ContainerConstraint registratorConstraint = constraintFactory.getRegistratorConstraint(gatewayHostname, cluster.getName(),
                        getGatewayPrivateIp(stack));
                containers.put(REGISTRATOR.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, REGISTRATOR), credential,
                        registratorConstraint, clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId())));
            }

            ContainerConstraint ambariServerDbConstraint = constraintFactory.getAmbariServerDbConstraint(gatewayHostname, cluster.getName());
            List<ContainerInfo> dbContainer = containerOrchestrator.runContainer(containerConfigService.get(stack, AMBARI_DB), credential,
                    ambariServerDbConstraint, clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId()));
            containers.put(AMBARI_DB.name(), dbContainer);

            String serverDbHostName = dbContainer.get(0).getHost();
            ContainerConstraint ambariServerConstraint = constraintFactory.getAmbariServerConstraint(serverDbHostName, gatewayHostname,
                    cloudPlatform, cluster.getName());
            List<ContainerInfo> ambariServerContainer = containerOrchestrator.runContainer(containerConfigService.get(stack, AMBARI_SERVER),
                    credential, ambariServerConstraint, clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId()));
            containers.put(AMBARI_SERVER.name(), ambariServerContainer);
            String ambariServerHost = ambariServerContainer.get(0).getHost();

            if (cluster.isSecure()) {
                ContainerConstraint havegedConstraint = constraintFactory.getHavegedConstraint(gatewayHostname, cluster.getName());
                containers.put(HAVEGED.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, HAVEGED), credential, havegedConstraint,
                        clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId())));

                ContainerConstraint kerberosServerConstraint = constraintFactory.getKerberosServerConstraint(cluster, gatewayHostname);
                containers.put(KERBEROS.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, KERBEROS), credential,
                        kerberosServerConstraint, clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId())));
            }

            if (cluster.isLdapRequired()) {
                ContainerConstraint ldapConstraint = constraintFactory.getLdapConstraint(ambariServerHost);
                containers.put(LDAP.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, LDAP), credential, ldapConstraint,
                        clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId())));
            }

            if (SWARM.equals(orchestrator.getType()) && cluster.getEnableShipyard()) {
                ContainerConstraint shipyardDbConstraint = constraintFactory.getShipyardDbConstraint(ambariServerHost);
                containers.put(SHIPYARD_DB.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, SHIPYARD_DB), credential,
                        shipyardDbConstraint, clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId())));

                ContainerConstraint shipyardConstraint = constraintFactory.getShipyardConstraint(ambariServerHost);
                containers.put(SHIPYARD.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, SHIPYARD), credential, shipyardConstraint,
                        clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId())));
            }

            List<String> hostBlackList = new ArrayList<>();
            for (HostGroup hostGroup : hostGroupRepository.findHostGroupsInCluster(stack.getCluster().getId())) {
                ContainerConstraint ambariAgentConstraint = constraintFactory.getAmbariAgentConstraint(ambariServerHost, null, cloudPlatform, hostGroup,
                        null, hostBlackList);
                List<ContainerInfo> containerInfos = containerOrchestrator.runContainer(containerConfigService.get(stack, AMBARI_AGENT), credential,
                        ambariAgentConstraint, clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId()));
                containers.put(hostGroup.getName(), containerInfos);
                hostBlackList.addAll(getHostsFromContainerInfo(containerInfos));
            }

            if (SWARM.equals(orchestrator.getType())) {
                List<String> hosts = getHosts(stack);
                ContainerConstraint consulWatchConstraint = constraintFactory.getConsulWatchConstraint(hosts);
                containers.put(CONSUL_WATCH.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, CONSUL_WATCH), credential,
                        consulWatchConstraint, clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId())));

                ContainerConstraint logrotateConstraint = constraintFactory.getLogrotateConstraint(hosts);
                containers.put(LOGROTATE.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, LOGROTATE), credential,
                        logrotateConstraint, clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId())));
            }
            return saveContainers(containers, cluster);
        } catch (CloudbreakOrchestratorException ex) {
            if (!containers.isEmpty()) {
                saveContainers(containers, cluster);
            }
            checkCancellation(ex);
            throw ex;
        }
    }

    private String getGatewayHostName(Stack stack) {
        String gatewayHostname = "";
        if (stack.getInstanceGroups() != null && !stack.getInstanceGroups().isEmpty()) {
            InstanceMetaData gatewayInstance = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
            gatewayHostname = gatewayInstance.getDiscoveryFQDN();
        }
        return gatewayHostname;
    }

    private String getGatewayPrivateIp(Stack stack) {
        String gatewayHostname = "";
        if (stack.getInstanceGroups() != null && !stack.getInstanceGroups().isEmpty()) {
            InstanceMetaData gatewayInstance = stack.getGatewayInstanceGroup().getInstanceMetaData().iterator().next();
            gatewayHostname = gatewayInstance.getPrivateIp();
        }
        return gatewayHostname;
    }

    private Map<String, List<Container>> addClusterContainers(Stack stack, String cloudPlatform, String hostGroupName, Integer adjustment)
            throws CloudbreakException, CloudbreakOrchestratorException {

        Orchestrator orchestrator = stack.getOrchestrator();
        Map<String, Object> map = new HashMap<>();
        map.putAll(orchestrator.getAttributes().getMap());
        map.put("certificateDir", tlsSecurityService.prepareCertDir(stack.getId()));
        OrchestrationCredential credential = new OrchestrationCredential(orchestrator.getApiEndpoint(), map);
        ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get(orchestrator.getType());
        Map<String, List<ContainerInfo>> containers = new HashMap<>();
        Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());

        try {
            Set<Container> existingContainers = containerService.findContainersInCluster(cluster.getId());
            String ambariServerHost = FluentIterable.from(existingContainers).firstMatch(new Predicate<Container>() {
                @Override
                public boolean apply(Container input) {
                    return input.getImage().contains(AMBARI_SERVER.getName());
                }
            }).get().getHost();
            final HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(cluster.getId(), hostGroupName);
            String ambariAgentApp = FluentIterable.from(existingContainers).firstMatch(new Predicate<Container>() {
                @Override
                public boolean apply(Container input) {
                    return hostGroup.getHostNames().contains(input.getHost()) && input.getImage().contains(AMBARI_AGENT.getName());
                }
            }).get().getName();
            List<String> hostBlackList = getOtherHostgroupsAgentHostsFromContainer(existingContainers, hostGroupName);
            ContainerConstraint ambariAgentConstraint = constraintFactory.getAmbariAgentConstraint(ambariServerHost, ambariAgentApp,
                    cloudPlatform, hostGroup, adjustment, hostBlackList);
            containers.put(hostGroup.getName(), containerOrchestrator.runContainer(containerConfigService.get(stack, AMBARI_AGENT), credential,
                    ambariAgentConstraint, clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId())));

            if (SWARM.equals(orchestrator.getType())) {
                List<String> hosts = ambariAgentConstraint.getHosts();

                ContainerConstraint consulWatchConstraint = constraintFactory.getConsulWatchConstraint(hosts);
                containers.put(CONSUL_WATCH.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, CONSUL_WATCH), credential,
                        consulWatchConstraint, clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId())));

                ContainerConstraint logrotateConstraint = constraintFactory.getLogrotateConstraint(hosts);
                containers.put(LOGROTATE.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, LOGROTATE), credential,
                        logrotateConstraint, clusterDeletionBasedExitCriteriaModel(stack.getId(), cluster.getId())));
            }

            return saveContainers(containers, cluster);
        } catch (CloudbreakOrchestratorException ex) {
            if (!containers.isEmpty()) {
                saveContainers(containers, cluster);
            }
            checkCancellation(ex);
            throw ex;
        }
    }

    private List<String> getOtherHostgroupsAgentHostsFromContainer(Set<Container> existingContainers, String hostGroupName) {
        final String hostGroupNamePart = hostGroupName.replace("_", "-");
        return FluentIterable.from(existingContainers).filter(new Predicate<Container>() {
            @Override
            public boolean apply(@Nullable Container input) {
                return input.getImage().contains(AMBARI_AGENT.getName()) && !input.getName().contains(hostGroupNamePart);
            }
        }).transform(new Function<Container, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Container input) {
                return input.getHost();
            }
        }).toList();
    }

    private List<String> getHostsFromContainerInfo(List<ContainerInfo> containerInfos) {
        return FluentIterable.from(containerInfos).transform(new Function<ContainerInfo, String>() {
            @Nullable
            @Override
            public String apply(@Nullable ContainerInfo input) {
                return input.getHost();
            }
        }).toList();
    }

    private List<String> getHosts(Stack stack) {
        List<String> hosts = new ArrayList<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            hosts.add(instanceMetaData.getDiscoveryFQDN());
        }
        return hosts;
    }

    private List<Container> convert(List<ContainerInfo> containerInfo, Cluster cluster) {
        List<Container> containers = new ArrayList<>();
        for (ContainerInfo source : containerInfo) {
            Container container = conversionService.convert(source, Container.class);
            container.setCluster(cluster);
            containers.add(container);
        }
        return containers;
    }

    private Map<String, List<Container>> saveContainers(Map<String, List<ContainerInfo>> containerInfo, Cluster cluster) {
        Map<String, List<Container>> containers = new HashMap<>();
        for (Map.Entry<String, List<ContainerInfo>> containerInfoEntry : containerInfo.entrySet()) {
            List<Container> hostGroupContainers = convert(containerInfoEntry.getValue(), cluster);
            containers.put(containerInfoEntry.getKey(), hostGroupContainers);
            containerService.save(hostGroupContainers);
        }
        return containers;
    }

    private void checkCancellation(CloudbreakOrchestratorException ex) {
        if (ex instanceof CloudbreakOrchestratorCancelledException || ExceptionUtils.getRootCause(ex) instanceof CloudbreakOrchestratorCancelledException) {
            throw new CancellationException("Creation of cluster containers was cancelled.");
        }
    }
}

