package com.sequenceiq.it.cloudbreak.util;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;

/**
 * SSH df -B1 and lsblk per mount to verify guest-visible size after disk resize (e.g. {@code resize-storage-volumes.j2}).
 */
@Component
public class InstanceHostDiskAssertions {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceHostDiskAssertions.class);

    /** Minimum fraction of provisioned GiB that df/lsblk must report (7% slack for ext4 root reserve, XFS metadata/journals, and GiB rounding). */
    private static final double MIN_FRACTION = 0.93;

    @Inject
    private SshJClientActions sshJClientActions;

    public void assertMountPointsAtLeastProvisionedSize(List<InstanceGroupV4Response> instanceGroups, String hostGroupName,
            Map<String, Integer> mountPointToExpectedGiB, String validationContext) {
        if (!isSshReachable(instanceGroups, hostGroupName)) {
            LOGGER.warn("{}: skipping host disk size assertions — SSH not reachable for host group '{}'", validationContext, hostGroupName);
            return;
        }
        for (Map.Entry<String, Integer> entry : mountPointToExpectedGiB.entrySet()) {
            String mount = entry.getKey();
            int gib = entry.getValue();
            long minBytes = Math.round(gib * MIN_FRACTION * 1024L * 1024L * 1024L);
            String cmd = shellCommand(mount);
            sshJClientActions.executeSshCommandOnHost(instanceGroups, List.of(hostGroupName), cmd, false)
                    .forEach((ip, result) -> {
                        assertSize(ip, mount, gib, minBytes, result, validationContext);
                    });
        }
    }

    private boolean isSshReachable(List<InstanceGroupV4Response> instanceGroups, String hostGroupName) {
        try {
            sshJClientActions.executeSshCommandOnHost(instanceGroups, List.of(hostGroupName), "true", false);
            return true;
        } catch (TestFailException e) {
            LOGGER.warn("SSH not reachable for host group '{}': {}", hostGroupName, e.getMessage());
            return false;
        }
    }

    private static String shellCommand(String mountPoint) {
        return "MP=" + mountPoint + "; "
                + "df_sz=$(sudo df -B1 -P \"$MP\" | tail -1 | awk '{print $2}'); "
                + "src=$(sudo findmnt -nro SOURCE --target \"$MP\"); "
                + "lb_sz=$(sudo lsblk -b -ndo SIZE \"$src\" 2>/dev/null || echo 0); "
                + "echo DF=$df_sz LSBLK=$lb_sz";
    }

    private static void assertSize(String ip, String mount, int expectedGiB, long minBytes, Pair<Integer, String> result, String ctx) {
        if (result.getKey() != 0) {
            throw new TestFailException(String.format("%s: %s %s probe exit %d: %s", ctx, ip, mount, result.getKey(), result.getValue()));
        }
        long df = -1;
        long lsblk = -1;
        for (String part : result.getValue().trim().split("\\s+")) {
            if (part.startsWith("DF=")) {
                df = Long.parseLong(part.substring(3));
            } else if (part.startsWith("LSBLK=")) {
                lsblk = Long.parseLong(part.substring(6));
            }
        }
        if (df < 0 || lsblk < 0) {
            throw new TestFailException(String.format("%s: %s %s unparseable: %s", ctx, ip, mount, result.getValue()));
        }
        if (df < minBytes) {
            throw new TestFailException(String.format("%s: %s %s df %d < %d bytes (~%d GiB)", ctx, ip, mount, df, minBytes, expectedGiB));
        }
        if (lsblk > 0 && lsblk < minBytes) {
            throw new TestFailException(String.format("%s: %s %s lsblk %d < %d bytes (~%d GiB)", ctx, ip, mount, lsblk, minBytes, expectedGiB));
        }
    }
}
