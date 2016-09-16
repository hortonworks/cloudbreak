package com.sequenceiq.cloudbreak.core.bootstrap.service.container;

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
import static com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration.DOMAIN_REALM;
import static com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration.REALM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.port.TcpPortBinding;
import com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.VolumeUtils;

@Component
public class ContainerConstraintFactory {
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
    private static final String HOSTNAME_SEPARATOR = "|";

    @Value("#{'${cb.docker.env.ldap}'.split('\\|')}")
    private List<String> ldapEnvs;

    @Value("#{'${cb.byos.dfs.data.dir}'.split(',')}")
    private List<String> byosDfsDataDirs;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private HostGroupRepository hostGroupRepository;

    public ContainerConstraint getRegistratorConstraint(String gatewayHostname, String clusterName, String gatewayPrivateIp) {
        return new ContainerConstraint.Builder()
                .withName(createContainerInstanceName(REGISTRATOR.getName(), clusterName))
                .networkMode(HOST_NETWORK_MODE)
                .instances(1)
                .addVolumeBindings(ImmutableMap.of("/var/run/docker.sock", "/tmp/docker.sock"))
                .addHosts(ImmutableList.of(gatewayHostname))
                .cmd(new String[]{
                        "-ip", gatewayPrivateIp,
                        "-resync", Integer.toString(REGISTRATOR_RESYNC_SECONDS),
                        String.format("consul://%s:8500", gatewayPrivateIp)})
                .build();
    }

    public ContainerConstraint getAmbariServerDbConstraint(String gatewayHostname, String clusterName) {
        ContainerConstraint.Builder builder = new ContainerConstraint.Builder()
                .withName(createContainerInstanceName(AMBARI_DB.getName(), clusterName))
                .instances(1)
                .networkMode(HOST_NETWORK_MODE)
                .addVolumeBindings(ImmutableMap.of("/var/lib/ambari-server/pgsql/data", "/var/lib/postgresql/data"))
                .addEnv(ImmutableMap.of("POSTGRES_PASSWORD", "bigdata", "POSTGRES_USER", "ambari"));
        if (gatewayHostname != null) {
            builder.addHosts(ImmutableList.of(gatewayHostname));
        }
        return builder.build();
    }

    public ContainerConstraint getAmbariServerConstraint(String dbHostname, String gatewayHostname, String cloudPlatform, String clusterName) {
        String env = String.format("/usr/sbin/init systemd.setenv=POSTGRES_DB=%s systemd.setenv=CLOUD_PLATFORM=%s", dbHostname, cloudPlatform);
        ContainerConstraint.Builder builder = new ContainerConstraint.Builder()
                .withName(createContainerInstanceName(AMBARI_SERVER.getName(), clusterName))
                .instances(1)
                .networkMode(HOST_NETWORK_MODE)
                .tcpPortBinding(new TcpPortBinding(AMBARI_PORT, "0.0.0.0", AMBARI_PORT))
                .addVolumeBindings(ImmutableMap.of("/var/log/ambari-server-container", CONTAINER_VOLUME_PATH, "/etc/krb5.conf", "/etc/krb5.conf"))
                .addEnv(ImmutableMap.of("SERVICE_NAME", "ambari-8080"));
        if (!StringUtils.isEmpty(gatewayHostname)) {
            builder.addHosts(ImmutableList.of(gatewayHostname));
        } else {
            env = env.concat(" systemd.setenv=USE_CONSUL_DNS=false");
        }
        builder.cmd(new String[]{env});
        return builder.build();
    }

    public ContainerConstraint getHavegedConstraint(String gatewayHostname, String clusterName) {
        return new ContainerConstraint.Builder()
                .withNamePrefix(createContainerInstanceName(HAVEGED.getName(), clusterName))
                .instances(1)
                .addHosts(ImmutableList.of(gatewayHostname))
                .build();
    }

    public ContainerConstraint getLdapConstraint(String gatewayHostname) {
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

    public ContainerConstraint getKerberosServerConstraint(Cluster cluster, String gatewayHostname) {
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
                .addVolumeBindings(ImmutableMap.of("/var/log/kerberos-container", CONTAINER_VOLUME_PATH, "/etc/krb5.conf", "/etc/krb5.conf"))
                .addHosts(ImmutableList.of(gatewayHostname))
                .addEnv(env)
                .build();
    }

    public ContainerConstraint getShipyardDbConstraint(String gatewayHostname) {
        return new ContainerConstraint.Builder()
                .withName(SHIPYARD_DB.getName())
                .instances(1)
                .tcpPortBinding(new TcpPortBinding(SHIPYARD_DB_CONTAINER_PORT, "0.0.0.0", SHIPYARD_DB_EXPOSED_PORT))
                .addHosts(ImmutableList.of(gatewayHostname))
                .addEnv(ImmutableMap.of("SERVICE_NAME", SHIPYARD_DB.getName()))
                .build();
    }

    public ContainerConstraint getShipyardConstraint(String gatewayHostname) {
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

    public ContainerConstraint getAmbariAgentConstraint(String ambariServerHost, String ambariAgentApp, String cloudPlatform,
            HostGroup hostGroup, Integer adjustment, List<String> hostBlackList) {
        Constraint hgConstraint = hostGroup.getConstraint();
        ContainerConstraint.Builder builder = new ContainerConstraint.Builder()
                .withNamePrefix(createContainerInstanceName(hostGroup, AMBARI_AGENT.getName()))
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
            if (byosDfsDataDirs != null && !byosDfsDataDirs.isEmpty()) {
                for (String dataDir : byosDfsDataDirs) {
                    if (!"".equals(dataDir)) {
                        dataVolumeBinds.put(dataDir, dataDir);
                    }
                }
            }
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

    public ContainerConstraint getConsulWatchConstraint(List<String> hosts) {
        return new ContainerConstraint.Builder()
                .withNamePrefix(CONSUL_WATCH.getName())
                .addEnv(ImmutableMap.of("CONSUL_HOST", "127.0.0.1"))
                .networkMode(HOST_NETWORK_MODE)
                .addVolumeBindings(ImmutableMap.of("/var/run/docker.sock", "/var/run/docker.sock"))
                .addHosts(hosts)
                .build();
    }

    public ContainerConstraint getLogrotateConstraint(List<String> hosts) {
        return new ContainerConstraint.Builder()
                .withNamePrefix(LOGROTATE.getName())
                .networkMode(HOST_NETWORK_MODE)
                .addVolumeBindings(ImmutableMap.of("/var/lib/docker/containers", "/var/lib/docker/containers"))
                .addHosts(hosts)
                .build();
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


}
