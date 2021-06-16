package com.sequenceiq.cloudbreak.service.upgrade.sync;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class CmParcelInfoRetrieverService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmParcelInfoRetrieverService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterApiConnectors apiConnectors;

    /**
     * Will query all active parcels (CDH and non-CDH as well) from the CM server. Received format:
     * CDH -> 7.2.7-1.cdh7.2.7.p7.12569826
     * <p>
     * TODO: the call is blocking for some time. Check if it is possible to block for shorter period of time.
     *
     * @param stackId The id of the stack of which CM cloudbreak should query
     * @return List of parcels found in the CM
     */
    List<ParcelInfo> getActiveParcelsFromServer(long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
//        String cmVersion = apiConnectors.getConnector(stack).clusterStatusService().getClusterManagerVersion();
        Map<String, String> installedParcels = apiConnectors.getConnector(stack).gatherInstalledParcels(stack.getName());
        LOGGER.debug("Reading parcel info from CM server, found parcels: " + installedParcels);
        return installedParcels.entrySet().stream()
                .map(es -> new ParcelInfo(es.getKey(), es.getValue()))
                .collect(Collectors.toList());
    }

}
