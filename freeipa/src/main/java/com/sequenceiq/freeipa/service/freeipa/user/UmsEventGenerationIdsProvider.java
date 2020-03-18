package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient.INTERNAL_ACTOR_CRN;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;

@Component
public class UmsEventGenerationIdsProvider {

    @VisibleForTesting
    enum EventMapping {
        LAST_ROLE_ASSIGNMENT_EVENT_ID("lastRoleAssignmentEventId", GetEventGenerationIdsResponse::getLastRoleAssignmentEventId),
        LAST_RESOURCE_ROLE_ASSIGNMENT_EVENT_ID("lastResourceRoleAssignmentEventId", GetEventGenerationIdsResponse::getLastResourceRoleAssignmentEventId),
        LAST_GROUP_MEMBERHSIP_CHANGED_EVENT_ID("lastGroupMembershipChangedEventId", GetEventGenerationIdsResponse::getLastGroupMembershipChangedEventId),
        LAST_ACTOR_DELETED_EVENT_ID("lastActorDeletedEventId", GetEventGenerationIdsResponse::getLastActorDeletedEventId),
        LAST_ACTOR_WORKLOAD_CREDENTIALS_CHANGED_EVENT_ID("lastActorWorkloadCredentialsChangedEventId",
                GetEventGenerationIdsResponse::getLastActorWorkloadCredentialsChangedEventId);

        private String eventName;

        private Function<GetEventGenerationIdsResponse, String> converter;

        EventMapping(String eventName, Function<GetEventGenerationIdsResponse, String> converter) {
            this.eventName = eventName;
            this.converter = converter;
        }

        public String getEventName() {
            return eventName;
        }

        public Function<GetEventGenerationIdsResponse, String> getConverter() {
            return converter;
        }
    }

    @Inject
    private GrpcUmsClient grpcUmsClient;

    public UmsEventGenerationIds getEventGenerationIds(String accountId, Optional<String> requestId) {
        GetEventGenerationIdsResponse response = grpcUmsClient.getEventGenerationIds(INTERNAL_ACTOR_CRN, accountId, requestId);

        UmsEventGenerationIds umsEventGenerationIds = new UmsEventGenerationIds();
        Map<String, String> eventGenerationIdsMap = new HashMap<>();
        for (EventMapping eventMapping : EventMapping.values()) {
            String eventId = eventMapping.converter.apply(response);
            if (!Strings.isNullOrEmpty(eventId)) {
                eventGenerationIdsMap.put(eventMapping.getEventName(), eventMapping.getConverter().apply(response));
            }
        }
        umsEventGenerationIds.setEventGenerationIds(eventGenerationIdsMap);

        return umsEventGenerationIds;
    }
}