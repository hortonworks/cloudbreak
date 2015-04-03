package com.sequenceiq.cloudbreak.core.flow.containers;

import static com.sequenceiq.cloudbreak.service.cluster.flow.DockerContainer.AMBARI_AGENT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.RestartPolicy;
import com.github.dockerjava.api.model.Volume;

public class AmbariAgentBootstrap implements Callable<Boolean> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariAgentBootstrap.class);

    private static  final int PORT = 9996;
    private final DockerClient docker;
    private final String privateIp;
    private final String longName;
    private final String ambariDockerTag;
    private final int volumeCount;
    private final Long id;

    public AmbariAgentBootstrap(DockerClient docker, String privateIp, String longName, String ambariDockerTag, int volumeCount, Long id) {
        this.docker = docker;
        this.privateIp = privateIp;
        this.longName = longName;
        this.ambariDockerTag = ambariDockerTag;
        this.volumeCount = volumeCount;
        this.id = id;
    }

    @Override
    public Boolean call() throws Exception {
        LOGGER.info(String.format("Ambari agent starting on %s node with %s private ip", longName, privateIp));
        try {
            HostConfig hostConfig = new HostConfig();
            hostConfig.setNetworkMode("host");
            hostConfig.setPrivileged(true);
            hostConfig.setRestartPolicy(RestartPolicy.alwaysRestart());

            CreateContainerResponse response = docker.createContainerCmd(ambariDockerTag)
                    .withHostConfig(hostConfig)
                    .withName(String.format("%s-%s", AMBARI_AGENT.getName(), id))
                    .withEnv(String.format("constraint:node==%s", longName),
                            String.format("BRIDGE_IP=%s", privateIp),
                            "HADOOP_CLASSPATH=/data/jars/*:/usr/lib/hadoop/lib/*")
                    .withCmd("/start-agent")
                    .exec();
            List<Bind> binds = new ArrayList<>();
            binds.add(new Bind("/usr/local/public_host_script.sh", new Volume("/etc/ambari-agent/conf/public-hostname.sh")));
            binds.add(new Bind("/data/jars", new Volume("/data/jars")));
            for (int j = 0; j < volumeCount; j++) {
                binds.add(new Bind("/hadoopfs/fs" + j, new Volume("/hadoopfs/fs" + j)));
            }
            Bind[] array = new Bind[binds.size()];
            binds.toArray(array);
            LOGGER.info(String.format("Bind array buildt on %s host %s", longName, Arrays.toString(array)));
            docker.startContainerCmd(response.getId())
                    .withNetworkMode("host")
                    .withRestartPolicy(RestartPolicy.alwaysRestart())
                    .withBinds(array)
                    .exec();
            LOGGER.info(String.format("Ambari agent start was success on %s node with %s private ip", longName, privateIp));
            return true;
        } catch (Exception ex) {
            LOGGER.error(String.format("Ambari agent start failed on %s node with %s private ip", longName, privateIp));
            throw ex;
        }
    }
}
