package com.sequenceiq.redbeams.events.notification;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.notification.NotificationState;
import com.sequenceiq.notification.domain.NotificationGroupType;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

/**
 * Resolves the {@link NotificationState} of the Data Hub / Data Lake cluster that owns an external database, by calling the owning service
 * (core {@link StackV4Endpoint} or datalake {@link SdxEndpoint}) by cluster CRN. The service is chosen from the CRN via
 * {@link NotificationGroupType#byCrn}. redbeams background poller threads have no logged-in user, so the remote call runs as the internal actor.
 *
 * <p>A CRN that is neither a data hub nor a data lake, or any lookup failure, is treated as {@link NotificationState#DISABLED} so the email is
 * skipped rather than sent in error.</p>
 */
@Service
public class DBStackOwnerIdentifierService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBStackOwnerIdentifierService.class);

    private static final long INTERNAL_WORKSPACE_ID = 0L;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private SdxEndpoint sdxEndpoint;

    public DBSourceClusterDto getOwningClusterNotificationState(String clusterCrn) {
        NotificationGroupType groupType = NotificationGroupType.byCrn(clusterCrn);
        try {
            return ThreadBasedUserCrnProvider.doAsInternalActor(() -> queryNotificationState(groupType, clusterCrn));
        } catch (RuntimeException e) {
            LOGGER.warn("Could not resolve notification state for owning cluster {}: {}. Treating it as DISABLED.", clusterCrn, e.getMessage(), e);
            return new DBSourceClusterDto(
                    clusterCrn,
                    clusterCrn,
                    StackType.LEGACY,
                    NotificationState.DISABLED
            );
        }
    }

    private DBSourceClusterDto queryNotificationState(NotificationGroupType groupType, String clusterCrn) {
        return switch (groupType) {
            case DATAHUB ->  {
                StackV4Response stackV4Response = stackV4Endpoint.getByCrn(INTERNAL_WORKSPACE_ID, clusterCrn, Set.of());
                yield new DBSourceClusterDto(
                        stackV4Response.getName(),
                        stackV4Response.getCrn(),
                        StackType.WORKLOAD,
                        stackV4Response.getNotificationState()
                );
            }
            case DATALAKE -> {
                SdxClusterResponse sdxClusterResponse = sdxEndpoint.getByCrn(clusterCrn);
                yield new DBSourceClusterDto(
                        sdxClusterResponse.getName(),
                        sdxClusterResponse.getCrn(),
                        StackType.DATALAKE,
                        sdxClusterResponse.getNotificationState()
                );
            }
            default -> {
                LOGGER.info("Cluster CRN {} is neither a data hub nor a data lake ({}); treating notification state as DISABLED.", clusterCrn, groupType);
                yield new DBSourceClusterDto(
                        clusterCrn,
                        clusterCrn,
                        StackType.LEGACY,
                        NotificationState.DISABLED
                );
            }
        };
    }
}
