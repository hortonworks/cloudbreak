package com.sequenceiq.cloudbreak.service.stack.flow.diskvalidator;

import static java.util.function.Predicate.not;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.job.disk.model.InstanceResourceDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;

@Service
public class LsblkFetcher {

    static final String LSBLK_COMMAND = "lsblk -b -P -o NAME,SIZE,SERIAL,UUID,MOUNTPOINT,TYPE,HCTL; ";

    private static final Pattern PAIR_PATTERN = Pattern.compile("(\\w+)=\"([^\"]*)\"");

    private static final Long BYTES_IN_GB = 1024L * 1024L * 1024L;

    private static final Logger LOGGER = LoggerFactory.getLogger(LsblkFetcher.class);

    @Inject
    private HostOrchestrator hostOrchestrator;

    public MultiValuedMap<String, InstanceResourceDto.VolumeDto> getLsblkResults(List<GatewayConfig> allGatewayConfigs, Set<String> targetFqdns)
        throws CloudbreakOrchestratorFailedException {
        Map<String, String> lsblkResults = hostOrchestrator.runCommandOnHosts(allGatewayConfigs, targetFqdns, LSBLK_COMMAND);
        LOGGER.info("lsblk command results: {}", lsblkResults);
        return extractSyncLsblkLines(lsblkResults);
    }

    private MultiValuedMap<String, InstanceResourceDto.VolumeDto> extractSyncLsblkLines(Map<String, String> lsblkResults) {
        MultiValuedMap<String, InstanceResourceDto.VolumeDto> lsblkResultMap = new ArrayListValuedHashMap<>();
        for (String hostName : lsblkResults.keySet()) {
            lsblkResults.get(hostName).lines().map(String::trim).filter(not(String::isEmpty)).forEach(lsblkLine -> {
                Map<String, String> lsblkLineParts = parseLine(lsblkLine);
                if (!lsblkLineParts.isEmpty()) {
                    lsblkResultMap.put(hostName, convertToVolumeDto(lsblkLineParts));
                }
            });
        }
        return lsblkResultMap;
    }

    private InstanceResourceDto.VolumeDto convertToVolumeDto(Map<String, String> lsblkLineParts) {
        String device = lsblkLineParts.get("NAME");
        String mountPoint = lsblkLineParts.get("MOUNTPOINT");
        String size = lsblkLineParts.get("SIZE");
        String type = lsblkLineParts.get("TYPE");
        String uuid = lsblkLineParts.get("UUID");
        String serial = lsblkLineParts.get("SERIAL");
        String hctl = lsblkLineParts.get("HCTL");
        return new InstanceResourceDto.VolumeDto(null, device, mountPoint, convertBytesToGb(size), type,
            uuid, serial, hctl);
    }

    private int convertBytesToGb(String rawByteString) {
        if (rawByteString == null || rawByteString.isEmpty()) {
            return 0;
        }
        long bytes = Long.parseLong(rawByteString);

        return (int) (bytes / BYTES_IN_GB);
    }

    private Map<String, String> parseLine(String line) {
        Map<String, String> map = new HashMap<>();
        Matcher m = PAIR_PATTERN.matcher(line);
        while (m.find()) {
            map.put(m.group(1), m.group(2));
        }
        return map;
    }
}
