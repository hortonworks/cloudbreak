package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.core.bootstrap.service.StackDeletionBasedExitCriteriaModel.stackDeletionBasedExitCriteriaModel;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.AMBARI_AGENT;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.AMBARI_DB;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.AMBARI_SERVER;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.CONSUL_WATCH;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.HAVEGED;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.KERBEROS;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.LDAP;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.LOGROTATE;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.REGISTRATOR;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.SHIPYARD;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.SHIPYARD_DB;
import static com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration.DOMAIN_REALM;
import static com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration.REALM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.DefaultFlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.model.port.TcpPortBinding;
import com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration;
import com.sequenceiq.cloudbreak.repository.ContainerRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ContainerService;
import com.sequenceiq.cloudbreak.service.stack.connector.VolumeUtils;

@Component
public class ClusterContainerRunner {
    private static final String CONTAINER_VOLUME_PATH = "/var/log";
    private static final String HOST_VOLUME_PATH = VolumeUtils.getLogVolume("logs");
    private static final String HOST_NETWORK_MODE = "host";
    private static final int AMBARI_PORT = 8080;
    private static final int SHIPYARD_CONTAINER_PORT = 8080;
    private static final int SHIPYARD_EXPOSED_PORT = 7070;
    private static final int SHIPYARD_DB_CONTAINER_PORT = 8080;
    private static final int SHIPYARD_DB_EXPOSED_PORT = 7071;
    private static final int LDAP_PORT = 389;
    private static final int REGISTRATOR_RESYNC_SECONDS = 60;
    private static final String NONE = "none";

    @Value("#{'${cb.docker.env.ldap}'.split('\\|')}")
    private List<String> ldapEnvs;

    @Value("${cb.docker.env.shipyard.enabled:}")
    private Boolean shipyardEnabled;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private ContainerConfigService containerConfigService;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Inject
    private ContainerService containerService;

    @Inject
    private ContainerRepository containerRepository;

    @Inject
    private ConversionService conversionService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public Map<String, List<Container>> runClusterContainers(ProvisioningContext context) throws CloudbreakException {
        try {
            Stack stack = stackRepository.findOneWithLists(context.getStackId());
            return initializeClusterContainers(stack, cloudPlatform(context));
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    public Map<String, List<Container>> addClusterContainers(ClusterScalingContext context) throws CloudbreakException {
        try {
            Stack stack = stackRepository.findOneWithLists(context.getStackId());
            return addClusterContainers(
                    stack,
                    cloudPlatform(context),
                    context.getHostGroupAdjustment().getHostGroup(),
                    context.getHostGroupAdjustment().getScalingAdjustment());
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    private String cloudPlatform(DefaultFlowContext context) {
        String cloudPlatform = NONE;
        if (context.getCloudPlatform() != null) {
            cloudPlatform = context.getCloudPlatform().value();
        }
        return cloudPlatform;
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
            if ("SWARM".equals(orchestrator.getType())) {
                ContainerConstraint registratorConstraint = getRegistratorConstraint(gatewayHostname, cluster.getName());
                containers.put(REGISTRATOR.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, REGISTRATOR), credential,
                        registratorConstraint, stackDeletionBasedExitCriteriaModel(stack.getId())));
            }

            ContainerConstraint ambariServerDbConstraint = getAmbariServerDbConstraint(gatewayHostname, cluster.getName());
            List<ContainerInfo> dbContainer = containerOrchestrator.runContainer(containerConfigService.get(stack, AMBARI_DB), credential,
                    ambariServerDbConstraint, stackDeletionBasedExitCriteriaModel(stack.getId()));
            containers.put(AMBARI_DB.name(), dbContainer);

            String serverDbHostName = dbContainer.get(0).getHost();
            ContainerConstraint ambariServerConstraint = getAmbariServerConstraint(serverDbHostName, gatewayHostname, cloudPlatform, cluster.getName());
            List<ContainerInfo> ambariServerContainer = containerOrchestrator.runContainer(containerConfigService.get(stack, AMBARI_SERVER),
                    credential, ambariServerConstraint, stackDeletionBasedExitCriteriaModel(stack.getId()));
            containers.put(AMBARI_SERVER.name(), ambariServerContainer);
            String ambariServerHost = ambariServerContainer.get(0).getHost();

            if (cluster.isSecure()) {
                ContainerConstraint havegedConstraint = getHavegedConstraint(gatewayHostname, cluster.getName());
                containers.put(HAVEGED.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, HAVEGED), credential, havegedConstraint,
                        stackDeletionBasedExitCriteriaModel(stack.getId())));

                ContainerConstraint kerberosServerConstraint = getKerberosServerConstraint(cluster, gatewayHostname);
                containers.put(KERBEROS.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, KERBEROS), credential,
                        kerberosServerConstraint, stackDeletionBasedExitCriteriaModel(stack.getId())));
            }

            if (cluster.isLdapRequired()) {
                ContainerConstraint ldapConstraint = getLdapConstraint(ambariServerHost);
                containers.put(LDAP.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, LDAP), credential, ldapConstraint,
                        stackDeletionBasedExitCriteriaModel(stack.getId())));
            }

            if ("SWARM".equals(orchestrator.getType()) && shipyardEnabled) {
                ContainerConstraint shipyardDbConstraint = getShipyardDbConstraint(ambariServerHost);
                containers.put(SHIPYARD_DB.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, SHIPYARD_DB), credential,
                        shipyardDbConstraint, stackDeletionBasedExitCriteriaModel(stack.getId())));

                ContainerConstraint shipyardConstraint = getShipyardConstraint(ambariServerHost);
                containers.put(SHIPYARD.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, SHIPYARD), credential, shipyardConstraint,
                        stackDeletionBasedExitCriteriaModel(stack.getId())));
            }

            for (HostGroup hostGroup : hostGroupRepository.findHostGroupsInCluster(stack.getCluster().getId())) {
                ContainerConstraint ambariAgentConstraint = getAmbariAgentConstraint(ambariServerHost, null, cloudPlatform, hostGroup, null);
                containers.put(hostGroup.getName(), containerOrchestrator.runContainer(containerConfigService.get(stack, AMBARI_AGENT), credential,
                        ambariAgentConstraint, stackDeletionBasedExitCriteriaModel(stack.getId())));
            }

            if ("SWARM".equals(orchestrator.getType())) {
                List<String> hosts = getHosts(stack);
                ContainerConstraint consulWatchConstraint = getConsulWatchConstraint(hosts);
                containers.put(CONSUL_WATCH.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, CONSUL_WATCH), credential,
                        consulWatchConstraint, stackDeletionBasedExitCriteriaModel(stack.getId())));

                ContainerConstraint logrotateConstraint = getLogrotateConstraint(hosts);
                containers.put(LOGROTATE.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, LOGROTATE), credential,
                        logrotateConstraint, stackDeletionBasedExitCriteriaModel(stack.getId())));
            }
            return saveContainers(containers, cluster);
        } catch (CloudbreakOrchestratorException ex) {
            if (!containers.isEmpty()) {
                saveContainers(containers, cluster);
            }
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
            Set<Container> existingContainers = containerRepository.findContainersInCluster(cluster.getId());
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
            ContainerConstraint ambariAgentConstraint = getAmbariAgentConstraint(ambariServerHost, ambariAgentApp, cloudPlatform, hostGroup, adjustment);
            containers.put(hostGroup.getName(), containerOrchestrator.runContainer(containerConfigService.get(stack, AMBARI_AGENT), credential,
                    ambariAgentConstraint, stackDeletionBasedExitCriteriaModel(stack.getId())));

            if ("SWARM".equals(orchestrator.getType())) {
                List<String> hosts = collectUpscaleCandidates(cluster.getId(), hostGroupName, adjustment);

                ContainerConstraint consulWatchConstraint = getConsulWatchConstraint(hosts);
                containers.put(CONSUL_WATCH.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, CONSUL_WATCH), credential,
                        consulWatchConstraint, stackDeletionBasedExitCriteriaModel(stack.getId())));

                ContainerConstraint logrotateConstraint = getLogrotateConstraint(hosts);
                containers.put(LOGROTATE.name(), containerOrchestrator.runContainer(containerConfigService.get(stack, LOGROTATE), credential,
                        logrotateConstraint, stackDeletionBasedExitCriteriaModel(stack.getId())));
            }

            return saveContainers(containers, cluster);
        } catch (CloudbreakOrchestratorException ex) {
            if (!containers.isEmpty()) {
                saveContainers(containers, cluster);
            }
            throw ex;
        }
    }

    private ContainerConstraint getRegistratorConstraint(String gatewayHostname, String clusterName) {
        return new ContainerConstraint.Builder()
                .withName(createContainerInstanceName(REGISTRATOR.getName(), clusterName))
                .networkMode(HOST_NETWORK_MODE)
                .instances(1)
                .addVolumeBindings(ImmutableMap.of("/var/run/docker.sock", "/tmp/docker.sock"))
                .addHosts(ImmutableList.of(gatewayHostname))
                //TODO rewrite registrator cmd
                //sequenceiq/cloudbreak/blob/master/core/src/main/java/com/sequenceiq/cloudbreak/core/bootstrap/service/ClusterContainerRunner.java#L193
                .cmd(new String[]{"-resync", Integer.toString(REGISTRATOR_RESYNC_SECONDS), "consul://127.0.0.1:8500"})
                .build();
    }

    private ContainerConstraint getAmbariServerDbConstraint(String gatewayHostname, String clusterName) {
        ContainerConstraint.Builder builder = new ContainerConstraint.Builder()
                .withName(createContainerInstanceName(AMBARI_DB.getName(), clusterName))
                .instances(1)
                .networkMode(HOST_NETWORK_MODE)
                .addVolumeBindings(ImmutableMap.of("/data/ambari-server/pgsql/data", "/var/lib/postgresql/data",
                        HOST_VOLUME_PATH + "/consul-watch", HOST_VOLUME_PATH + "/consul-watch"))
                .addEnv(ImmutableMap.of("POSTGRES_PASSWORD", "bigdata", "POSTGRES_USER", "ambari"));
        if (gatewayHostname != null) {
            builder.addHosts(ImmutableList.of(gatewayHostname));
        }
        return builder.build();
    }

    private ContainerConstraint getAmbariServerConstraint(String dbHostname, String gatewayHostname, String cloudPlatform, String clusterName) {
        ContainerConstraint.Builder builder = new ContainerConstraint.Builder()
                .withName(createContainerInstanceName(AMBARI_SERVER.getName(), clusterName))
                .instances(1)
                .networkMode(HOST_NETWORK_MODE)
                .tcpPortBinding(new TcpPortBinding(AMBARI_PORT, "0.0.0.0", AMBARI_PORT))
                .addVolumeBindings(ImmutableMap.of(HOST_VOLUME_PATH, CONTAINER_VOLUME_PATH, "/etc/krb5.conf", "/etc/krb5.conf"))
                .addEnv(ImmutableMap.of("SERVICE_NAME", "ambari-8080"))
                .cmd(new String[]{String.format("/usr/sbin/init systemd.setenv=POSTGRES_DB=%s systemd.setenv=CLOUD_PLATFORM=%s", dbHostname, cloudPlatform)});
        if (gatewayHostname != null) {
            builder.addHosts(ImmutableList.of(gatewayHostname));
        }
        return builder.build();
    }

    private ContainerConstraint getHavegedConstraint(String gatewayHostname, String clusterName) {
        return new ContainerConstraint.Builder()
                .withNamePrefix(createContainerInstanceName(HAVEGED.getName(), clusterName))
                .instances(1)
                .addHosts(ImmutableList.of(gatewayHostname))
                .build();
    }

    private ContainerConstraint getLdapConstraint(String gatewayHostname) {
        Map<String, String> env = new HashMap<>();
        env.put("SERVICE_NAME", LDAP.getName());
        env.put("NAMESERVER_IP", "127.0.0.1");
        for (String ldapEnv : ldapEnvs) {
            String[] envValue = ldapEnv.split("=");
            if (envValue.length == 2) {
                env.put(envValue[0], envValue[1]);
            } else {
                throw new RuntimeException(String.format("Could not be parse LDAP parameter from value: '%s'!", ldapEnv));
            }
        }

        return new ContainerConstraint.Builder()
                .withNamePrefix(LDAP.getName())
                .instances(1)
                .networkMode(HOST_NETWORK_MODE)
                .tcpPortBinding(new TcpPortBinding(LDAP_PORT, "0.0.0.0", LDAP_PORT))
                .addHosts(ImmutableList.of(gatewayHostname))
                .addEnv(env)
                .build();
    }

    private ContainerConstraint getKerberosServerConstraint(Cluster cluster, String gatewayHostname) {
        KerberosConfiguration kerberosConf = new KerberosConfiguration(cluster.getKerberosMasterKey(), cluster.getKerberosAdmin(),
                cluster.getKerberosPassword());

        Map<String, String> env = new HashMap<>();
        env.put("SERVICE_NAME", KERBEROS.getName());
        env.put("NAMESERVER_IP", "127.0.0.1");
        env.put("REALM", REALM);
        env.put("DOMAIN_REALM", DOMAIN_REALM);
        env.put("KERB_MASTER_KEY", kerberosConf.getMasterKey());
        env.put("KERB_ADMIN_USER", kerberosConf.getUser());
        env.put("KERB_ADMIN_PASS", kerberosConf.getPassword());

        return new ContainerConstraint.Builder()
                .withName(createContainerInstanceName(KERBEROS.getName(), cluster.getName()))
                .instances(1)
                .networkMode(HOST_NETWORK_MODE)
                .addVolumeBindings(ImmutableMap.of(HOST_VOLUME_PATH, CONTAINER_VOLUME_PATH, "/etc/krb5.conf", "/etc/krb5.conf"))
                .addHosts(ImmutableList.of(gatewayHostname))
                .addEnv(env)
                .build();
    }

    private ContainerConstraint getShipyardDbConstraint(String gatewayHostname) {
        return new ContainerConstraint.Builder()
                .withName(SHIPYARD_DB.getName())
                .instances(1)
                .tcpPortBinding(new TcpPortBinding(SHIPYARD_DB_CONTAINER_PORT, "0.0.0.0", SHIPYARD_DB_EXPOSED_PORT))
                .addHosts(ImmutableList.of(gatewayHostname))
                .addEnv(ImmutableMap.of("SERVICE_NAME", SHIPYARD_DB.getName()))
                .build();
    }

    private ContainerConstraint getShipyardConstraint(String gatewayHostname) {
        return new ContainerConstraint.Builder()
                .withName(SHIPYARD.getName())
                .instances(1)
                .tcpPortBinding(new TcpPortBinding(SHIPYARD_CONTAINER_PORT, "0.0.0.0", SHIPYARD_EXPOSED_PORT))
                .addHosts(ImmutableList.of(gatewayHostname))
                .addEnv(ImmutableMap.of("SERVICE_NAME", SHIPYARD.getName()))
                .addLink("swarm-manager", "swarm")
                .addLink(SHIPYARD_DB.getName(), "rethinkdb")
                .cmd(new String[]{"server", "-d", "tcp://swarm:3376"})
                .build();
    }

    private Map<String, String> getDataVolumeBinds(long volumeCount) {
        Map<String, String> dataVolumeBinds = new HashMap<>();
        for (int i = 1; i <= volumeCount; i++) {
            String dataVolumePath = VolumeUtils.VOLUME_PREFIX + i;
            dataVolumeBinds.put(dataVolumePath, dataVolumePath);
        }
        return dataVolumeBinds;
    }

    private ContainerConstraint getAmbariAgentConstraint(String ambariServerHost, String ambariAgentApp, String cloudPlatform,
            HostGroup hostGroup, Integer adjustment) {
        Constraint hgConstraint = hostGroup.getConstraint();
        ContainerConstraint.Builder builder = new ContainerConstraint.Builder()
                .withNamePrefix(createContainerInstanceName(hostGroup, AMBARI_AGENT.getName()))
                .withAppName(ambariAgentApp)
                .networkMode(HOST_NETWORK_MODE);
        if (hgConstraint.getInstanceGroup() != null) {
            InstanceGroup instanceGroup = hgConstraint.getInstanceGroup();
            int volumeCount = instanceGroup.getTemplate().getVolumeCount();
            Map<String, String> dataVolumeBinds = getDataVolumeBinds(volumeCount);
            ImmutableMap<String, String> volumeBinds = ImmutableMap.of("/data/jars", "/data/jars", HOST_VOLUME_PATH, CONTAINER_VOLUME_PATH);
            dataVolumeBinds.putAll(volumeBinds);

            builder.addVolumeBindings(dataVolumeBinds);
            if (adjustment != null) {
                List<String> candidates = collectUpscaleCandidates(hostGroup.getCluster().getId(), hostGroup.getName(), adjustment);
                builder.addHosts(getHosts(candidates, instanceGroup));
            } else {
                builder.addHosts(getHosts(null, instanceGroup));
            }
            builder.cmd(new String[]{String.format(
                    "/usr/sbin/init systemd.setenv=AMBARI_SERVER_ADDR=%s systemd.setenv=CLOUD_PLATFORM=%s", ambariServerHost, cloudPlatform)});
        }

        if (hgConstraint.getConstraintTemplate() != null) {
            builder.cpus(hgConstraint.getConstraintTemplate().getCpu());
            builder.memory(hgConstraint.getConstraintTemplate().getMemory());
            if (adjustment != null) {
                builder.instances(adjustment);
            } else {
                builder.instances(hgConstraint.getHostCount());
            }
            builder.withDiskSize(hgConstraint.getConstraintTemplate().getDisk());
            builder.cmd(new String[]{String.format(
                    "/usr/sbin/init systemd.setenv=AMBARI_SERVER_ADDR=%s systemd.setenv=USE_CONSUL_DNS=false", ambariServerHost)});
        }

        return builder.build();
    }

    private ContainerConstraint getConsulWatchConstraint(List<String> hosts) {
        return new ContainerConstraint.Builder()
                .withNamePrefix(CONSUL_WATCH.getName())
                .addEnv(ImmutableMap.of("CONSUL_HOST", "127.0.0.1"))
                .networkMode(HOST_NETWORK_MODE)
                .addVolumeBindings(ImmutableMap.of("/var/run/docker.sock", "/var/run/docker.sock"))
                .addHosts(hosts)
                .build();
    }

    private ContainerConstraint getLogrotateConstraint(List<String> hosts) {
        return new ContainerConstraint.Builder()
                .withNamePrefix(LOGROTATE.getName())
                .networkMode(HOST_NETWORK_MODE)
                .addVolumeBindings(ImmutableMap.of("/var/lib/docker/containers", "/var/lib/docker/containers"))
                .addHosts(hosts)
                .build();
    }

    private List<String> getHosts(Stack stack) {
        List<String> hosts = new ArrayList<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            hosts.add(instanceMetaData.getDiscoveryFQDN());
        }
        return hosts;
    }

    private List<String> getHosts(List<String> candidateAddresses, InstanceGroup instanceGroup) {
        List<String> hosts = new ArrayList<>();
        for (InstanceMetaData instanceMetaData : instanceMetaDataRepository.findAliveInstancesInInstanceGroup(instanceGroup.getId())) {
            String fqdn = instanceMetaData.getDiscoveryFQDN();
            if (candidateAddresses == null || candidateAddresses.contains(fqdn)) {
                hosts.add(instanceMetaData.getDiscoveryFQDN());
            }
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

    private String createContainerInstanceName(HostGroup hostGroup, String containerName) {
        String hostGroupName = hostGroup.getName();
        String clusterName = hostGroup.getCluster().getName();
        return createContainerInstanceName(containerName, hostGroupName, clusterName);
    }

    private String createContainerInstanceName(String containerName, String clusterName) {
        return createContainerInstanceName(containerName, clusterName, "");
    }

    private String createContainerInstanceName(String containerName, String clusterName, String hostGroupName) {
        String separator = "-";
        StringBuilder sb = new StringBuilder(containerName);
        if (!StringUtils.isEmpty(hostGroupName)) {
            sb.append(separator).append(hostGroupName);
        }
        if (!StringUtils.isEmpty(clusterName)) {
            sb.append(separator).append(clusterName);
        }
        return sb.toString();
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

    private List<String> collectUpscaleCandidates(Long clusterId, String hostGroupName, Integer adjustment) {
        HostGroup hostGroup = hostGroupRepository.findHostGroupInClusterByName(clusterId, hostGroupName);
        if (hostGroup.getConstraint().getInstanceGroup() != null) {
            Long instanceGroupId = hostGroup.getConstraint().getInstanceGroup().getId();
            Set<InstanceMetaData> unusedHostsInInstanceGroup = instanceMetaDataRepository.findUnusedHostsInInstanceGroup(instanceGroupId);
            List<String> hostNames = new ArrayList<>();
            for (InstanceMetaData instanceMetaData : unusedHostsInInstanceGroup) {
                hostNames.add(instanceMetaData.getDiscoveryFQDN());
                if (hostNames.size() >= adjustment) {
                    break;
                }
            }
            return hostNames;
        }
        return null;
    }
}

