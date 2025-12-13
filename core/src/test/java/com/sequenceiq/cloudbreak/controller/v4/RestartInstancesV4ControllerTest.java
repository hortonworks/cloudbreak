package com.sequenceiq.cloudbreak.controller.v4;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.service.StackCommonService;

@ExtendWith(MockitoExtension.class)
class RestartInstancesV4ControllerTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String STACK_CRN = "crn:cdp:datahub:us-west-1:cloudbreak:datahub:guid";

    private static final String STACK_NAME = "name";

    private static final List<String> INSTANCE_IDS = List.of("i-1", "i-2");

    @Mock
    private StackCommonService stackCommonService;

    @InjectMocks
    private RestartInstancesV4Controller underTest;

    @Test
    void testInvocationStackCommonServiceRestartMultipleInstancesWithCrn() {
        try (MockedStatic<ThreadBasedUserCrnProvider> utilities = mockStatic(ThreadBasedUserCrnProvider.class)) {
            utilities.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);
            underTest.restartInstancesForClusterCrn(STACK_CRN, INSTANCE_IDS);
            verify(stackCommonService, times(1)).restartMultipleInstances(NameOrCrn.ofCrn(STACK_CRN), ACCOUNT_ID, INSTANCE_IDS);
        }
    }

    @Test
    void testInvocationStackCommonServiceRestartMultipleInstancesWithName() {
        try (MockedStatic<ThreadBasedUserCrnProvider> utilities = mockStatic(ThreadBasedUserCrnProvider.class)) {
            utilities.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);
            underTest.restartInstancesForClusterName(STACK_NAME, INSTANCE_IDS);
            verify(stackCommonService, times(1)).restartMultipleInstances(NameOrCrn.ofName(STACK_NAME), ACCOUNT_ID, INSTANCE_IDS);
        }
    }
}