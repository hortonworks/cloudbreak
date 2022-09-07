package com.sequenceiq.cloudbreak.telemetry.databus;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.telemetry.TelemetryPillarConfigGenerator;
import com.sequenceiq.cloudbreak.telemetry.UMSSecretKeyFormatter;
import com.sequenceiq.cloudbreak.telemetry.context.DatabusContext;
import com.sequenceiq.cloudbreak.telemetry.context.TelemetryContext;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;

@Service
public class DatabusConfigService implements TelemetryPillarConfigGenerator<DatabusConfigView> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabusConfigService.class);

    private static final String ED25519 = "Ed25519";

    private static final String SALT_STATE = "databus";

    @Override
    public DatabusConfigView createConfigs(TelemetryContext context) {
        final DatabusContext databusContext = context.getDatabusContext();
        final DataBusCredential dataBusCredential = databusContext.getCredential();
        String accessKeySecretAlgorithm = StringUtils.defaultIfBlank(dataBusCredential.getAccessKeyType(), ED25519);
        return new DatabusConfigView.Builder()
                .withEnabled(databusContext.isEnabled())
                .withEndpoint(databusContext.getEndpoint())
                .withAccessKeyId(dataBusCredential.getAccessKey())
                .withAccessKeySecret(UMSSecretKeyFormatter.formatSecretKey(accessKeySecretAlgorithm, dataBusCredential.getPrivateKey()).toCharArray())
                .withAccessKeySecretAlgorithm(accessKeySecretAlgorithm)
                .build();
    }

    @Override
    public boolean isEnabled(TelemetryContext context) {
        final DatabusContext databusContext = context.getDatabusContext();
        boolean databusIsSet = databusContext.isEnabled() && databusContext.getCredential() != null;
        boolean validDbusSettings = false;
        if (databusIsSet) {
            final DataBusCredential credential = databusContext.getCredential();
            boolean validKeyPair = credential.isValid();
            validDbusSettings = validKeyPair && StringUtils.isNotBlank(databusContext.getEndpoint());
            if (!validDbusSettings) {
                LOGGER.debug("Although metering or cluster deployment log reporting is enabled, databus "
                                + "credentials/endpoint is not provided properly. endpoint: {}, accessKey: {}, privateKey: {}",
                        databusContext.getEndpoint(), credential.getAccessKey(), validKeyPair ? "*****" : "<empty>");
            }
        } else {
            LOGGER.debug("Databus related settings are not filled. Skip databus usage.");
        }
        return databusIsSet && validDbusSettings;
    }

    @Override
    public String saltStateName() {
        return SALT_STATE;
    }
}
