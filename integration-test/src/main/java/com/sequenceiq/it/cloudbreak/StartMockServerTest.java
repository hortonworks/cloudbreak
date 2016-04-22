package com.sequenceiq.it.cloudbreak;

import javax.inject.Inject;

import org.springframework.core.io.ResourceLoader;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.it.mock.restito.ambari.AmbariAdminPutStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariBlueprintsGetStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariBlueprintsPostStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariCheckGetStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariClustersGetStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariClustersHostsGetStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariClustersPostStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariClustersRequestsGetStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariClustersRequestsPostStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariHostsGetStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariServicesComponentsGetStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariViewsInstancesFilesStub;
import com.sequenceiq.it.mock.restito.consul.ConsulEventPutStub;
import com.sequenceiq.it.mock.restito.consul.ConsulKeyValueGetStub;
import com.sequenceiq.it.mock.restito.consul.ConsulKeyValuePutStub;
import com.sequenceiq.it.mock.restito.consul.ConsulMembersStub;
import com.sequenceiq.it.mock.restito.docker.DockerContainersGetStub;
import com.sequenceiq.it.mock.restito.docker.DockerInfoGetStub;
import com.sequenceiq.it.mock.restito.docker.DockerContainersStartPostStub;
import com.sequenceiq.it.mock.restito.docker.SwarmContainerStub;
import com.sequenceiq.it.mock.restito.docker.SwarmInfoStub;
import com.sequenceiq.it.mock.restito.docker.SwarmStartContainerStub;
import com.xebialabs.restito.server.StubServer;

public class StartMockServerTest extends AbstractCloudbreakIntegrationTest {

    @Inject
    private ResourceLoader resourceLoader;

    @Test
    @Parameters({"port", "serverNumber"})
    public void startWireMockServer(@Optional("port") Integer port, @Optional("1") String serverNumber) throws InterruptedException {
        StubServer stubServer = new StubServer(port).secured().run();

        int numberOfServers = Integer.parseInt(serverNumber);

        stubServer.addStub(new DockerContainersStartPostStub());
        stubServer.addStub(new DockerInfoGetStub());
        stubServer.addStub(new SwarmStartContainerStub());
        stubServer.addStub(new SwarmInfoStub(numberOfServers));
        stubServer.addStub(new SwarmContainerStub());
        stubServer.addStub(new DockerContainersGetStub());

        stubServer.addStub(new ConsulKeyValueGetStub());
        stubServer.addStub(new ConsulKeyValuePutStub());
        stubServer.addStub(new ConsulEventPutStub());
        stubServer.addStub(new ConsulMembersStub(numberOfServers));

        stubServer.addStub(new AmbariClustersRequestsGetStub());
        stubServer.addStub(new AmbariViewsInstancesFilesStub());
        stubServer.addStub(new AmbariClustersHostsGetStub(numberOfServers));
        stubServer.addStub(new AmbariClustersGetStub(numberOfServers));
        stubServer.addStub(new AmbariClustersPostStub());
        stubServer.addStub(new AmbariClustersRequestsPostStub());
        stubServer.addStub(new AmbariServicesComponentsGetStub());
        stubServer.addStub(new AmbariHostsGetStub(numberOfServers));
        stubServer.addStub(new AmbariBlueprintsGetStub());
        stubServer.addStub(new AmbariBlueprintsPostStub());
        stubServer.addStub(new AmbariAdminPutStub());
        stubServer.addStub(new AmbariCheckGetStub());
    }
}
