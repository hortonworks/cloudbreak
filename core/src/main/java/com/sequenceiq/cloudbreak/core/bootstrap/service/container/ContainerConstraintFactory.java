package com.sequenceiq.cloudbreak.core.bootstrap.service.container;

import static com.sequenceiq.cloudbreak.common.type.OrchestratorConstants.YARN;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_AGENT;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_DB;
import static com.sequenceiq.cloudbreak.orchestrator.container.DockerContainer.AMBARI_SERVER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint.Builder;
import com.sequenceiq.cloudbreak.orchestrator.model.port.TcpPortBinding;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.clusterdefinition.VolumeUtils;

@Component
public class ContainerConstraintFactory {

    private static final String CONTAINER_VOLUME_PATH = "/var/log";

    private static final String HADOOP_MOUNT_DIR = "/hadoopfs";

    private static final String HOST_VOLUME_PATH = VolumeUtils.getLogVolume("logs");

    private static final String HOST_NETWORK_MODE = "host";

    private static final int AMBARI_PORT = 8080;

    private static final String HOSTNAME_SEPARATOR = "|";

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private HostGroupRepository hostGroupRepository;

    public ContainerConstraint getAmbariServerDbConstraint(String gatewayHostname, String clusterName, String identifier) {
        Builder builder = new Builder()
                .withName(createContainerInstanceName(AMBARI_DB.getName(), clusterName, identifier))
                .instances(1)
                .networkMode(HOST_NETWORK_MODE)
                .addVolumeBindings(ImmutableMap.of("/var/lib/ambari-server/pgsql/data", "/var/lib/postgresql/data"))
                .addEnv(ImmutableMap.of("POSTGRES_PASSWORD", "bigdata", "POSTGRES_USER", "ambari"));
        if (gatewayHostname != null) {
            builder.addHosts(ImmutableList.of(gatewayHostname));
        }
        return builder.build();
    }

    public ContainerConstraint getAmbariServerConstraint(String dbHostname, String gatewayHostname, String cloudPlatform,
            String clusterName, String identifier) {
        String env = String.format("/usr/sbin/init systemd.setenv=POSTGRES_DB=%s systemd.setenv=CLOUD_PLATFORM=%s", dbHostname, cloudPlatform);
        Builder builder = new Builder()
                .withName(createContainerInstanceName(AMBARI_SERVER.getName(), clusterName, identifier))
                .instances(1)
                .networkMode(HOST_NETWORK_MODE)
                .tcpPortBinding(new TcpPortBinding(AMBARI_PORT, "0.0.0.0", AMBARI_PORT))
                .addVolumeBindings(ImmutableMap.of("/var/log/ambari-server-container", CONTAINER_VOLUME_PATH, "/etc/krb5.conf", "/etc/krb5.conf"))
                .addEnv(ImmutableMap.of("SERVICE_NAME", "ambari-8080"));
        if (!StringUtils.isEmpty(gatewayHostname)) {
            builder.addHosts(ImmutableList.of(gatewayHostname));
        } else {
            env += " systemd.setenv=USE_CONSUL_DNS=false";
        }
        builder.cmd(new String[]{env});
        return builder.build();
    }

    public ContainerConstraint getAmbariAgentConstraint(String ambariServerHost, String ambariAgentApp, String cloudPlatform,
            HostGroup hostGroup, Integer adjustment, List<String> hostBlackList, String identifier) {
        String containerInstanceName;
        containerInstanceName = YARN.equals(hostGroup.getCluster().getStack().getOrchestrator().getType())
                ? createContainerInstanceNameForYarn(hostGroup, AMBARI_AGENT.getName(), identifier)
                : createContainerInstanceName(hostGroup, AMBARI_AGENT.getName(), identifier);
        Constraint hgConstraint = hostGroup.getConstraint();
        Builder builder = new Builder()
                .withNamePrefix(containerInstanceName)
                .withAppName(ambariAgentApp)
                .networkMode(HOST_NETWORK_MODE);
        if (hgConstraint.getInstanceGroup() != null) {
            InstanceGroup instanceGroup = hgConstraint.getInstanceGroup();
            Map<String, String> dataVolumeBinds = new HashMap<>();
            dataVolumeBinds.put("/var/log/ambari-agent-container", CONTAINER_VOLUME_PATH);
            dataVolumeBinds.put(HADOOP_MOUNT_DIR, HADOOP_MOUNT_DIR);
            dataVolumeBinds.putAll(ImmutableMap.of("/data/jars", "/data/jars", HOST_VOLUME_PATH, CONTAINER_VOLUME_PATH));
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
            builder.constraints(getConstraints(hostBlackList));
            if (adjustment != null) {
                builder.instances(adjustment);
            } else {
                builder.instances(hgConstraint.getHostCount());
            }
            builder.withDiskSize(hgConstraint.getConstraintTemplate().getDisk());
            Map<String, String> dataVolumeBinds = new HashMap<>();
            dataVolumeBinds.put("/var/log/ambari-agent-container", CONTAINER_VOLUME_PATH);
            builder.addVolumeBindings(dataVolumeBinds);
            builder.cmd(new String[]{String.format(
                    "/usr/sbin/init systemd.setenv=AMBARI_SERVER_ADDR=%s systemd.setenv=USE_CONSUL_DNS=false", ambariServerHost)});
        }

        return builder.build();
    }

    private List<List<String>> getConstraints(List<String> hostBlackList) {
        List<List<String>> constraints = new ArrayList<>();
        constraints.add(ImmutableList.of("hostname", "UNIQUE"));
        if (!hostBlackList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hostBlackList.size(); i++) {
                sb.append(hostBlackList.get(i));
                if (i < hostBlackList.size() - 1) {
                    sb.append(HOSTNAME_SEPARATOR);
                }
            }
            constraints.add(ImmutableList.of("hostname", "UNLIKE", sb.toString()));
        }
        return constraints;
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

    private List<String> getHosts(Collection<String> candidateAddresses, InstanceGroup instanceGroup) {
        List<String> hosts = new ArrayList<>();
        for (InstanceMetaData instanceMetaData : instanceMetaDataRepository.findAliveInstancesInInstanceGroup(instanceGroup.getId())) {
            String fqdn = instanceMetaData.getDiscoveryFQDN();
            if (candidateAddresses == null || candidateAddresses.contains(fqdn)) {
                hosts.add(instanceMetaData.getDiscoveryFQDN());
            }
        }
        return hosts;
    }

    private String createContainerInstanceName(HostGroup hostGroup, String containerName, String identifier) {
        String hostGroupName = hostGroup.getName();
        String clusterName = hostGroup.getCluster().getName();
        return createContainerInstanceName(containerName, hostGroupName, clusterName, "");
    }

    private String createContainerInstanceName(String containerName, String clusterName, String identifier) {
        return createContainerInstanceName(containerName, clusterName, "", "");
    }

    private String createContainerInstanceName(String containerName, String clusterName, String hostGroupName, String identifier) {
        String separator = "-";
        return createContainerInstanceNameWithSeparator(containerName, clusterName, hostGroupName, separator, "");
    }

    private String createContainerInstanceNameForYarn(HostGroup hostGroup, String containerName, String identifier) {
        String hostGroupName = hostGroup.getName();
        String clusterName = hostGroup.getCluster().getName();
        return createContainerInstanceNameForYarn(containerName, hostGroupName, clusterName, "");
    }

    private String createContainerInstanceNameForYarn(String containerName, String clusterName, String hostGroupName, String identifier) {
        String separator = "%";
        return createContainerInstanceNameWithSeparator(containerName, clusterName, hostGroupName, separator, "");
    }

    private String createContainerInstanceNameWithSeparator(String containerName, String clusterName, String hostGroupName,
            String separator, String identifier) {
        StringBuilder sb = new StringBuilder(containerName);
        if (!StringUtils.isEmpty(hostGroupName)) {
            sb.append(separator).append(hostGroupName);
        }
        if (!StringUtils.isEmpty(clusterName)) {
            sb.append(separator).append(clusterName);
        }
        if (!StringUtils.isEmpty(identifier)) {
            sb.append(separator).append(identifier);
        }
        return sb.toString();
    }
}
