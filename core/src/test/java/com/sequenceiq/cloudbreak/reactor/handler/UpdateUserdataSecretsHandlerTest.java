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

import java.util.List;

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
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpdateUserdataSecretsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpdateUserdataSecretsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpdateUserdataSecretsSuccess;
import com.sequenceiq.cloudbreak.service.encryption.CloudInformationDecorator;
import com.sequenceiq.cloudbreak.service.encryption.CloudInformationDecoratorProvider;
import com.sequenceiq.cloudbreak.service.encryption.EncryptionKeyService;
import com.sequenceiq.cloudbreak.service.encryption.UserdataSecretsService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;

@ExtendWith(MockitoExtension.class)
class UpdateUserdataSecretsHandlerTest {

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

    @Mock
    private CloudInformationDecoratorProvider cloudInformationDecoratorProvider;

    @Mock
    private CloudInformationDecorator cloudInformationDecorator;

    @Mock
    private EncryptionKeyService encryptionKeyService;

    @Mock
    private Stack stack;

    @InjectMocks
    private UpdateUserdataSecretsHandler underTest;

    @Captor
    private ArgumentCaptor<Event<UpdateUserdataSecretsSuccess>> successEventCaptor;

    @Captor
    private ArgumentCaptor<Event<UpdateUserdataSecretsFailed>> failedEventCaptor;

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
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withEnableSecretEncryption(true)
                .build();
        InstanceMetaData imd1 = new InstanceMetaData();
        InstanceMetaData imd2 = new InstanceMetaData();
        imd1.setUserdataSecretResourceId(1L);
        when(stackService.findEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(cloudInformationDecoratorProvider.getForStack(stack)).thenReturn(cloudInformationDecorator);
        when(stack.getNotDeletedAndNotZombieInstanceMetaDataList()).thenReturn(List.of(imd1, imd2));
        when(cloudInformationDecorator.getAuthorizedClientForLuksEncryptionKey(stack, imd1)).thenReturn("instance-arn");

        underTest.accept(new Event<>(new UpdateUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(encryptionKeyService).updateLuksEncryptionKeyAccess(stack, CLOUD_CONTEXT, CLOUD_CREDENTIAL, List.of("instance-arn"), List.of());
        verify(userdataSecretsService).updateUserdataSecrets(stack, List.of(imd1), environment, CLOUD_CONTEXT, CLOUD_CREDENTIAL);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(UpdateUserdataSecretsSuccess.class)), successEventCaptor.capture());
        assertEquals(STACK_ID, successEventCaptor.getValue().getData().getResourceId());
    }

    @Test
    void testAcceptSecretEncryptionNotEnabled() {
        when(stackService.findEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(new DetailedEnvironmentResponse());

        underTest.accept(new Event<>(new UpdateUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(stackService, never()).getByIdWithListsInTransaction(anyLong());
        verifyNoInteractions(encryptionKeyService);
        verifyNoInteractions(userdataSecretsService);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(UpdateUserdataSecretsSuccess.class)), successEventCaptor.capture());
        assertEquals(STACK_ID, successEventCaptor.getValue().getData().getResourceId());
    }

    @Test
    void testAcceptFailureEvent() {
        RuntimeException e = new RuntimeException("test");
        doThrow(e).when(stackService).findEnvironmentCrnByStackId(any());

        underTest.accept(new Event<>(new UpdateUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(eventBus).notify(eq(EventSelectorUtil.selector(UpdateUserdataSecretsFailed.class)), failedEventCaptor.capture());
        UpdateUserdataSecretsFailed result = failedEventCaptor.getValue().getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(e, result.getException());
    }
}
