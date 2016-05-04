package com.sequenceiq.cloudbreak.orchestrator.salt.states;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.NetworkInterfaceResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.PingResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL;
import static com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltClientType.LOCAL_ASYNC;

public class SaltStates {

    public static PingResponse ping(SaltConnector sc, Target<String> target) {
        return sc.run(target, "test.ping", LOCAL, null, PingResponse.class);
    }

    public static Object highstate(SaltConnector sc, Target<String> target) {
        Object response = sc.run(target, "state.highstate", LOCAL_ASYNC, null, Object.class);
        return response;
    }

    public static Object consul(SaltConnector sc, Target<String> target) {
        Object response = sc.run(target, "state.apply", LOCAL_ASYNC, "consul", Object.class);
        return response;
    }

    public static NetworkInterfaceResponse networkInterfaceIP(SaltConnector sc, Target<String> target) {
        // TODO this does not work if the minion is dead
        return sc.run(target, "network.interface_ip", LOCAL, "eth0", NetworkInterfaceResponse.class);
    }

    public static Object removeMinions(SaltConnector sc, List<String> ips) {
        // This is slow
        // String targetIps = "S@" + ips.stream().collect(Collectors.joining(" or S@"));
        //Map<String, String> ipToMinionId = SaltStates.networkInterfaceIP(sc, new Compound(targetIps)).getResult();

        Map<String, String> ipSet = SaltStates.networkInterfaceIP(sc, Glob.ALL).getResult();
        List<String> minionIds = ipSet.entrySet().stream()
                .filter(entry -> ips.contains(entry.getKey()))
                .map(Map.Entry::getValue).collect(Collectors.toList());

        SaltAction saltAction = new SaltAction(SaltActionType.STOP);
        for (String ip : ips) {
            Minion minion = new Minion();
            minion.setAddress(ip);
            saltAction.addMinion(minion);
        }
        sc.action(saltAction);

        Object response = sc.wheel("key.delete", minionIds, Object.class);
        return response;
    }

}
