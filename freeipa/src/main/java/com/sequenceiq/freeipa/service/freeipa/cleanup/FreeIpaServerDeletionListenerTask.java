package com.sequenceiq.freeipa.service.freeipa.cleanup;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.polling.StatusCheckerTask;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.TopologySegment;
import com.sequenceiq.freeipa.client.model.TopologySuffix;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@Component
public class FreeIpaServerDeletionListenerTask implements StatusCheckerTask<FreeIpaServerDeletionPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaServerDeletionListenerTask.class);

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Override
    public boolean checkStatus(FreeIpaServerDeletionPollerObject freeIpaServerDeletionPollerObject) {
        boolean finished = false;
        // The IPA server delete continues running after the API returns. The last step is for FreeIPA to delete the topology segments.
        try {
            Long stackId = freeIpaServerDeletionPollerObject.getStackId();
            Set<String> hosts = freeIpaServerDeletionPollerObject.getHosts();
            FreeIpaClient client = freeIpaClientFactory.getFreeIpaClientForStackId(stackId);
            List<TopologySuffix> topologySuffixes = client.findAllTopologySuffixes();
            Set<TopologySegment> remainingSegments = new HashSet<>();
            for (TopologySuffix topologySuffix:topologySuffixes) {
                remainingSegments.addAll(client.findTopologySegments(topologySuffix.getCn()).stream()
                        .filter(s -> hosts.contains(s.getRightNode()) || hosts.contains(s.getLeftNode()))
                        .collect(Collectors.toList()));
            }
            if (remainingSegments.isEmpty()) {
                LOGGER.debug("All pending topology segments have been deleted");
                finished = true;
            } else {
                LOGGER.debug("The following topology segments are still pending deletion [{}]", remainingSegments);
            }
        } catch (Exception e) {
            LOGGER.debug("An exception occurred while waiting for FreeIPA server deletion to complete", e);
        }
        return finished;
    }

    @Override
    public void handleTimeout(FreeIpaServerDeletionPollerObject freeIpaServerDeletionPollerObject) {
        LOGGER.error("Operation timed out. FreeIPA server deletion failed to complete.");
    }

    @Override
    public String successMessage(FreeIpaServerDeletionPollerObject freeIpaServerDeletionPollerObject) {
        return "FreeIPA server deletion is complete.";
    }

    @Override
    public boolean exitPolling(FreeIpaServerDeletionPollerObject freeIpaServerDeletionPollerObject) {
        return false;
    }

    @Override
    public void handleException(Exception e) {
        LOGGER.error("FreeIPA server deletion polling exception", e);
    }
}
