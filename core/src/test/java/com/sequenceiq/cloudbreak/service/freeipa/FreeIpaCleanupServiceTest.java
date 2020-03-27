package com.sequenceiq.cloudbreak.service.freeipa;

import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;

@RunWith(MockitoJUnitRunner.class)
public class FreeIpaCleanupServiceTest {

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String STACK_NAME = "stack name";

    private static final String CLOUD_PLATFORM = "cloudPlatform";

    @Mock
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @Mock
    private PollingService<FreeIpaOperationPollerObject> freeIpaOperationChecker;

    @Mock
    private OperationV1Endpoint operationV1Endpoint;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private KerberosDetailService kerberosDetailService;

    @Mock
    private EnvironmentConfigProvider environmentConfigProvider;

    @InjectMocks
    private FreeIpaCleanupService victim;

    @Test
    public void shouldSendCleanupRequestInCaseOfKeytabNeedsToBeUpdated() throws Exception {
        Stack stack = aStack();
        Optional<KerberosConfig> kerberosConfig = Optional.of(mock(KerberosConfig.class));
        Pair<PollingResult, Exception> pollingResultExceptionPair = new ImmutablePair<>(PollingResult.SUCCESS, null);
        OperationStatus operationStatus = new OperationStatus(null, OperationType.CLEANUP, null, null, null, null, 0L, null);

        when(kerberosConfigService.get(ENVIRONMENT_CRN, STACK_NAME)).thenReturn(kerberosConfig);
        when(environmentConfigProvider.isChildEnvironment(ENVIRONMENT_CRN)).thenReturn(true);
        when(kerberosDetailService.keytabsShouldBeUpdated(CLOUD_PLATFORM, true, kerberosConfig)).thenReturn(true);
        when(freeIpaV1Endpoint.cleanup(any(CleanupRequest.class))).thenReturn(operationStatus);
        when(freeIpaOperationChecker.pollWithAbsoluteTimeout(any(), any(), anyLong(), anyLong(), anyInt())).thenReturn(pollingResultExceptionPair);

        victim.cleanup(stack, false, false, emptySet(), emptySet());

        verify(freeIpaV1Endpoint).cleanup(any());
    }

    @Test
    public void shouldNotSendCleanupRequestInCaseOfKeytabDoesNotNeedToBeUpdated() throws Exception {
        Stack stack = aStack();
        Optional<KerberosConfig> kerberosConfig = Optional.of(mock(KerberosConfig.class));

        when(kerberosConfigService.get(ENVIRONMENT_CRN, STACK_NAME)).thenReturn(kerberosConfig);
        when(environmentConfigProvider.isChildEnvironment(ENVIRONMENT_CRN)).thenReturn(true);
        when(kerberosDetailService.keytabsShouldBeUpdated(CLOUD_PLATFORM, true, kerberosConfig)).thenReturn(false);

        victim.cleanup(stack, false, false, emptySet(), emptySet());

        verifyNoMoreInteractions(freeIpaV1Endpoint);
    }

    private Stack aStack() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setName(STACK_NAME);
        stack.setCloudPlatform(CLOUD_PLATFORM);
        return stack;
    }
}