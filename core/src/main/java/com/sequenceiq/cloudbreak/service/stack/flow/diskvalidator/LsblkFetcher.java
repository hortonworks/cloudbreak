package com.sequenceiq.cloudbreak.service.stack.flow.diskvalidator;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;

@Service
public class LsblkFetcher {

    public static final String LSBLK_COMMAND = "lsblk -dn --output NAME,SIZE,MOUNTPOINT --bytes | " +
            "awk '{printf \"%s %s %s\\n\", $1, $2/1024/1024/1024, $3, $4}'";

    private static final Logger LOGGER = LoggerFactory.getLogger(LsblkFetcher.class);

    @Inject
    private HostOrchestrator hostOrchestrator;

    public MultiValuedMap<String, LsblkLine> getLsblkResults(List<GatewayConfig> allGatewayConfigs, Set<String> targetFqdns) throws
            CloudbreakOrchestratorFailedException {
        Map<String, String> lsblkResults = hostOrchestrator.runCommandOnHosts(allGatewayConfigs, targetFqdns, LSBLK_COMMAND);
        LOGGER.info("lsblk command results: {}", lsblkResults);
        MultiValuedMap<String, LsblkLine> lsblkResultMap = new ArrayListValuedHashMap<>();
        for (String hostName : lsblkResults.keySet()) {
            lsblkResults.get(hostName).lines().map(String::trim).filter(not(String::isEmpty)).forEach(lsblkLine -> {
                String[] lsblkLineParts = lsblkLine.split(" ");
                if (lsblkLineParts.length > 0) {
                    String device = lsblkLineParts[0];
                    String size = null;
                    if (lsblkLineParts.length > 1) {
                        size = lsblkLineParts[1];
                    }
                    String mountPoint = lsblkLineParts.length > 2 ? lsblkLineParts[2] : null;
                    lsblkResultMap.put(hostName, new LsblkLine(device, size, mountPoint));
                }
            });
        }
        return lsblkResultMap;
    }
}
