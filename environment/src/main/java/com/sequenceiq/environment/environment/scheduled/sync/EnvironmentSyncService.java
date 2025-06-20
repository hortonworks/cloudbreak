package com.sequenceiq.environment.environment.scheduled.sync;

import static com.sequenceiq.environment.environment.EnvironmentStatus.ENV_STOPPED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.FREEIPA_CREATION_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.FREEIPA_DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.environment.environment.EnvironmentStatus.FREEIPA_DELETE_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.FREEIPA_REBUILD_FAILED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.FREEIPA_REBUILD_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.FREEIPA_STALE;
import static com.sequenceiq.environment.environment.EnvironmentStatus.FREEIPA_UNHEALTHY;
import static com.sequenceiq.environment.environment.EnvironmentStatus.FREEIPA_UNREACHABLE;
import static com.sequenceiq.environment.environment.EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_FREEIPA_FAILED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.PROXY_CONFIG_MODIFICATION_ON_FREEIPA_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.START_FREEIPA_FAILED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.START_FREEIPA_STARTED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.START_SYNCHRONIZE_USERS_STARTED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.STOP_FREEIPA_FAILED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.STOP_FREEIPA_STARTED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.UPDATE_INITIATED;
import static com.sequenceiq.environment.environment.EnvironmentStatus.UPGRADE_CCM_ON_FREEIPA_IN_PROGRESS;
import static com.sequenceiq.environment.environment.EnvironmentStatus.UPGRADE_DEFAULT_OUTBOUND_ON_FREEIPA_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.CREATE_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.CREATE_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETED_ON_PROVIDER_SIDE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DIAGNOSTICS_COLLECTION_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DOWNSCALE_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.MAINTENANCE_MODE_ENABLED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.MODIFY_PROXY_CONFIG_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.MODIFY_PROXY_CONFIG_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.MODIFY_PROXY_CONFIG_REQUESTED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.REBUILD_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.REBUILD_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.REPAIR_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.REQUESTED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STACK_AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STALE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.START_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.START_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.START_REQUESTED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOPPED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOP_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOP_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOP_REQUESTED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.TRUST_SETUP_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.TRUST_SETUP_FINISH_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.TRUST_SETUP_FINISH_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.TRUST_SETUP_FINISH_REQUIRED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.TRUST_SETUP_FINISH_SUCCESSFUL;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.TRUST_SETUP_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.UNHEALTHY;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.UNKNOWN;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.UNREACHABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.UPDATE_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.UPDATE_REQUESTED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.UPGRADE_CCM_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.UPGRADE_CCM_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.UPGRADE_CCM_REQUESTED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.UPGRADE_DEFAULT_OUTBOUND_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.UPGRADE_DEFAULT_OUTBOUND_IN_PROGRESS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.UPGRADE_DEFAULT_OUTBOUND_REQUESTED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.UPGRADE_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.UPSCALE_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.VERTICAL_SCALE_FAILED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.WAIT_FOR_SYNC;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Component
public class EnvironmentSyncService {

    private static final Map<Status, EnvironmentStatus> FREEIPA_STATUS_TO_ENV_STATUS_MAP = Map.ofEntries(
            Map.entry(REQUESTED, FREEIPA_CREATION_IN_PROGRESS),
            Map.entry(CREATE_IN_PROGRESS, FREEIPA_CREATION_IN_PROGRESS),
            Map.entry(AVAILABLE, EnvironmentStatus.AVAILABLE),
            Map.entry(STACK_AVAILABLE, FREEIPA_CREATION_IN_PROGRESS),
            Map.entry(DIAGNOSTICS_COLLECTION_IN_PROGRESS, EnvironmentStatus.AVAILABLE),
            Map.entry(UPDATE_IN_PROGRESS, UPDATE_INITIATED),
            Map.entry(UPDATE_REQUESTED, UPDATE_INITIATED),
            Map.entry(UPDATE_FAILED, EnvironmentStatus.UPDATE_FAILED),
            Map.entry(UPSCALE_FAILED, EnvironmentStatus.AVAILABLE),
            Map.entry(DOWNSCALE_FAILED, EnvironmentStatus.AVAILABLE),
            Map.entry(VERTICAL_SCALE_FAILED, EnvironmentStatus.AVAILABLE),
            Map.entry(REPAIR_FAILED, FREEIPA_UNHEALTHY),
            Map.entry(CREATE_FAILED, EnvironmentStatus.CREATE_FAILED),
            Map.entry(DELETE_IN_PROGRESS, FREEIPA_DELETE_IN_PROGRESS),
            Map.entry(DELETE_FAILED, EnvironmentStatus.DELETE_FAILED),
            Map.entry(DELETE_COMPLETED, FREEIPA_DELETE_IN_PROGRESS),
            Map.entry(STOPPED, ENV_STOPPED),
            Map.entry(STOP_REQUESTED, STOP_FREEIPA_STARTED),
            Map.entry(START_REQUESTED, START_FREEIPA_STARTED),
            Map.entry(STOP_IN_PROGRESS, STOP_FREEIPA_STARTED),
            Map.entry(START_IN_PROGRESS, START_FREEIPA_STARTED),
            Map.entry(START_FAILED, START_FREEIPA_FAILED),
            Map.entry(STOP_FAILED, STOP_FREEIPA_FAILED),
            Map.entry(WAIT_FOR_SYNC, START_SYNCHRONIZE_USERS_STARTED),
            Map.entry(MAINTENANCE_MODE_ENABLED, EnvironmentStatus.AVAILABLE),
            Map.entry(DELETED_ON_PROVIDER_SIDE, FREEIPA_DELETED_ON_PROVIDER_SIDE),
            Map.entry(UPGRADE_CCM_REQUESTED, UPGRADE_CCM_ON_FREEIPA_IN_PROGRESS),
            Map.entry(UPGRADE_CCM_IN_PROGRESS, UPGRADE_CCM_ON_FREEIPA_IN_PROGRESS),
            Map.entry(MODIFY_PROXY_CONFIG_REQUESTED, PROXY_CONFIG_MODIFICATION_ON_FREEIPA_IN_PROGRESS),
            Map.entry(MODIFY_PROXY_CONFIG_IN_PROGRESS, PROXY_CONFIG_MODIFICATION_ON_FREEIPA_IN_PROGRESS),
            Map.entry(MODIFY_PROXY_CONFIG_FAILED, PROXY_CONFIG_MODIFICATION_ON_FREEIPA_FAILED),
            Map.entry(UPGRADE_CCM_FAILED, EnvironmentStatus.AVAILABLE),
            Map.entry(UPGRADE_DEFAULT_OUTBOUND_REQUESTED, UPGRADE_DEFAULT_OUTBOUND_ON_FREEIPA_IN_PROGRESS),
            Map.entry(UPGRADE_DEFAULT_OUTBOUND_IN_PROGRESS, UPGRADE_DEFAULT_OUTBOUND_ON_FREEIPA_IN_PROGRESS),
            Map.entry(UPGRADE_DEFAULT_OUTBOUND_FAILED, EnvironmentStatus.AVAILABLE),
            Map.entry(UNREACHABLE, FREEIPA_UNREACHABLE),
            Map.entry(UNHEALTHY, FREEIPA_UNHEALTHY),
            Map.entry(UNKNOWN, FREEIPA_UNHEALTHY),
            Map.entry(UPGRADE_FAILED, EnvironmentStatus.AVAILABLE),
            Map.entry(REBUILD_IN_PROGRESS, FREEIPA_REBUILD_IN_PROGRESS),
            Map.entry(REBUILD_FAILED, FREEIPA_REBUILD_FAILED),
            Map.entry(STALE, FREEIPA_STALE),
            Map.entry(TRUST_SETUP_IN_PROGRESS, EnvironmentStatus.TRUST_SETUP_IN_PROGRESS),
            Map.entry(TRUST_SETUP_FINISH_REQUIRED, EnvironmentStatus.TRUST_SETUP_FINISH_REQUIRED),
            Map.entry(TRUST_SETUP_FAILED, EnvironmentStatus.TRUST_SETUP_FAILED),
            Map.entry(TRUST_SETUP_FINISH_IN_PROGRESS, EnvironmentStatus.TRUST_SETUP_FINISH_IN_PROGRESS),
            Map.entry(TRUST_SETUP_FINISH_FAILED, EnvironmentStatus.TRUST_SETUP_FINISH_FAILED),
            Map.entry(TRUST_SETUP_FINISH_SUCCESSFUL, EnvironmentStatus.AVAILABLE)
    );

    private final FreeIpaService freeIpaService;

    public EnvironmentSyncService(FreeIpaService freeIpaService) {
        this.freeIpaService = freeIpaService;
    }

    public EnvironmentStatus getStatusByFreeipa(Environment environment) {
        Optional<DescribeFreeIpaResponse> freeIpaResponseOpt = freeIpaService.internalDescribe(environment.getResourceCrn(), environment.getAccountId());
        if (isHybridEnvironment(environment)) {
            return environment.getStatus();
        } else {
            if (freeIpaResponseOpt.isPresent()) {
                return FREEIPA_STATUS_TO_ENV_STATUS_MAP.get(freeIpaResponseOpt.get().getStatus());
            } else if (environment.isCreateFreeIpa()) {
                return FREEIPA_DELETED_ON_PROVIDER_SIDE;
            }
            return EnvironmentStatus.AVAILABLE;
        }

    }

    private boolean isHybridEnvironment(Environment environment) {
        return environment.getEnvironmentType() != null && environment.getEnvironmentType().isHybrid();
    }

    @VisibleForTesting
    Map<Status, EnvironmentStatus> getStatusMap() {
        return FREEIPA_STATUS_TO_ENV_STATUS_MAP;
    }
}
