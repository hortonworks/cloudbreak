package com.sequenceiq.freeipa.flow.stack.termination.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

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
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.termination.event.secret.DeleteUserdataSecretsFailed;
import com.sequenceiq.freeipa.flow.stack.termination.event.secret.DeleteUserdataSecretsFinished;
import com.sequenceiq.freeipa.flow.stack.termination.event.secret.DeleteUserdataSecretsRequest;
import com.sequenceiq.freeipa.service.secret.UserdataSecretsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class DeleteUserdataSecretsHandlerTest {

    private static final long STACK_ID = 1L;

    private static final CloudContext CLOUD_CONTEXT = CloudContext.Builder.builder().build();

    private static final CloudCredential CLOUD_CREDENTIAL = new CloudCredential();

    @Mock
    private EventBus eventBus;

    @Mock
    private StackService stackService;

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
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData imd1 = new InstanceMetaData();
        InstanceMetaData imd2 = new InstanceMetaData();
        imd1.setUserdataSecretResourceId(1L);
        instanceGroup.setInstanceMetaData(Set.of(imd1, imd2));
        stack.setInstanceGroups(Set.of(instanceGroup));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        underTest.accept(new Event<>(new DeleteUserdataSecretsRequest(STACK_ID, false, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(userdataSecretsService).deleteUserdataSecretsForInstances(List.of(imd1), CLOUD_CONTEXT, CLOUD_CREDENTIAL);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(DeleteUserdataSecretsFinished.class)), successEventCaptor.capture());
        DeleteUserdataSecretsFinished result = successEventCaptor.getValue().getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(false, result.getForced());
    }

    @Test
    void testAcceptWhenForcedAndProviderThrowsException() {
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData imd1 = new InstanceMetaData();
        imd1.setUserdataSecretResourceId(1L);
        instanceGroup.setInstanceMetaData(Set.of(imd1));
        stack.setInstanceGroups(Set.of(instanceGroup));
        RuntimeException e = new RuntimeException("unauthorized");
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        doThrow(e).when(userdataSecretsService).deleteUserdataSecretsForInstances(List.of(imd1), CLOUD_CONTEXT, CLOUD_CREDENTIAL);

        underTest.accept(new Event<>(new DeleteUserdataSecretsRequest(STACK_ID, true, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(userdataSecretsService).deleteUserdataSecretsForInstances(List.of(imd1), CLOUD_CONTEXT, CLOUD_CREDENTIAL);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(DeleteUserdataSecretsFinished.class)), successEventCaptor.capture());
        DeleteUserdataSecretsFinished result = successEventCaptor.getValue().getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(true, result.getForced());
    }

    @Test
    void testAcceptFailureEvent() {
        RuntimeException e = new RuntimeException("test");
        doThrow(e).when(stackService).getByIdWithListsInTransaction(any());

        underTest.accept(new Event<>(new DeleteUserdataSecretsRequest(STACK_ID, false, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(eventBus).notify(eq(EventSelectorUtil.selector(DeleteUserdataSecretsFailed.class)), failureEventCaptor.capture());
        DeleteUserdataSecretsFailed result = failureEventCaptor.getValue().getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(e, result.getException());
    }
}
