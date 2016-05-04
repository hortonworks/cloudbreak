package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.*;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

public class SaltConnectorTest {

    private SaltConnector client;

    @Before
    public void setup() {
        String id = "614";
        GatewayConfig gatewayConfig = new GatewayConfig("172.16.252.33", "10.0.0.5", 9443,
                "/Users/akanto/prj/cbd-test/certs/stack-" + id,
                "/Users/akanto/prj/cbd-test/certs/stack-" + id + "/ca.pem",
                "/Users/akanto/prj/cbd-test/certs/stack-" + id + "/cert.pem",
                "/Users/akanto/prj/cbd-test/certs/stack-" + id + "/key.pem");
        client = new SaltConnector(gatewayConfig, true);
    }

   // @Test
    public void testHealth() {
        client.health();
    }

    //@Test
    public void testPillar() {
        List<String> consulServers = Arrays.asList("10.0.0.3", "10.0.0.2", "10.0.0.5");
        Map<String, Object> conf = new HashMap<>();
        Map<String, Object> consul = new HashMap();
        consul.put("bootstrap_expect", consulServers.size());
        consul.put("retry_join", consulServers);
        conf.put("consul", consul);
        Pillar pillar = new Pillar("/consul/init.sls", conf);
        client.pillar(pillar);
    }

    //@Test
    public void testStart() {

        String gatewayPrivateIp = "10.0.0.5";
        List<String> targets = Arrays.asList("10.0.0.3", "10.0.0.2", "10.0.0.5");
        SaltAction saltAction = new SaltAction(SaltActionType.RUN);

        if (targets.contains(gatewayPrivateIp)) {
            saltAction.setServer(gatewayPrivateIp);
            List<String> roles = new ArrayList<>();
            roles.add("ambari_server");
            roles = appendConsulRole(targets, gatewayPrivateIp, roles);
            saltAction.addMinion(createMinion(gatewayPrivateIp, gatewayPrivateIp, roles));
        }
        for (String minionIp : targets) {
            if (!minionIp.equals(gatewayPrivateIp)) {
                List<String> roles = new ArrayList<>();
                roles.add("ambari_agent");
                roles = appendConsulRole(targets, gatewayPrivateIp, roles);
                saltAction.addMinion(createMinion(gatewayPrivateIp, minionIp, roles));
            }
        }

        client.action(saltAction);
    }

    private List<String> appendConsulRole( List<String> consulServers, String minionIp, List<String> roles) {
        if (consulServers.contains(minionIp)) {
            roles.add("consul_server");
        } else {
            roles.add("consul_agent");
        }
        return roles;
    }

    private Minion createMinion(String gatewayPrivateIp, String address, List<String> roles) {
        Minion minion = new Minion();
        minion.setAddress(address);
        minion.setRoles(roles);
        minion.setServer(gatewayPrivateIp);
        return minion;
    }




   //@Test
    public void testRunPing() {
        PingResponse pingResponse = SaltStates.ping(client, Glob.ALL);
        Assert.assertNotNull(pingResponse.getResult());
    }

    //@Test
    public void testRunNetIf() {
        List<String> targets = Arrays.asList("10.0.0.5");
        String targetIps = "S@" + targets.stream().collect(Collectors.joining(" or S@"));
        NetworkInterfaceResponse response = SaltStates.networkInterfaceIP(client, new Compound(targetIps));
        System.out.println(response.getResult().size());
        System.out.println(response.getResult());
        Assert.assertNotNull(response.getResult());
    }

    @Test
    public void testRunDeleteMinions() {
        List<String> targets = Arrays.asList("10.0.0.5");
        Object object = SaltStates.removeMinions(client, targets);
        Assert.assertNotNull(object);
    }

    //@Test
    public void testRunConsul() {
        Object object = SaltStates.consul(client, Glob.ALL);
        Assert.assertNotNull(object);
    }

    //@Test
    public void testRunHighstate() {
        Object object = SaltStates.highstate(client, Glob.ALL);
        Assert.assertNotNull(object);
    }

}
