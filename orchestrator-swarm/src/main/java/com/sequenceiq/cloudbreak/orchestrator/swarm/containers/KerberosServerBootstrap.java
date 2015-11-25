package com.sequenceiq.cloudbreak.orchestrator.swarm.containers;

import static com.github.dockerjava.api.model.RestartPolicy.alwaysRestart;
import static com.sequenceiq.cloudbreak.orchestrator.containers.DockerContainer.KERBEROS;
import static com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration.DOMAIN_REALM;
import static com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration.REALM;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.createContainer;
import static com.sequenceiq.cloudbreak.orchestrator.swarm.DockerClientUtil.startContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;
import com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration;
import com.sequenceiq.cloudbreak.orchestrator.swarm.builder.BindsBuilder;

public class KerberosServerBootstrap implements ContainerBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosServerBootstrap.class);

    private final DockerClient docker;
    private final String imageName;
    private final String nodeName;
    private final LogVolumePath logVolumePath;
    private final KerberosConfiguration config;

    public KerberosServerBootstrap(DockerClient docker, String imageName, String nodeName, LogVolumePath logVolumePath, KerberosConfiguration config) {
        this.docker = docker;
        this.imageName = imageName;
        this.nodeName = nodeName;
        this.logVolumePath = logVolumePath;
        this.config = config;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info("Creating Kerberos server container on: {}", nodeName);

        Bind[] binds = new BindsBuilder().addLog(logVolumePath).add("/etc/krb5.conf").build();

        String name = KERBEROS.getName();
        createContainer(docker, docker.createContainerCmd(imageName)
                .withNetworkMode("host")
                .withRestartPolicy(alwaysRestart())
                .withPrivileged(true)
                .withBinds(binds)
                .withName(name)
                .withEnv(
                        String.format("constraint:node==%s", nodeName),
                        String.format("SERVICE_NAME=%s", name),
                        "NAMESERVER_IP=127.0.0.1",
                        String.format("REALM=%s", REALM),
                        String.format("DOMAIN_REALM=%s", DOMAIN_REALM),
                        String.format("KERB_MASTER_KEY=%s", config.getMasterKey()),
                        String.format("KERB_ADMIN_USER=%s", config.getUser()),
                        String.format("KERB_ADMIN_PASS=%s", config.getPassword())
                ), nodeName
        );

        startContainer(docker, name);

        return true;
    }
}
