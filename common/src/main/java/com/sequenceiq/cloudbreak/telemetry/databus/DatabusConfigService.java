package com.sequenceiq.cloudbreak.telemetry.databus;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DatabusConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabusConfigService.class);

    public DatabusConfigView createDatabusConfigs(String accessKeyId,
            char[] accessKeySecret, String secretAlgorithm, String endpoint) {
        final DatabusConfigView.Builder builder = new DatabusConfigView.Builder();
        boolean validAccessKeySecret = accessKeySecret != null && StringUtils.isNotEmpty(new String(accessKeySecret));
        boolean enableDbus = validAccessKeySecret && StringUtils.isNoneEmpty(endpoint, new String(accessKeySecret));
        if (enableDbus) {
            builder.withEndpoint(endpoint)
                    .withAccessKeyId(accessKeyId)
                    .withAccessKeySecret(accessKeySecret)
                    .withAccessKeySecretAlgorithm(secretAlgorithm);
        } else {
            LOGGER.debug("Although metering or cluster deployment log reporting is enabled, databus "
                            + "credentials/endpoint is not provided properly. endpoint: {}, accessKey: {}, privateKey: {}",
                    endpoint, accessKeyId, validAccessKeySecret ? "*****" : "<empty>");
        }
        return builder
                .withEnabled(enableDbus)
                .build();
    }
}
