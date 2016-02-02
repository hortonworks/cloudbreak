package com.sequenceiq.cloudbreak.orchestrator.marathon;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;
import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.model.v2.Container;
import mesosphere.marathon.client.model.v2.Docker;
import mesosphere.marathon.client.utils.MarathonException;

@RunWith(MockitoJUnitRunner.class)
public class MarathonContainerOrchestratorTest {

    private Marathon client;

    @Before
    public void setUp() throws Exception {
        client = MarathonClient.getInstance("http://172.16.252.31:8080");
    }

    @Test
    public void testCreateAmbariServerDbContainer() {
        App ambariDb = new App();
        ambariDb.setId("ambari-db");
        ambariDb.setCpus(0.5);
        ambariDb.setMem(512.0);
        ambariDb.setInstances(1);

        Container dbContainer = new Container();
        dbContainer.setType("DOCKER");

        Docker dbDocker = new Docker();
        dbDocker.setImage("postgres:9.4.1");
        dbDocker.setNetwork("HOST");
        dbContainer.setDocker(dbDocker);

        ambariDb.setEnv(ImmutableMap.of("POSTGRES_PASSWORD", "bigdata", "POSTGRES_USER", "ambari"));
        ambariDb.setContainer(dbContainer);

        try {
            ambariDb = client.createApp(ambariDb);
            System.out.println(ambariDb);
        } catch (MarathonException e) {
            e.printStackTrace();
        }
        //this is an ampty collection so the task info needs to be got manually
//        List<Task> tasks = new ArrayList<>(ambariDb.getTasks());
//        String host = tasks.get(0).getHost();
    }

    @Test
    public void testCreateAmbariServerContainer() {

        String dbHost = "mesos-slave2";

        App server = new App();
        server.setId("ambari-server3");
        server.setCpus(1.5);
        server.setMem(4096.0);
        server.setInstances(1);
        server.addPort(8080);

        Container serverContainer = new Container();
        serverContainer.setType("DOCKER");

        Docker serverDocker = new Docker();
        serverDocker.setPrivileged(true);
        serverDocker.setImage("hortonworks/ambari-server:2.2.1-v5");
        serverDocker.setNetwork("HOST");
        serverContainer.setDocker(serverDocker);

//        server.(ImmutableMap.of("systemd.setenv", String.format("POSTGRES_DB=%s", host)));
        server.setCmd(String.format("/usr/sbin/init systemd.setenv=POSTGRES_DB=%s", dbHost));
        server.setContainer(serverContainer);

        try {
            server = client.createApp(server);
            System.out.println(server);
        } catch (MarathonException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateAmbariAgentsContainer() {

        String serverHost = "mesos-slave1";

        App agents = new App();
        agents.setId("ambari-agent");
        agents.setCpus(1.5);
        agents.setMem(4096.0);
        agents.setInstances(3);

        Container agentContainer = new Container();
        agentContainer.setType("DOCKER");

        Docker agentDocker = new Docker();
        agentDocker.setPrivileged(true);
        agentDocker.setImage("hortonworks/ambari-agent:2.2.1-v5");
        agentDocker.setNetwork("HOST");
        agentContainer.setDocker(agentDocker);

//        server.(ImmutableMap.of("systemd.setenv", String.format("POSTGRES_DB=%s", host)));
        agents.setCmd(String.format("/usr/sbin/init systemd.setenv=AMBARI_SERVER_ADDR=%s systemd.setenv=USE_CONSUL_DNS=false", serverHost));
        agents.setContainer(agentContainer);

        try {
            agents = client.createApp(agents);
            System.out.println(agents);
        } catch (MarathonException e) {
            e.printStackTrace();
        }
    }
}