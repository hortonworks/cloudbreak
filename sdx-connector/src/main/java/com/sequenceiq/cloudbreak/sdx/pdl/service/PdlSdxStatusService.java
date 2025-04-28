package com.sequenceiq.cloudbreak.sdx.pdl.service;

import static com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails.StatusEnum.AVAILABLE;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails.StatusEnum;
import com.sequenceiq.cloudbreak.sdx.common.service.PlatformAwareSdxStatusService;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;

@Service
public class PdlSdxStatusService extends AbstractPdlSdxService implements PlatformAwareSdxStatusService<PrivateDatalakeDetails.StatusEnum> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdlSdxStatusService.class);

    @Override
    public Set<Pair<String, StatusEnum>> listSdxCrnStatusPair(String environmentCrn) {
        Set<Pair<String, StatusEnum>> result = new HashSet<>();
        if (isEnabled(environmentCrn)) {
            try {
                Environment environment = getPrivateEnvForPublicEnv(environmentCrn);
                if (environment != null && environment.getPvcEnvironmentDetails() != null
                        && environment.getPvcEnvironmentDetails().getPrivateDatalakeDetails() != null) {
                    result.add(Pair.of(environment.getCrn(), environment.getPvcEnvironmentDetails().getPrivateDatalakeDetails().getStatus()));
                }
            } catch (RuntimeException exception) {
                LOGGER.error(String.format("Private datalake not found for environment. CRN: %s.", environmentCrn), exception);
                return Collections.emptySet();
            }
        }
        return result;
    }

    @Override
    public StatusCheckResult getAvailabilityStatusCheckResult(StatusEnum status) {
        return (AVAILABLE == status) ? StatusCheckResult.AVAILABLE : StatusCheckResult.NOT_AVAILABLE;
    }
}