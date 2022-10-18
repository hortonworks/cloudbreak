package com.sequenceiq.cloudbreak.orchestrator.salt.utils;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;

@Component
public class MinionUtil {

    public Minion createMinion(Node node, List<String> gatewayPrivateIps, boolean restartNeededFlagSupported, boolean restartNeeded) {
        Minion minion = new Minion();
        minion.setAddress(node.getPrivateIp());
        minion.setHostGroup(node.getHostGroup());
        minion.setHostName(node.getHostname());
        minion.setDomain(node.getDomain());
        minion.setServers(calculateServerIps(gatewayPrivateIps, restartNeededFlagSupported, restartNeeded));
        minion.setRestartNeeded(restartNeeded);
        // set due to compatibility reasons
        minion.setServer(gatewayPrivateIps.get(0));
        return minion;
    }

    /***
     *  Restart needed flag was introduced for the following use case:
     *
     *     In a repair scenario where salt master has the same IP address as before, the minions on other instances are not restarted by salt-bootstrap.
     *     Salt minion does not try to communicate with master by itself for a long time (30mins or sthg like this).
     *     So when we are waiting for the minions' keys on salt-master then timeout can happen if all the minions haven't tried to communicate with
     *     master during this time frame.
     *
     *     Timeout error message in this scenario: There are missing nodes from salt network response.
     *     Solution was to introduce a restartNeeded flag in salt-bootstrap and we restart salt minions every time we do a salt-master replacement.
     *     (it used to be restarted only in case when salt-master IP address changed)
     *
     *  In case of older salt-bootsrap ( pre 0.13.4 ) the restartNeeded flag is not interpreted. So in an upgrade scenario (it has a repair flow in
     *  itself) where the upgrade starts from a 7.1.0 image, the minions are not restarted so the above mentioned timeout could happen.
     *
     *  It is a dirty fix but if the restartNeeded flag is set but salt-bootstrap does not support this flag, then we change the
     *  salt master ip address to loopback address (127.0.0.1), so salt-minion will be restarted by older salt-bootstrap also.
     *
     *  The original IP will be set in the next iteration because restartNeeded flag will be false.
     *
     * @param restartNeededFlagSupported is restartNeeded flag supported by salt-bootstrap
     * @param restartNeeded restart minions
     * @return salt master(s) ip adress(es)
     */
    private static List<String> calculateServerIps(List<String> gatewayPrivateIps, boolean restartNeededFlagSupported, boolean restartNeeded) {
        if (!restartNeededFlagSupported && restartNeeded) {
            return Collections.singletonList("127.0.0.1");
        } else {
            return gatewayPrivateIps;
        }
    }
}
