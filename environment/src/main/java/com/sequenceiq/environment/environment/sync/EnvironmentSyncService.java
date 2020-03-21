package com.sequenceiq.environment.environment.sync;

import static com.sequenceiq.environment.environment.EnvironmentStatus.ENV_STOPPED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.FREEIPA_DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.environment.environment.EnvironmentStatus.START_FREEIPA_FAILED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.START_FREEIPA_STARTED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.STOP_FREEIPA_FAILED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.STOP_FREEIPA_STARTED;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Component
public class EnvironmentSyncService {

    private final FreeIpaService freeIpaService;

    public EnvironmentSyncService(FreeIpaService freeIpaService) {
        this.freeIpaService = freeIpaService;
    }

    public EnvironmentStatus getStatusByFreeipa(Environment environment) {
        Optional<DescribeFreeIpaResponse> freeIpaResponseOpt = freeIpaService.describe(environment.getResourceCrn());
        if (freeIpaResponseOpt.isPresent()) {
            DescribeFreeIpaResponse freeIpaResponse = freeIpaResponseOpt.get();
            switch (freeIpaResponse.getStatus()) {
                case STOPPED:
                    return ENV_STOPPED;
                case DELETED_ON_PROVIDER_SIDE:
                    return FREEIPA_DELETED_ON_PROVIDER_SIDE;
                case STOP_FAILED:
                    return STOP_FREEIPA_FAILED;
                case START_FAILED:
                    return START_FREEIPA_FAILED;
                case START_IN_PROGRESS:
                    return START_FREEIPA_STARTED;
                case STOP_IN_PROGRESS:
                    return STOP_FREEIPA_STARTED;
                default:
                    break;
            }
        } else if (environment.isCreateFreeIpa()) {
            return FREEIPA_DELETED_ON_PROVIDER_SIDE;
        }
        return EnvironmentStatus.AVAILABLE;
    }
}
