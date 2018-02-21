package com.sequenceiq.cloudbreak.service.cluster.ambari;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.blueprint.SmartsenseConfigurationLocator;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;

@Service
public class AmbariSmartSenseCapturer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariSmartSenseCapturer.class);

    @Inject
    private SmartsenseConfigurationLocator smartsenseConfigurationLocator;

    @Inject
    private SmartSenseSubscriptionService smartSenseSubscriptionService;

    public void capture(int caseId, String blueprintText, AmbariClient ambariClient) {
        Optional<SmartSenseSubscription> smartSenseSubscription = smartSenseSubscriptionService.getDefault();
        if (smartsenseConfigurationLocator.smartsenseConfigurable(blueprintText, smartSenseSubscription)) {
            try {
                LOGGER.info("Triggering SmartSense data capture.");
                ambariClient.smartSenseCapture(caseId);
            } catch (Exception e) {
                LOGGER.error("Triggering SmartSense capture is failed.", e);
            }
        }
    }
}
