package com.sequenceiq.freeipa.flow.stack.termination.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.termination.event.secret.DeleteUserdataSecretsFailed;
import com.sequenceiq.freeipa.flow.stack.termination.event.secret.DeleteUserdataSecretsFinished;
import com.sequenceiq.freeipa.flow.stack.termination.event.secret.DeleteUserdataSecretsRequest;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.secret.UserdataSecretsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class DeleteUserdataSecretsHandlerTest {

    private static final long STACK_ID = 1L;

    private static final CloudContext CLOUD_CONTEXT = CloudContext.Builder.builder().build();

    private static final CloudCredential CLOUD_CREDENTIAL = new CloudCredential();

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final DetailedEnvironmentResponse ENVIRONMENT = DetailedEnvironmentResponse.builder()
            .withCrn(ENVIRONMENT_CRN)
            .withEnableSecretEncryption(true)
            .build();

    @Mock
    private EventBus eventBus;

    @Mock
    private StackService stackService;

    @Mock
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Mock
    private UserdataSecretsService userdataSecretsService;

    @InjectMocks
    private DeleteUserdataSecretsHandler underTest;

    @Captor
    private ArgumentCaptor<Event<DeleteUserdataSecretsFinished>> successEventCaptor;

    @Captor
    private ArgumentCaptor<Event<DeleteUserdataSecretsFailed>> failureEventCaptor;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(DeleteUserdataSecretsRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("test");
        DeleteUserdataSecretsFailed result = (DeleteUserdataSecretsFailed) underTest.defaultFailureEvent(STACK_ID, e,
                new Event<>(new DeleteUserdataSecretsRequest(STACK_ID, false, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        assertEquals(STACK_ID, result.getResourceId());
    }

    @Test
    void testAccept() {
        Stack stack = new Stack();
        when(stackService.getEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);
        when(cachedEnvironmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(ENVIRONMENT);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        underTest.accept(new Event<>(new DeleteUserdataSecretsRequest(STACK_ID, false, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(userdataSecretsService).deleteUserdataSecretsForStack(stack, CLOUD_CONTEXT, CLOUD_CREDENTIAL);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(DeleteUserdataSecretsFinished.class)), successEventCaptor.capture());
        DeleteUserdataSecretsFinished result = successEventCaptor.getValue().getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(false, result.getForced());
    }

    @Test
    void testAcceptWhenSecretEncryptionNotEnabled() {
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withEnableSecretEncryption(false)
                .build();
        when(stackService.getEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);
        when(cachedEnvironmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);

        underTest.accept(new Event<>(new DeleteUserdataSecretsRequest(STACK_ID, false, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(userdataSecretsService, never()).deleteUserdataSecretsForInstances(anyList(), any(), any());
        verify(eventBus).notify(eq(EventSelectorUtil.selector(DeleteUserdataSecretsFinished.class)), successEventCaptor.capture());
        DeleteUserdataSecretsFinished result = successEventCaptor.getValue().getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(false, result.getForced());
    }

    @Test
    void testAcceptWhenForcedAndProviderThrowsException() {
        Stack stack = new Stack();
        RuntimeException e = new RuntimeException("unauthorized");
        when(stackService.getEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);
        when(cachedEnvironmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(ENVIRONMENT);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        doThrow(e).when(userdataSecretsService).deleteUserdataSecretsForStack(stack, CLOUD_CONTEXT, CLOUD_CREDENTIAL);

        underTest.accept(new Event<>(new DeleteUserdataSecretsRequest(STACK_ID, true, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(userdataSecretsService).deleteUserdataSecretsForStack(stack, CLOUD_CONTEXT, CLOUD_CREDENTIAL);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(DeleteUserdataSecretsFinished.class)), successEventCaptor.capture());
        DeleteUserdataSecretsFinished result = successEventCaptor.getValue().getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(true, result.getForced());
    }

    @Test
    void testAcceptFailureEvent() {
        RuntimeException e = new RuntimeException("test");
        doThrow(e).when(stackService).getEnvironmentCrnByStackId(any());

        underTest.accept(new Event<>(new DeleteUserdataSecretsRequest(STACK_ID, false, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(eventBus).notify(eq(EventSelectorUtil.selector(DeleteUserdataSecretsFailed.class)), failureEventCaptor.capture());
        DeleteUserdataSecretsFailed result = failureEventCaptor.getValue().getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(e, result.getException());
    }
}
