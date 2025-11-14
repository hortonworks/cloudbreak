package com.sequenceiq.cloudbreak.orchestrator.salt;

import static com.sequenceiq.cloudbreak.common.json.JsonUtil.isValid;
import static com.sequenceiq.cloudbreak.common.json.JsonUtil.readValue;
import static java.time.Instant.ofEpochSecond;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusFromFileResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusWithTimestamp;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

@Service
public class SaltSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltSyncService.class);

    private static final int PROXY_TIMEOUT_MS = 15000;

    private static final int CONNECT_TIMEOUT_MS = 5000;

    private static final int READ_TIMEOUT_MS = 15000;

    private static final Duration TIMESTAMP_DIFFERENCE_THRESHOLD = Duration.ofMinutes(10);

    private static final String SALT_CHECK_JSON_LOCATION = "/opt/salt-check-result.json";

    @Inject
    private SaltStateService saltStateService;

    @Inject
    private SaltService saltService;

    @Measure(SaltSyncService.class)
    public Optional<Set<String>> checkSaltMinions(GatewayConfig gatewayConfig) {
        try (SaltConnector sc = saltService.createSaltConnectorWithCustomTimeout(gatewayConfig, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, PROXY_TIMEOUT_MS)) {
            MinionStatusFromFileResponse response = saltStateService.collectNodeStatusWithLimitedRetry(sc, SALT_CHECK_JSON_LOCATION);
            Map<String, String> resultMap = response.getResult().iterator().next();
            if (resultMap.containsKey(sc.getHostname())) {
                String resultString = resultMap.get(sc.getHostname());
                if (isValid(resultString)) {
                    try {
                        MinionStatusWithTimestamp minionResponse = readValue(resultString, MinionStatusWithTimestamp.class);
                        Instant responseTimestamp = ofEpochSecond(minionResponse.getTimestamp());
                        Instant now = Instant.now();
                        if (!responseTimestamp.isBefore(now.minus(TIMESTAMP_DIFFERENCE_THRESHOLD)) && !emptyIfNull(minionResponse.getDown()).isEmpty()) {
                            return Optional.of(new HashSet<>(minionResponse.getDown()));
                        }
                    } catch (IOException e) {
                        LOGGER.debug("JSON format of salt status check file is not valid , skipping status check!");
                    }
                }
            } else {
                LOGGER.error("Result map of salt sync call does not contain key for salt master host, " +
                        "please double check the implementation to review this anomaly.");
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred during check of salt check result json file: ", e);
            return Optional.of(Set.of(gatewayConfig.getHostname()));
        }
        return Optional.empty();
    }
}
