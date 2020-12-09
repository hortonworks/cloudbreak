package com.sequenceiq.freeipa.service.freeipa.user.ums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;

@ExtendWith(MockitoExtension.class)
class UmsUsersStateProviderDispatcherTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final Set<String> ENVIRONMENT_CRNS = Set.of(
            CrnTestUtil.getEnvironmentCrnBuilder()
                    .setAccountId(ACCOUNT_ID)
                    .setResource(UUID.randomUUID().toString())
                    .build()
                    .toString()
    );

    @Mock
    private BulkUmsUsersStateProvider bulkUmsUsersStateProvider;

    @InjectMocks
    private UmsUsersStateProviderDispatcher underTest;

    @Test
    void testFullSync() {
        Map<String, UmsUsersState> expected = createExpectedResponse();
        when(bulkUmsUsersStateProvider.get(anyString(), any(Set.class), any(Optional.class)))
                .thenReturn(expected);

        Optional<String> requestIdOptional = Optional.of(UUID.randomUUID().toString());
        Map<String, UmsUsersState> response = underTest.getEnvToUmsUsersStateMap(
                ACCOUNT_ID, ENVIRONMENT_CRNS, requestIdOptional);

        assertEquals(expected, response);
        verify(bulkUmsUsersStateProvider).get(ACCOUNT_ID, ENVIRONMENT_CRNS, requestIdOptional);
    }

    private Map<String, UmsUsersState> createExpectedResponse() {
        return ENVIRONMENT_CRNS.stream()
                .collect(Collectors.toMap(Function.identity(),
                        env -> UmsUsersState.newBuilder()
                                .setUsersState(UsersState.newBuilder().build())
                                .build()));
    }
}