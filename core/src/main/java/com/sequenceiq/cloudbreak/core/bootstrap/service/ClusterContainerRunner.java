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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.flow.context.ClusterScalingContext;
import com.sequenceiq.cloudbreak.core.flow.context.DefaultFlowContext;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.model.port.TcpPortBinding;
import com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.connector.VolumeUtils;

@Component
public class ClusterContainerRunner {
    private static final String CONTAINER_VOLUME_PATH = "/var/log";
    private static final String HADOOP_MOUNT_DIR = "/hadoopfs";
    private static final String HOST_VOLUME_PATH = VolumeUtils.getLogVolume("logs");
    private static final String HOST_NETWORK_MODE = "host";
    private static final int AMBARI_PORT = 8080;
    private static final int SHIPYARD_CONTAINER_PORT = 8080;
    private static final int SHIPYARD_EXPOSED_PORT = 7070;
    private static final int SHIPYARD_DB_CONTAINER_PORT = 8080;
    private static final int SHIPYARD_DB_EXPOSED_PORT = 7071;
    private static final int LDAP_PORT = 389;
    private static final int REGISTRATOR_RESYNC_SECONDS = 60;

    @Value("#{'${cb.docker.env.ldap}'.split('\\|')}")
    private List<String> ldapEnvs;

    @Value("${cb.docker.env.shipyard.enabled:}")
    private Boolean shipyardEnabled;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ContainerConfigService containerConfigService;

    @Inject
    private ContainerOrchestratorResolver containerOrchestratorResolver;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public void runClusterContainers(ProvisioningContext context) throws CloudbreakException {
        try {
            initializeClusterContainers(context, false, Collections.<String>emptySet());
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    public void addClusterContainers(ClusterScalingContext context) throws CloudbreakException {
        try {
            initializeClusterContainers(context, true, context.getUpscaleCandidateAddresses());
        } catch (CloudbreakOrchestratorCancelledException e) {
            throw new CancellationException(e.getMessage());
        } catch (CloudbreakOrchestratorException e) {
            throw new CloudbreakException(e);
        }
    }

    private void initializeClusterContainers(DefaultFlowContext context, Boolean add, Set<String> candidateAddresses) throws CloudbreakException,
            CloudbreakOrchestratorException {
        ContainerOrchestrator containerOrchestrator = containerOrchestratorResolver.get();
        String cloudPlatform = context.getCloudPlatform().value();
        Stack stack = stackRepository.findOneWithLists(context.getStackId());
        InstanceGroup gateway = stack.getGatewayInstanceGroup();
        InstanceMetaData gatewayInstance = gateway.getInstanceMetaData().iterator().next();
        GatewayConfig gatewayConfig = tlsSecurityService.buildGatewayConfig(stack.getId(),
                gatewayInstance.getPublicIp(), gatewayInstance.getPrivateIp());
        Set<Node> nodes = getNodes(add, stack, candidateAddresses);
        OrchestrationCredential credential = new OrchestrationCredential(gatewayConfig.getPublicAddress(), gatewayConfig.getPrivateAddress(),
                gatewayConfig.getCertificateDir());

        if (!add) {
            Cluster cluster = clusterService.retrieveClusterByStackId(stack.getId());
            Node gatewayNode = getGatewayNode(gatewayConfig.getPublicAddress(), nodes);

            ContainerConstraint registratorConstraint = getRegistratorConstraint(gatewayNode);
            containerOrchestrator.runContainer(containerConfigService.get(stack, REGISTRATOR), credential, registratorConstraint,
                    stackDeletionBasedExitCriteriaModel(stack.getId()));

            ContainerConstraint ambariServerDbConstraint = getAmbariServerDbConstraint(gatewayNode);
            containerOrchestrator.runContainer(containerConfigService.get(stack, AMBARI_DB), credential, ambariServerDbConstraint,
                    stackDeletionBasedExitCriteriaModel(stack.getId()));

            ContainerConstraint ambariServerConstraint = getAmbariServerConstraint(cloudPlatform, gatewayNode);
            containerOrchestrator.runContainer(containerConfigService.get(stack, AMBARI_SERVER), credential, ambariServerConstraint,
                    stackDeletionBasedExitCriteriaModel(stack.getId()));

            if (cluster.isSecure()) {
                ContainerConstraint havegedConstraint = getHavegedConstraint(gatewayNode);
                containerOrchestrator.runContainer(containerConfigService.get(stack, HAVEGED), credential, havegedConstraint,
                        stackDeletionBasedExitCriteriaModel(stack.getId()));

                ContainerConstraint kerberosServerConstraint = getKerberosServerConstraint(cluster, gatewayNode);
                containerOrchestrator.runContainer(containerConfigService.get(stack, KERBEROS), credential, kerberosServerConstraint,
                        stackDeletionBasedExitCriteriaModel(stack.getId()));
            }

            if (cluster.isLdapRequired()) {
                ContainerConstraint ldapConstraint = getLdapConstraint(gatewayNode);
                containerOrchestrator.runContainer(containerConfigService.get(stack, LDAP), credential, ldapConstraint,
                        stackDeletionBasedExitCriteriaModel(stack.getId()));
            }

            if (shipyardEnabled) {
                ContainerConstraint shipyardDbConstraint = getShipyardDbConstraint(gatewayNode);
                containerOrchestrator.runContainer(containerConfigService.get(stack, SHIPYARD_DB), credential, shipyardDbConstraint,
                        stackDeletionBasedExitCriteriaModel(stack.getId()));

                ContainerConstraint shipyardConstraint = getShipyardConstraint(cloudPlatform, gatewayNode);
                containerOrchestrator.runContainer(containerConfigService.get(stack, SHIPYARD), credential, shipyardConstraint,
                        stackDeletionBasedExitCriteriaModel(stack.getId()));
            }
        }

        runAmbariAgentContainers(add, candidateAddresses, containerOrchestrator, cloudPlatform, stack, credential);

        Map<String, String> privateIpsByHostname = getPrivateIpsByHostname(nodes);
        ContainerConstraint consulWatchConstraint = getConsulWatchConstraint(privateIpsByHostname);
        containerOrchestrator.runContainer(containerConfigService.get(stack, CONSUL_WATCH), credential, consulWatchConstraint,
                stackDeletionBasedExitCriteriaModel(stack.getId()));

        ContainerConstraint logrotateConstraint = getLogrotateConstraint(privateIpsByHostname);
        containerOrchestrator.runContainer(containerConfigService.get(stack, LOGROTATE), credential, logrotateConstraint,
                stackDeletionBasedExitCriteriaModel(stack.getId()));
    }

    private Node getGatewayNode(String gatewayPublicAddress, Collection<Node> nodes) {
        for (Node node : nodes) {
            if (node.getPublicIp() != null && node.getPublicIp().equals(gatewayPublicAddress)) {
                return node;
            }
        }
        throw new RuntimeException("Gateway not found in cluster");
    }

    private ContainerConstraint getRegistratorConstraint(Node gatewayNode) {
        return new ContainerConstraint.Builder()
                .withName(REGISTRATOR.getName())
                .networkMode(HOST_NETWORK_MODE)
                .addVolumeBindings(ImmutableMap.of("/var/run/docker.sock", "/tmp/docker.sock"))
                .addPrivateIpsByHostname(ImmutableMap.of(gatewayNode.getHostname(), gatewayNode.getPrivateIp()))
                .cmd(new String[]{"-ip", gatewayNode.getPrivateIp(), "-resync",
                        Integer.toString(REGISTRATOR_RESYNC_SECONDS), String.format("consul://%s:8500", gatewayNode.getPrivateIp())})
                .build();
    }

    private ContainerConstraint getAmbariServerDbConstraint(Node gatewayNode) {
        return new ContainerConstraint.Builder()
                .withName(AMBARI_DB.getName())
                .networkMode(HOST_NETWORK_MODE)
                .addVolumeBindings(ImmutableMap.of("/data/ambari-server/pgsql/data", "/var/lib/postgresql/data",
                        HOST_VOLUME_PATH + "/consul-watch", HOST_VOLUME_PATH + "/consul-watch"))
                .addPrivateIpsByHostname(ImmutableMap.of(gatewayNode.getHostname(), gatewayNode.getPrivateIp()))
                .addEnv(Arrays.asList(String.format("POSTGRES_PASSWORD=%s", "bigdata"), String.format("POSTGRES_USER=%s", "ambari")))
                .build();
    }

    private ContainerConstraint getAmbariServerConstraint(String cloudPlatform, Node gatewayNode) {
        return new ContainerConstraint.Builder()
                .withName(AMBARI_SERVER.getName())
                .networkMode(HOST_NETWORK_MODE)
                .tcpPortBinding(new TcpPortBinding(AMBARI_PORT, "0.0.0.0", AMBARI_PORT))
                .addVolumeBindings(ImmutableMap.of(HOST_VOLUME_PATH, CONTAINER_VOLUME_PATH, "/etc/krb5.conf", "/etc/krb5.conf"))
                .addPrivateIpsByHostname(ImmutableMap.of(gatewayNode.getHostname(), gatewayNode.getPrivateIp()))
                .addEnv(Arrays.asList("SERVICE_NAME=ambari-8080"))
                .cmd(new String[]{String.format("systemd.setenv=POSTGRES_DB=localhost systemd.setenv=CLOUD_PLATFORM=%s", cloudPlatform)})
                .build();
    }

    private ContainerConstraint getHavegedConstraint(Node gatewayNode) {
        return new ContainerConstraint.Builder()
                .withNamePrefix(HAVEGED.getName())
                .addPrivateIpsByHostname(ImmutableMap.of(gatewayNode.getHostname(), gatewayNode.getPrivateIp()))
                .build();
    }

    private ContainerConstraint getLdapConstraint(Node gatewayNode) {
        return new ContainerConstraint.Builder()
                .withNamePrefix(LDAP.getName())
                .networkMode(HOST_NETWORK_MODE)
                .tcpPortBinding(new TcpPortBinding(LDAP_PORT, "0.0.0.0", LDAP_PORT))
                .addPrivateIpsByHostname(ImmutableMap.of(gatewayNode.getHostname(), gatewayNode.getPrivateIp()))
                .addEnv(Arrays.asList(String.format("constraint:node==%s", gatewayNode.getHostname()),
                        String.format("SERVICE_NAME=%s", LDAP.getName()),
                        "NAMESERVER_IP=127.0.0.1"))
                .addEnv(ldapEnvs)
                .build();
    }

    private ContainerConstraint getKerberosServerConstraint(Cluster cluster, Node gatewayNode) {
        KerberosConfiguration kerberosConf = new KerberosConfiguration(cluster.getKerberosMasterKey(), cluster.getKerberosAdmin(),
                cluster.getKerberosPassword());
        return new ContainerConstraint.Builder()
                .withNamePrefix(KERBEROS.getName())
                .networkMode(HOST_NETWORK_MODE)
                .addVolumeBindings(ImmutableMap.of(HOST_VOLUME_PATH, CONTAINER_VOLUME_PATH, "/etc/krb5.conf", "/etc/krb5.conf"))
                .addPrivateIpsByHostname(ImmutableMap.of(gatewayNode.getHostname(), gatewayNode.getPrivateIp()))
                .addEnv(Arrays.asList(String.format("constraint:node==%s", gatewayNode.getHostname()),
                        String.format("SERVICE_NAME=%s", KERBEROS.getName()),
                        "NAMESERVER_IP=127.0.0.1",
                        String.format("REALM=%s", REALM),
                        String.format("DOMAIN_REALM=%s", DOMAIN_REALM),
                        String.format("KERB_MASTER_KEY=%s", kerberosConf.getMasterKey()),
                        String.format("KERB_ADMIN_USER=%s", kerberosConf.getUser()),
                        String.format("KERB_ADMIN_PASS=%s", kerberosConf.getPassword())))
                .build();
    }

    private ContainerConstraint getShipyardConstraint(String cloudPlatform, Node gatewayNode) {
        return new ContainerConstraint.Builder()
                .withName(SHIPYARD.getName())
                .tcpPortBinding(new TcpPortBinding(SHIPYARD_CONTAINER_PORT, "0.0.0.0", SHIPYARD_EXPOSED_PORT))
                .addPrivateIpsByHostname(ImmutableMap.of(gatewayNode.getHostname(), gatewayNode.getPrivateIp()))
                .addEnv(Arrays.asList(String.format("SERVICE_NAME=%s", SHIPYARD.getName())))
                .addLink("swarm-manager", "swarm")
                .addLink(SHIPYARD_DB.getName(), "rethinkdb")
                .cmd(new String[]{"server", "-d", "tcp://swarm:3376"})
                .build();
    }

    private ContainerConstraint getShipyardDbConstraint(Node gatewayNode) {
        return new ContainerConstraint.Builder()
                .withName(SHIPYARD_DB.getName())
                .tcpPortBinding(new TcpPortBinding(SHIPYARD_DB_CONTAINER_PORT, "0.0.0.0", SHIPYARD_DB_EXPOSED_PORT))
                .addPrivateIpsByHostname(ImmutableMap.of(gatewayNode.getHostname(), gatewayNode.getPrivateIp()))
                .addEnv(Arrays.asList(String.format("SERVICE_NAME=%s", SHIPYARD_DB.getName())))
                .build();
    }

    private void runAmbariAgentContainers(Boolean add, Set<String> candidateAddresses, ContainerOrchestrator orchestrator, String cloudPlatform, Stack stack,
            OrchestrationCredential cred) throws CloudbreakOrchestratorException {
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            Map<String, String> dataVolumeBinds = new HashMap<>();
            dataVolumeBinds.put(HADOOP_MOUNT_DIR, HADOOP_MOUNT_DIR);
            dataVolumeBinds.putAll(ImmutableMap.of("/data/jars", "/data/jars", HOST_VOLUME_PATH, CONTAINER_VOLUME_PATH));
            Map<String, String> privateIpsByHostname = getPrivateIpsByHostname(add, candidateAddresses, cred, instanceGroup);
            ContainerConstraint ambariAgentConstraint = getAmbariAgentConstraint(cloudPlatform, dataVolumeBinds, privateIpsByHostname);
            orchestrator.runContainer(containerConfigService.get(stack, AMBARI_AGENT), cred, ambariAgentConstraint,
                    stackDeletionBasedExitCriteriaModel(stack.getId()));
        }
    }

    private ContainerConstraint getAmbariAgentConstraint(String cloudPlatform, Map<String, String> dataVolumeBinds, Map<String, String> privateIpsByHostname) {
        return new ContainerConstraint.Builder()
                .withNamePrefix(AMBARI_AGENT.getName())
                .networkMode(HOST_NETWORK_MODE)
                .addVolumeBindings(dataVolumeBinds)
                .addPrivateIpsByHostname(privateIpsByHostname)
                .cmd(new String[]{String.format("systemd.setenv=CLOUD_PLATFORM=%s", cloudPlatform)})
                .build();
    }

    private ContainerConstraint getConsulWatchConstraint(Map<String, String> privateIpsByHostname) {
        return new ContainerConstraint.Builder()
                .withNamePrefix(CONSUL_WATCH.getName())
                .networkMode(HOST_NETWORK_MODE)
                .addVolumeBindings(ImmutableMap.of("/var/run/docker.sock", "/var/run/docker.sock"))
                .addPrivateIpsByHostname(privateIpsByHostname)
                .cmd(new String[]{String.format("consul://127.0.0.1:8500")})
                .build();
    }

    private ContainerConstraint getLogrotateConstraint(Map<String, String> privateIpsByHostname) {
        return new ContainerConstraint.Builder()
                .withNamePrefix(LOGROTATE.getName())
                .networkMode(HOST_NETWORK_MODE)
                .addVolumeBindings(ImmutableMap.of("/var/lib/docker/containers", "/var/lib/docker/containers"))
                .addPrivateIpsByHostname(privateIpsByHostname)
                .build();
    }

    private Set<Node> getNodes(Boolean add, Stack stack, Set<String> candidateAddresses) {
        Set<Node> nodes = new HashSet<>();
        for (InstanceMetaData instanceMetaData : stack.getRunningInstanceMetaData()) {
            if (!add || candidateAddresses.contains(instanceMetaData.getPrivateIp())) {
                int volumeCount = instanceMetaData.getInstanceGroup().getTemplate().getVolumeCount();
                Set<String> dataVolumes = new HashSet<>();
                for (int i = 1; i <= volumeCount; i++) {
                    dataVolumes.add(VolumeUtils.VOLUME_PREFIX + i);
                }
                nodes.add(new Node(instanceMetaData.getPrivateIp(), instanceMetaData.getPublicIp(), instanceMetaData.getDiscoveryName(), dataVolumes));
            }
        }
        return nodes;
    }

    private Map<String, String> getPrivateIpsByHostname(Boolean add, Set<String> candidateAddresses, OrchestrationCredential credential,
            InstanceGroup instanceGroup) {
        Map<String, String> privateIpsByHostname = new HashMap<>();
        for (InstanceMetaData instanceMetaData : instanceGroup.getInstanceMetaData()) {
            String privateIp = instanceMetaData.getPrivateIp();
            if (!privateIp.equals(credential.getPrivateApiAddress()) && (!add || candidateAddresses.contains(privateIp))) {
                privateIpsByHostname.put(instanceMetaData.getDiscoveryName(), instanceMetaData.getPrivateIp());
            }
        }
        return privateIpsByHostname;
    }

    private Map<String, String> getPrivateIpsByHostname(Set<Node> nodes) {
        Map<String, String> privateIpsByHostname = new HashMap<>();
        for (Node node : nodes) {
            privateIpsByHostname.put(node.getHostname().trim(), node.getPrivateIp());
        }
        return privateIpsByHostname;
    }
}
