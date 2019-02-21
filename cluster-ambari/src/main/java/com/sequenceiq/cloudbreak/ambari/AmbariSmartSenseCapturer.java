package com.sequenceiq.cloudbreak.ambari;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;

@Service
public class AmbariSmartSenseCapturer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariSmartSenseCapturer.class);

    public void capture(int caseId, AmbariClient ambariClient) {
        try {
            LOGGER.debug("Triggering SmartSense data capture.");
            ambariClient.smartSenseCapture(caseId);
        } catch (Exception e) {
            LOGGER.info("Triggering SmartSense capture is failed.", e);
        }
    }
}
