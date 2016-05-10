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
        return sc.run(target, "test.ping", LOCAL, null, PingResponse.class);
    }

    public static Object highstate(SaltConnector sc, Target<String> target) {
        return sc.run(target, "state.highstate", LOCAL_ASYNC, null, Object.class);
    }

    public static Object consul(SaltConnector sc, Target<String> target) {
        return sc.run(target, "state.apply", LOCAL_ASYNC, "consul", Object.class);
    }

    public static NetworkInterfaceResponse networkInterfaceIP(SaltConnector sc, Target<String> target) {
        return sc.run(target, "network.interface_ip", LOCAL, "eth0", NetworkInterfaceResponse.class);
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

}
