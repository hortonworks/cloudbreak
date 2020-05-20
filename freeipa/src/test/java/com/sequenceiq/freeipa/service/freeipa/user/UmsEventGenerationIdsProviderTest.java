package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetEventGenerationIdsResponse;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;

@ExtendWith(MockitoExtension.class)
class UmsEventGenerationIdsProviderTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    // The UmsEventGenerationIdsProvider will include all fields from the response in the eventGenerationIds map.
    // Test for known fields at the time of implementation. This enum should be updated when the GetEventGenerationIdsResponse
    // is changed
    enum EventMapping {
        LAST_ROLE_ASSIGNMENT_EVENT_ID("lastRoleAssignmentEventId", GetEventGenerationIdsResponse::getLastRoleAssignmentEventId),
        LAST_RESOURCE_ROLE_ASSIGNMENT_EVENT_ID("lastResourceRoleAssignmentEventId", GetEventGenerationIdsResponse::getLastResourceRoleAssignmentEventId),
        LAST_GROUP_MEMBERSHIP_CHANGED_EVENT_ID("lastGroupMembershipChangedEventId", GetEventGenerationIdsResponse::getLastGroupMembershipChangedEventId),
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

    @Mock
    GrpcUmsClient grpcUmsClient;

    @InjectMocks
    UmsEventGenerationIdsProvider underTest;

    @Test
    void testGetEventGenerationIds() {
        GetEventGenerationIdsResponse response = createGetEventGenerationIdsResponse();
        when(grpcUmsClient.getEventGenerationIds(any(), any(), any())).thenReturn(response);

        UmsEventGenerationIds umsEventGenerationIds = underTest.getEventGenerationIds(ACCOUNT_ID, Optional.of(UUID.randomUUID().toString()));

        for (EventMapping eventMapping : EventMapping.values()) {
            assertEquals(eventMapping.getConverter().apply(response), umsEventGenerationIds.getEventGenerationIds().get(eventMapping.getEventName()));
        }
    }

    GetEventGenerationIdsResponse createGetEventGenerationIdsResponse() {

        GetEventGenerationIdsResponse.Builder builder = GetEventGenerationIdsResponse.newBuilder();

        Arrays.stream(GetEventGenerationIdsResponse.Builder.class.getMethods())
                .filter(m -> m.getName().startsWith("set"))
                .filter(m -> !m.getName().endsWith("Bytes"))
                .filter(m -> !m.getName().endsWith("Field"))
                .filter(m -> !m.getName().endsWith("Fields"))
                .forEach(m -> {
                    try {
                        m.invoke(builder, UUID.randomUUID().toString());
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        fail("Could not build GetEventGenerationIdsResponse.", e);
                    }
                });

        return builder.build();
    }
}