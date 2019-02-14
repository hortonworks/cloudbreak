package com.sequenceiq.cloudbreak.service.cluster.ambari;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.clusterdefinition.SmartsenseConfigurationLocator;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;

@Service
public class AmbariSmartSenseCapturer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariSmartSenseCapturer.class);

    @Inject
    private SmartsenseConfigurationLocator smartsenseConfigurationLocator;

    @Inject
    private SmartSenseSubscriptionService smartSenseSubscriptionService;

    public void capture(int caseId, AmbariClient ambariClient) {
        Optional<SmartSenseSubscription> smartSenseSubscription = smartSenseSubscriptionService.getDefault();
        if (smartsenseConfigurationLocator.smartsenseConfigurable(smartSenseSubscription)) {
            try {
                LOGGER.debug("Triggering SmartSense data capture.");
                ambariClient.smartSenseCapture(caseId);
            } catch (Exception e) {
                LOGGER.info("Triggering SmartSense capture is failed.", e);
            }
        }
    }
}
