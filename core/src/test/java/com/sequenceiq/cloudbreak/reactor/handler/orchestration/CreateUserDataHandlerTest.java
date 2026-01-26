package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.CreateUserDataFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.CreateUserDataRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.CreateUserDataSuccess;
import com.sequenceiq.cloudbreak.service.encryption.EncryptionKeyService;
import com.sequenceiq.cloudbreak.service.encryption.UserdataSecretsService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.idbroker.IdBrokerService;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;

@ExtendWith(MockitoExtension.class)
class CreateUserDataHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String ENVIRONMENT_CRN = "enivronment-crn";

    private static final CloudContext CLOUD_CONTEXT = CloudContext.Builder.builder().build();

    private static final CloudCredential CLOUD_CREDENTIAL = new CloudCredential();

    @Mock
    private EventBus eventBus;

    @Mock
    private UserDataService userDataService;

    @Mock
    private IdBrokerService idBrokerService;

    @Mock
    private StackService stackService;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private UserdataSecretsService userdataSecretsService;

    @Mock
    private EncryptionKeyService encryptionKeyService;

    @Mock
    private Stack stack;

    @InjectMocks
    private CreateUserDataHandler underTest;

    @Captor
    private ArgumentCaptor<Event<CreateUserDataSuccess>> successEventCaptor;

    @Captor
    private ArgumentCaptor<Event<CreateUserDataFailed>> failedEventCaptor;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(CreateUserDataRequest.class), underTest.selector());
    }

    @Test
    void testAccept() {
        DetailedEnvironmentResponse environment = DetailedEnvironmentResponse.builder()
                .withEnableSecretEncryption(true)
                .build();
        InstanceMetaData imd1 = new InstanceMetaData();
        InstanceMetaData imd2 = new InstanceMetaData();
        imd1.setUserdataSecretResourceId(1L);
        imd2.setPrivateId(2L);
        Resource resource = new Resource();
        resource.setResourceReference("resource-reference");
        when(stack.getTerminatedAndNonTerminatedInstanceMetaDataAsList()).thenReturn(List.of(imd1, imd2));
        when(stackService.findEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(userdataSecretsService.createUserdataSecrets(stack, List.of(2L), CLOUD_CONTEXT, CLOUD_CREDENTIAL)).thenReturn(List.of(resource));

        underTest.accept(new Event<>(new CreateUserDataRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(idBrokerService).generateIdBrokerSignKey(STACK_ID);
        verify(userDataService).createUserData(STACK_ID);
        verify(userdataSecretsService).assignSecretsToInstances(stack, List.of(resource), List.of(imd2));
        verify(encryptionKeyService).updateCloudSecretManagerEncryptionKeyAccess(stack, CLOUD_CONTEXT, CLOUD_CREDENTIAL, List.of("resource-reference"),
                List.of());
        verify(eventBus).notify(eq(EventSelectorUtil.selector(CreateUserDataSuccess.class)), successEventCaptor.capture());
        assertEquals(STACK_ID, successEventCaptor.getValue().getData().getResourceId());
    }

    @Test
    void testAcceptSecretEncryptionNotEnabled() {
        when(stackService.findEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(new DetailedEnvironmentResponse());

        underTest.accept(new Event<>(new CreateUserDataRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(idBrokerService).generateIdBrokerSignKey(STACK_ID);
        verify(userDataService).createUserData(STACK_ID);
        verify(stackService, never()).getByIdWithListsInTransaction(any());
        verifyNoInteractions(userdataSecretsService);
        verifyNoInteractions(encryptionKeyService);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(CreateUserDataSuccess.class)), successEventCaptor.capture());
        assertEquals(STACK_ID, successEventCaptor.getValue().getData().getResourceId());
    }

    @Test
    void testAcceptFailure() {
        Exception e = new RuntimeException("test");
        doThrow(e).when(idBrokerService).generateIdBrokerSignKey(STACK_ID);

        underTest.accept(new Event<>(new CreateUserDataRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(eventBus).notify(eq(EventSelectorUtil.selector(CreateUserDataFailed.class)), failedEventCaptor.capture());
        assertEquals(e, failedEventCaptor.getValue().getData().getException());
    }
}
