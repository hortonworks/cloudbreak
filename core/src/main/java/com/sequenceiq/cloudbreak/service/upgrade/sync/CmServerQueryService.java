package com.sequenceiq.cloudbreak.service.upgrade.sync;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;

@Service
public class CmServerQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmServerQueryService.class);

    @Inject
    private ClusterApiConnectors apiConnectors;

    /**
     * Will query all active parcels (CDH and non-CDH as well) from the CM server. Received format:
     * CDH -> 7.2.7-1.cdh7.2.7.p7.12569826
     * <p>
     * TODO: the call is blocking for some time. Check if it is possible to block for shorter period of time.
     *
     * @param stack The stack, to get the coordinates of the CM to query
     * @return List of parcels found in the CM
     */
    Set<ParcelInfo> queryActiveParcels(Stack stack) {
        Map<String, String> installedParcels = apiConnectors.getConnector(stack).gatherInstalledParcels(stack.getName());
        LOGGER.debug("Reading parcel info from CM server, found parcels: " + installedParcels);
        return installedParcels.entrySet().stream()
                .map(es -> new ParcelInfo(es.getKey(), es.getValue()))
                .collect(Collectors.toSet());
    }

    /**
     * Will query the CM server for the CM version
     * @param stack The stack, to get the coordinates of the CM to query
     * @return The actual CM version
     */
    String queryCmVersion(Stack stack) {
        String cmVersion = apiConnectors.getConnector(stack).clusterStatusService().getClusterManagerVersion();
        LOGGER.debug("Reading CM version info from CM server, found version: {}", cmVersion);
        return cmVersion;
    }

}
