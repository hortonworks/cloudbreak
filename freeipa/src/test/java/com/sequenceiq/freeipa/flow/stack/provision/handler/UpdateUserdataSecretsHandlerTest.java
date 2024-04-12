package com.sequenceiq.freeipa.flow.stack.provision.handler;

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
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.UpdateUserdataSecretsFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.UpdateUserdataSecretsRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.UpdateUserdataSecretsSuccess;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.secret.UserdataSecretsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class UpdateUserdataSecretsHandlerTest {

    private static final long STACK_ID = 1L;

    private static final CloudContext CLOUD_CONTEXT = CloudContext.Builder.builder().build();

    private static final CloudCredential CLOUD_CREDENTIAL = new CloudCredential();

    private static final CredentialResponse CREDENTIAL = new CredentialResponse();

    private static final DetailedEnvironmentResponse ENVIRONMENT = DetailedEnvironmentResponse.builder()
            .withCrn("environmentCrn")
            .withEnableSecretEncryption(true)
            .withCredential(CREDENTIAL)
            .build();

    @Mock
    private EventBus eventBus;

    @Mock
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Mock
    private StackService stackService;

    @Mock
    private UserdataSecretsService userdataSecretsService;

    @InjectMocks
    private UpdateUserdataSecretsHandler underTest;

    @Captor
    private ArgumentCaptor<Event<UpdateUserdataSecretsSuccess>> successEventCaptor;

    @Captor
    private ArgumentCaptor<Event<UpdateUserdataSecretsFailed>> failureEventCaptor;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(UpdateUserdataSecretsRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("test");
        UpdateUserdataSecretsFailed result = (UpdateUserdataSecretsFailed) underTest
                .defaultFailureEvent(STACK_ID, e, new Event<>(new UpdateUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(e, result.getException());
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
        when(stackService.getEnvironmentCrnByStackId(STACK_ID)).thenReturn("environmentCrn");
        when(cachedEnvironmentClientService.getByCrn("environmentCrn")).thenReturn(ENVIRONMENT);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        underTest.accept(new Event<>(new UpdateUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(userdataSecretsService).updateUserdataSecrets(stack, List.of(imd1), CREDENTIAL, CLOUD_CONTEXT, CLOUD_CREDENTIAL);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(UpdateUserdataSecretsSuccess.class)), successEventCaptor.capture());
        assertEquals(STACK_ID, successEventCaptor.getValue().getData().getResourceId());
    }

    @Test
    void testAcceptFailureEvent() {
        RuntimeException e = new RuntimeException("test");
        doThrow(e).when(stackService).getEnvironmentCrnByStackId(any());

        underTest.accept(new Event<>(new UpdateUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(eventBus).notify(eq(EventSelectorUtil.selector(UpdateUserdataSecretsFailed.class)), failureEventCaptor.capture());
        UpdateUserdataSecretsFailed result = failureEventCaptor.getValue().getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(e, result.getException());
    }
}
