package com.sequenceiq.freeipa.converter.usersync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.usersync.UserSyncStatusResponse;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;

class UserSyncStatusToUserSyncStatusResponseConverterTest {

    private UserSyncStatusToUserSyncStatusResponseConverter underTest = new UserSyncStatusToUserSyncStatusResponseConverter();

    @Test
    void convertJsonException() throws IOException {
        Json eventGenerationIds = mock(Json.class);
        IOException e = new IOException("test exception");
        when(eventGenerationIds.get(any(Class.class))).thenThrow(e);

        UserSyncStatus status = new UserSyncStatus();
        status.setStack(mock(Stack.class));
        status.setUmsEventGenerationIds(eventGenerationIds);

        UserSyncStatusResponse response = underTest.convert(status);

        assertThat(response)
                .returns(null, UserSyncStatusResponse::getLastStartedUserSyncId)
                .returns(null, UserSyncStatusResponse::getLastSuccessfulUserSyncId)
                .returns(null, UserSyncStatusResponse::getEventGenerationIds);
    }

    @Test
    void convert() {
        String requestedId = UUID.randomUUID().toString();
        Operation requested = new Operation();
        requested.setOperationId(requestedId);

        String successfulId = UUID.randomUUID().toString();
        Operation successful = new Operation();
        successful.setOperationId(successfulId);

        Map<String, String> eventGenerationIdMap = Map.of(
                "key1", "value1",
                "key2", "value2"
        );
        UmsEventGenerationIds eventGenerationIds = new UmsEventGenerationIds();
        eventGenerationIds.setEventGenerationIds(eventGenerationIdMap);

        UserSyncStatus status = new UserSyncStatus();
        status.setLastStartedFullSync(requested);
        status.setLastSuccessfulFullSync(successful);
        status.setUmsEventGenerationIds(new Json(eventGenerationIds));

        UserSyncStatusResponse response = underTest.convert(status);

        assertThat(response)
                .returns(requestedId, UserSyncStatusResponse::getLastStartedUserSyncId)
                .returns(successfulId, UserSyncStatusResponse::getLastSuccessfulUserSyncId)
                .returns(eventGenerationIdMap, UserSyncStatusResponse::getEventGenerationIds);
    }
}
