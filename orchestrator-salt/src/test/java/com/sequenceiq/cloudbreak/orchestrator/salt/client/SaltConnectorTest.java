package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;

import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.NetworkInterfaceResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.StateType;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public class SaltConnectorTest {

    private SaltConnector client;

    @Before
    public void setup() {
        String id = "183";
        GatewayConfig gatewayConfig = new GatewayConfig("172.16.252.43", "10.0.0.5", "host-172-16-252-43", 9443,
                "/Users/rdoktorics/prj/certs/stack-" + id,
                "/Users/rdoktorics/prj/certs/stack-" + id + "/ca.pem",
                "/Users/rdoktorics/prj/certs/stack-" + id + "/cert.pem",
                "/Users/rdoktorics/prj/certs/stack-" + id + "/key.pem",
                "saltPasswd", "saltBootPassword", "signkey");
        client = new SaltConnector(gatewayConfig, true);
    }

    //@Test
    public void testJid() {
        Object pingResponse = SaltStates.jidInfo(client, "20160510134036754632", Glob.ALL, StateType.SIMPLE);
    }

    //@Test
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

    private List<String> appendConsulRole(List<String> consulServers, String minionIp, List<String> roles) {
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
    public void testRunNetIf() {
        List<String> targets = Collections.singletonList("10.0.0.5");
        String targetIps = "S@" + targets.stream().collect(Collectors.joining(" or S@"));
        NetworkInterfaceResponse response = SaltStates.networkInterfaceIP(client, new Compound(targetIps));
        Assert.assertNotNull(response.getResultGroupByHost());
    }

    //@Test
    public void testRunDeleteMinions() {
        List<String> targets = Collections.singletonList("10.0.0.5");
        Object object = SaltStates.removeMinions(client, targets);
        Assert.assertNotNull(object);
    }

}
