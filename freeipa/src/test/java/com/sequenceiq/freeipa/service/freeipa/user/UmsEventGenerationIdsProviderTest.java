package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

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

    @Mock
    GrpcUmsClient grpcUmsClient;

    @InjectMocks
    UmsEventGenerationIdsProvider underTest;

    @Test
    void testGetEventGenerationIds() {
        GetEventGenerationIdsResponse response = createGetEventGenerationIdsResponse();
        when(grpcUmsClient.getEventGenerationIds(any(), any(), any())).thenReturn(response);

        UmsEventGenerationIds umsEventGenerationIds = underTest.getEventGenerationIds(ACCOUNT_ID, Optional.of(UUID.randomUUID().toString()));

        for (UmsEventGenerationIdsProvider.EventMapping eventMapping : UmsEventGenerationIdsProvider.EventMapping.values()) {
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