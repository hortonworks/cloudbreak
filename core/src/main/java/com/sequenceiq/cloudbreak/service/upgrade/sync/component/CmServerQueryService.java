package com.sequenceiq.cloudbreak.service.upgrade.sync.component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.common.model.PackageInfo;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;

@Service
public class CmServerQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmServerQueryService.class);

    @Inject
    private ClusterApiConnectors apiConnectors;

    @Inject
    private CmVersionQueryService cmVersionQueryService;

    public Set<ParcelInfo> queryAllParcels(StackDto stackDto) {
        Set<ParcelInfo> parcels = apiConnectors.getConnector(stackDto).getAllParcels(stackDto.getName());
        LOGGER.debug("Reading parcel info from CM server, found parcels: " + parcels);
        return parcels;
    }

    /**
     * Will query all active parcels (CDH and non-CDH as well) from the CM server. Received format:
     * CDH -> 7.2.7-1.cdh7.2.7.p7.12569826
     * <p>
     * TODO: the call is blocking for some time. Check if it is possible to block for shorter period of time.
     *
     * @param stack The stack, to get the coordinates of the CM to query
     * @return Set of parcels found in the CM
     */
    public Set<ParcelInfo> queryActiveParcels(Stack stack) {
        Set<ParcelInfo> activeParcels = apiConnectors.getConnector(stack).gatherInstalledParcels(stack.getName());
        LOGGER.debug("Reading parcel info from CM server, found active parcels: " + activeParcels);
        return activeParcels;
    }

    /**
     * Will query all the nodes for the installed CM version
     * @param stack The stack, with metadata to be able to build the client to query package versions
     * @return The actual CM version, in format version-build number e.g. 7.2.2-13072522
     */
    public Optional<String> queryCmVersion(StackDtoDelegate stack) {
        try {
            Map<String, List<PackageInfo>> packageVersions = cmVersionQueryService.queryCmPackageInfo(stack);
            PackageInfo cmPackageInfo = cmVersionQueryService.checkCmPackageInfoConsistency(packageVersions, stack.getId());
            String version = cmPackageInfo.getFullVersionPrettyPrinted();
            LOGGER.debug("Reading CM version info, found version: {}", version);
            return Optional.of(version);
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.warn("Encountered error during reading CM version info", e);
            return Optional.empty();
        }
    }

    public boolean isCmServerRunning(Stack stack) {
        return apiConnectors.getConnector(stack).clusterStatusService().isClusterManagerRunning();
    }

    public boolean isCmServerNotRunning(Stack stack) {
        return !isCmServerRunning(stack);
    }

}
