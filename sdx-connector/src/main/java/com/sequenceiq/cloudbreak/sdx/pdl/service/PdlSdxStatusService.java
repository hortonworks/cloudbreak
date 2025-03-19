package com.sequenceiq.cloudbreak.sdx.pdl.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxStatusService;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;

@Service
public class PdlSdxStatusService extends AbstractPdlSdxService implements PlatformAwareSdxStatusService<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdlSdxStatusService.class);

    @Override
    public Set<Pair<String, String>> listSdxCrnStatusPair(String environmentCrn) {
        Set<Pair<String, String>> result = new HashSet<>();
        if (isEnabled(environmentCrn)) {
            try {
                Environment environment = getPrivateEnvForPublicEnv(environmentCrn);
                result.add(Pair.of(environment.getCrn(), environment.getStatus()));
            } catch (RuntimeException exception) {
                LOGGER.error(String.format("Private datalake not found for environment. CRN: %s.", environmentCrn), exception);
                return Collections.emptySet();
            }
        }
        return result
                .stream()
                .filter(entry -> EnvironmentStatus.valueOf(entry.getValue()) == EnvironmentStatus.AVAILABLE)
                .collect(Collectors.toSet());
    }

    @Override
    public StatusCheckResult getAvailabilityStatusCheckResult(String status) {
        return (EnvironmentStatus.AVAILABLE == EnvironmentStatus.valueOf(status)) ? StatusCheckResult.AVAILABLE : StatusCheckResult.NOT_AVAILABLE;
    }
}