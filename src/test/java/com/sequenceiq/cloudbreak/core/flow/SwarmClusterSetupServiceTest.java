package com.sequenceiq.cloudbreak.core.flow;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import com.github.dockerjava.core.DockerClientConfig;
import com.sequenceiq.cloudbreak.core.CloudbreakException;

public class SwarmClusterSetupServiceTest {

    private String ambariDockerTag = "sequenceiq/ambari:1.7.0-consul";
    private String consulServers = "10.0.89.64,10.0.132.154,10.0.184.111";
    private String consulJoinIps = "10.0.132.154:2376,10.0.184.111:2376,10.0.10.143:2376,10.0.89.64:2376";

    @Test
    public void test1() throws CloudbreakException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try {
            //DockerClient dockerApiClient = DockerClientBuilder.getInstance(getDockerClientConfig("104.197.46.186")).build();
            //new MunchausenBootstrap(dockerApiClient, "10.0.89.64", consulServers, consulJoinIps).call();
            //DockerClient swarmApiClient = DockerClientBuilder.getInstance(getSwarmClientConfig("146.148.64.198")).build();
            //new AmbariAgentBootstrap(swarmApiClient,
            //        "10.0.4.23", "ezegymunchausentest16-0-1428486572260", ambariDockerTag, 1).call();

        } catch (Exception ex) {
            throw new CloudbreakException(ex);
        }
    }

    private DockerClientConfig getSwarmClientConfig(String ip) {
        return DockerClientConfig.createDefaultConfigBuilder()
                .withVersion("1.16")
                .withUri("http://" + ip + ":3376")
                .build();
    }

    private DockerClientConfig getDockerClientConfig(String ip) {
        return DockerClientConfig.createDefaultConfigBuilder()
                .withVersion("1.16")
                .withUri("http://" + ip + ":2376")
                .build();
    }
}