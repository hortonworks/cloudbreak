package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL_ASYNC;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.NetworkInterfaceResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PingResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;

public class SaltStates {

    private SaltStates() {
    }

    public static PingResponse ping(SaltConnector sc, Target<String> target) {
        return sc.run(target, "test.ping", LOCAL, PingResponse.class);
    }

    public static Object ambariServer(SaltConnector sc, Target<String> target) {
        return applyState(sc, "ambari.server", target);
    }

    public static Object ambariAgent(SaltConnector sc, Target<String> target) {
        return applyState(sc, "ambari.agent", target);
    }

    public static Object kerberos(SaltConnector sc, Target<String> target) {
        return applyState(sc, "kerberos.server", target);
    }

    public static Object addRole(SaltConnector sc, Target<String> target, String role) {
        return sc.run(target, "grains.append", LOCAL, Object.class, "roles", role);
    }

    public static Object syncGrains(SaltConnector sc) {
        return sc.run(Glob.ALL, "saltutil.sync_grains", LOCAL, Object.class);
    }

    public static Object highstate(SaltConnector sc) {
        return sc.run(Glob.ALL, "state.highstate", LOCAL_ASYNC, Object.class);
    }

    public static Object consul(SaltConnector sc, Target<String> target) {
        return applyState(sc, "consul", target);
    }

    public static NetworkInterfaceResponse networkInterfaceIP(SaltConnector sc, Target<String> target) {
        return sc.run(target, "network.interface_ip", LOCAL, NetworkInterfaceResponse.class, "eth0");
    }

    public static Object removeMinions(SaltConnector sc, List<String> hostnames) {
        // This is slow
        // String targetIps = "S@" + hostnames.stream().collect(Collectors.joining(" or S@"));
        //Map<String, String> ipToMinionId = SaltStates.networkInterfaceIP(sc, new Compound(targetIps)).getResultGroupByHost();

        Map<String, String> saltHostnames = SaltStates.networkInterfaceIP(sc, Glob.ALL).getResultGroupByHost();
        List<String> hostnamesWithoutDomain = hostnames.stream().map(host -> host.split("\\.")[0]).collect(Collectors.toList());
        List<String> minionIds = saltHostnames.entrySet().stream()
                .filter(entry -> hostnamesWithoutDomain.contains(entry.getKey().split("\\.")[0]))
                .map(Map.Entry::getKey).collect(Collectors.toList());
        SaltAction saltAction = new SaltAction(SaltActionType.STOP);
        for (String hostname : minionIds) {
            Minion minion = new Minion();
            minion.setAddress(saltHostnames.get(hostname));
            saltAction.addMinion(minion);
        }
        sc.action(saltAction);

        return sc.wheel("key.delete", minionIds, Object.class);
    }

    private static Object applyState(SaltConnector sc, String service, Target<String> target) {
        return sc.run(target, "state.apply", LOCAL_ASYNC, Object.class, service);
    }

}
