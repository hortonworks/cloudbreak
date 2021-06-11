package com.sequenceiq.freeipa.service.freeipa.user.ums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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

import com.sequenceiq.cloudbreak.auth.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersState;

import io.grpc.StatusRuntimeException;

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
    private DefaultUmsUsersStateProvider defaultUmsUsersStateProvider;

    @Mock
    private BulkUmsUsersStateProvider bulkUmsUsersStateProvider;

    @InjectMocks
    private UmsUsersStateProviderDispatcher underTest;

    @Test
    void testFullSync() {
        Set<String> userCrns = Set.of();
        Set<String> machineUserCrns = Set.of();

        Map<String, UmsUsersState> expected = createExpectedResponse();
        when(bulkUmsUsersStateProvider.get(anyString(), any(Set.class), any(Optional.class)))
                .thenReturn(expected);

        Optional<String> requestIdOptional = Optional.of(UUID.randomUUID().toString());
        Map<String, UmsUsersState> response = underTest.getEnvToUmsUsersStateMap(
                ACCOUNT_ID, ENVIRONMENT_CRNS,
                userCrns, machineUserCrns, requestIdOptional);

        assertEquals(expected, response);
        verify(bulkUmsUsersStateProvider).get(ACCOUNT_ID, ENVIRONMENT_CRNS, requestIdOptional);
        verify(defaultUmsUsersStateProvider, never()).get(anyString(), any(Set.class),
                any(Set.class), any(Set.class), any(Optional.class), anyBoolean());
    }

    @Test
    void testBulkFallsBackToDefault() {
        Set<String> userCrns = Set.of();
        Set<String> machineUserCrns = Set.of();

        when(bulkUmsUsersStateProvider.get(anyString(), any(Set.class), any(Optional.class)))
                .thenThrow(StatusRuntimeException.class);
        Map<String, UmsUsersState> expected = createExpectedResponse();
        when(defaultUmsUsersStateProvider.get(anyString(), any(Set.class),
                any(Set.class), any(Set.class), any(Optional.class), anyBoolean()))
                .thenReturn(expected);

        Optional<String> requestIdOptional = Optional.of(UUID.randomUUID().toString());
        Map<String, UmsUsersState> response = underTest.getEnvToUmsUsersStateMap(
                ACCOUNT_ID, ENVIRONMENT_CRNS,
                userCrns, machineUserCrns, requestIdOptional);

        assertEquals(expected, response);
        verify(bulkUmsUsersStateProvider).get(ACCOUNT_ID, ENVIRONMENT_CRNS, requestIdOptional);
        verify(defaultUmsUsersStateProvider).get(ACCOUNT_ID, ENVIRONMENT_CRNS,
                userCrns, machineUserCrns, requestIdOptional, true);
    }

    @Test
    void testPartialSync() {
        Set<String> userCrns = Set.of(createActorCrn(CrnResourceDescriptor.USER));
        Set<String> machineUserCrns = Set.of(createActorCrn(CrnResourceDescriptor.MACHINE_USER));

        Map<String, UmsUsersState> expected = createExpectedResponse();
        when(defaultUmsUsersStateProvider.get(anyString(), any(Set.class),
                any(Set.class), any(Set.class), any(Optional.class), anyBoolean()))
                .thenReturn(expected);

        Optional<String> requestIdOptional = Optional.of(UUID.randomUUID().toString());
        Map<String, UmsUsersState> response = underTest.getEnvToUmsUsersStateMap(
                ACCOUNT_ID, ENVIRONMENT_CRNS,
                userCrns, machineUserCrns, requestIdOptional);

        assertEquals(expected, response);
        verify(bulkUmsUsersStateProvider, never()).get(ACCOUNT_ID, ENVIRONMENT_CRNS, requestIdOptional);
        verify(defaultUmsUsersStateProvider).get(ACCOUNT_ID, ENVIRONMENT_CRNS,
                userCrns, machineUserCrns, requestIdOptional, false);
    }

    private Map<String, UmsUsersState> createExpectedResponse() {
        return ENVIRONMENT_CRNS.stream()
                .collect(Collectors.toMap(Function.identity(),
                        env -> UmsUsersState.newBuilder()
                                .setUsersState(UsersState.newBuilder().build())
                                .build()));
    }

    private String createActorCrn(CrnResourceDescriptor resourceDescriptor) {
        return Crn.builder()
                .setPartition(Crn.Partition.CDP)
                .setRegion(Crn.Region.US_WEST_1)
                .setService(resourceDescriptor.getServiceType())
                .setResourceType(resourceDescriptor.getResourceType())
                .setAccountId(ACCOUNT_ID)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }
}