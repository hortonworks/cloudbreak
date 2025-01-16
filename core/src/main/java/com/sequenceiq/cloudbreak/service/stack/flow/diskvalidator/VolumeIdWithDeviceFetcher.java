package com.sequenceiq.cloudbreak.service.stack.flow.diskvalidator;

import static java.util.function.Predicate.not;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;

@Service
public class VolumeIdWithDeviceFetcher {

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public MultiValuedMap<String, VolumeIdWithDevice> getVolumeMappings(List<GatewayConfig> allGatewayConfigs, Set<String> targetFqdns,
            CloudPlatformVariant cloudPlatformVariant)
            throws CloudbreakOrchestratorFailedException {
        String volumeIdFetcherScript = cloudPlatformConnectors.get(cloudPlatformVariant).scriptResources().getVolumeIdFetcherScript();
        return fetchVolumeMap(allGatewayConfigs, targetFqdns, volumeIdFetcherScript);
    }

    private MultiValuedMap<String, VolumeIdWithDevice> fetchVolumeMap(List<GatewayConfig> allGatewayConfigs, Set<String> targetFqdns, String listCommand)
            throws CloudbreakOrchestratorFailedException {
        if (listCommand == null) {
            return new ArrayListValuedHashMap<>();
        }
        Map<String, String> listVolumesCommand = hostOrchestrator.runCommandOnHosts(allGatewayConfigs, targetFqdns, listCommand);
        MultiValuedMap<String, VolumeIdWithDevice> diskByIdResultMap = new ArrayListValuedHashMap<>();
        for (String hostName : listVolumesCommand.keySet()) {
            listVolumesCommand.get(hostName).lines().map(String::trim).filter(not(String::isEmpty)).forEach(lsblkLine -> {
                String[] lsblkLineParts = lsblkLine.split(" ");
                String volumeId = lsblkLineParts[0];
                String device = lsblkLineParts[1];
                diskByIdResultMap.put(hostName, new VolumeIdWithDevice(volumeId, device));
            });
        }
        return diskByIdResultMap;
    }
}
