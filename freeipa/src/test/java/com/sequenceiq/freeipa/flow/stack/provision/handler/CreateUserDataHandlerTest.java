package com.sequenceiq.freeipa.flow.stack.provision.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.userdata.CreateUserDataSuccess;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.encryption.EncryptionKeyService;
import com.sequenceiq.freeipa.service.image.userdata.UserDataService;
import com.sequenceiq.freeipa.service.secret.UserdataSecretsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class CreateUserDataHandlerTest {

    private static final long STACK_ID = 1L;

    private static final CloudContext CLOUD_CONTEXT = CloudContext.Builder.builder().build();

    private static final CloudCredential CLOUD_CREDENTIAL = new CloudCredential();

    @Mock
    private EventBus eventBus;

    @Mock
    private UserDataService userDataService;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Mock
    private StackService stackService;

    @Mock
    private UserdataSecretsService userdataSecretsService;

    @Mock
    private EncryptionKeyService encryptionKeyService;

    @InjectMocks
    private CreateUserDataHandler underTest;

    @Captor
    private ArgumentCaptor<Event<CreateUserDataSuccess>> successEventCaptor;

    @Captor
    private ArgumentCaptor<Event<CreateUserDataFailed>> failureEventCaptor;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(CreateUserDataRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("asdf");
        CreateUserDataFailed result = (CreateUserDataFailed) underTest
                .defaultFailureEvent(STACK_ID, e, new Event<>(new CreateUserDataRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(e, result.getException());
    }

    @Test
    void testAccept() throws TransactionService.TransactionExecutionException {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setEnableSecretEncryption(true);
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData imd1 = new InstanceMetaData();
        InstanceMetaData imd2 = new InstanceMetaData();
        imd1.setUserdataSecretResourceId(1L);
        imd2.setPrivateId(2L);
        instanceGroup.setInstanceMetaData(Set.of(imd1, imd2));
        stack.setInstanceGroups(Set.of(instanceGroup));
        Resource r = new Resource();
        r.setResourceReference("secretArn");
        List<Resource> secretResources = List.of(r);
        when(stackService.getEnvironmentCrnByStackId(STACK_ID)).thenReturn("environmentCrn");
        when(cachedEnvironmentClientService.getByCrn("environmentCrn")).thenReturn(environment);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(userdataSecretsService.createUserdataSecrets(any(), anyList(), any(), any())).thenReturn(secretResources);

        underTest.accept(new Event<>(new CreateUserDataRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(securityConfigService).initSaltSecurityConfigs(STACK_ID);
        verify(userDataService).createUserData(STACK_ID);
        verify(userdataSecretsService).createUserdataSecrets(stack, List.of(2L), CLOUD_CONTEXT, CLOUD_CREDENTIAL);
        verify(userdataSecretsService).assignSecretsToInstances(stack, List.of(r), List.of(imd2));
        verify(encryptionKeyService).updateCloudSecretManagerEncryptionKeyAccess(stack, CLOUD_CONTEXT, CLOUD_CREDENTIAL, List.of("secretArn"), List.of());
        verify(eventBus).notify(eq(EventSelectorUtil.selector(CreateUserDataSuccess.class)), successEventCaptor.capture());
        assertEquals(STACK_ID, successEventCaptor.getValue().getData().getResourceId());
    }

    @Test
    void testAcceptSecretEncryptionNotEnabled() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        when(stackService.getEnvironmentCrnByStackId(STACK_ID)).thenReturn("environmentCrn");
        when(cachedEnvironmentClientService.getByCrn("environmentCrn")).thenReturn(environment);

        underTest.accept(new Event<>(new CreateUserDataRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(stackService, never()).getByIdWithListsInTransaction(anyLong());
        verify(userdataSecretsService, never()).createUserdataSecrets(any(), anyList(), any(), any());
        verify(userdataSecretsService, never()).assignSecretsToInstances(any(), anyList(), anyList());
        verify(encryptionKeyService, never()).updateLuksEncryptionKeyAccess(any(), any(), any(), anyList(), anyList());
    }

    @Test
    void testAcceptFailure() throws TransactionService.TransactionExecutionException {
        Exception exception = new RuntimeException("asdf");
        doThrow(exception).when(securityConfigService).initSaltSecurityConfigs(STACK_ID);

        underTest.accept(new Event<>(new CreateUserDataRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL)));

        verify(eventBus).notify(eq(EventSelectorUtil.selector(CreateUserDataFailed.class)), failureEventCaptor.capture());
        CreateUserDataFailed result = failureEventCaptor.getValue().getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(exception, result.getException());
    }
}