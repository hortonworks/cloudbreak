package com.sequenceiq.cloudbreak.reactor.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.DeleteUserdataSecretsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.DeleteUserdataSecretsFinished;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.encryption.DeleteUserdataSecretsRequest;
import com.sequenceiq.cloudbreak.service.encryption.UserdataSecretsService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;

@ExtendWith(MockitoExtension.class)
class DeleteUserdataSecretsHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String ENVIRONMENT_CRN = "environment-crn";

    private static final CloudContext CLOUD_CONTEXT = CloudContext.Builder.builder().build();

    private static final CloudCredential CLOUD_CREDENTIAL = new CloudCredential();

    @Mock
    private EventBus eventBus;

    @Mock
    private StackService stackService;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private UserdataSecretsService userdataSecretsService;

    @InjectMocks
    private DeleteUserdataSecretsHandler underTest;

    @Captor
    private ArgumentCaptor<Event<DeleteUserdataSecretsFinished>> finishedEventCaptor;

    @Captor
    private ArgumentCaptor<Event<DeleteUserdataSecretsFailed>> failedEventCaptor;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(DeleteUserdataSecretsRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("test");
        DeleteUserdataSecretsFailed result = (DeleteUserdataSecretsFailed) underTest.defaultFailureEvent(STACK_ID, e,
                new Event<>(new DeleteUserdataSecretsRequest(STACK_ID, TerminationType.REGULAR, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        assertEquals(STACK_ID, result.getResourceId());
    }

    @Test
    void testAccept() {
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withEnableSecretEncryption(true)
                .build();
        Stack stack = new Stack();
        when(stackService.findEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        underTest.accept(new Event<>(new DeleteUserdataSecretsRequest(STACK_ID, TerminationType.REGULAR, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(userdataSecretsService).deleteUserdataSecretsForStack(stack, CLOUD_CONTEXT, CLOUD_CREDENTIAL);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(DeleteUserdataSecretsFinished.class)), finishedEventCaptor.capture());
        assertEquals(STACK_ID, finishedEventCaptor.getValue().getData().getResourceId());
    }

    @Test
    void testAcceptForceDelete() {
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withEnableSecretEncryption(true)
                .build();
        Stack stack = new Stack();
        RuntimeException e = new RuntimeException("test");
        when(stackService.findEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        doThrow(e).when(userdataSecretsService).deleteUserdataSecretsForStack(stack, CLOUD_CONTEXT, CLOUD_CREDENTIAL);

        underTest.accept(new Event<>(new DeleteUserdataSecretsRequest(STACK_ID, TerminationType.FORCED, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(eventBus).notify(eq(EventSelectorUtil.selector(DeleteUserdataSecretsFinished.class)), finishedEventCaptor.capture());
        assertEquals(STACK_ID, finishedEventCaptor.getValue().getData().getResourceId());
    }

    @Test
    void testAcceptSecretEncryptionNotEnabled() {
        when(stackService.findEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(new DetailedEnvironmentResponse());

        underTest.accept(new Event<>(new DeleteUserdataSecretsRequest(STACK_ID, TerminationType.REGULAR, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(stackService, never()).getByIdWithLists(anyLong());
        verifyNoInteractions(userdataSecretsService);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(DeleteUserdataSecretsFinished.class)), finishedEventCaptor.capture());
        assertEquals(STACK_ID, finishedEventCaptor.getValue().getData().getResourceId());
    }

    @Test
    void testAcceptFailureEvent() {
        RuntimeException e = new RuntimeException("test");
        doThrow(e).when(stackService).findEnvironmentCrnByStackId(any());

        underTest.accept(new Event<>(new DeleteUserdataSecretsRequest(STACK_ID, TerminationType.REGULAR, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));
        verify(eventBus).notify(eq(EventSelectorUtil.selector(DeleteUserdataSecretsFailed.class)), failedEventCaptor.capture());
        assertEquals(e, failedEventCaptor.getValue().getData().getException());
    }
}
