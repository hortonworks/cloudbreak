package com.sequenceiq.freeipa.flow.freeipa.upscale.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleUpdateUserdataSecretsRequest;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleUpdateUserdataSecretsSuccess;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.encryption.CloudInformationDecorator;
import com.sequenceiq.freeipa.service.encryption.CloudInformationDecoratorProvider;
import com.sequenceiq.freeipa.service.encryption.EncryptionKeyService;
import com.sequenceiq.freeipa.service.secret.UserdataSecretsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class UpscaleUpdateUserdataSecretsHandlerTest {

    private static final long STACK_ID = 1L;

    private static final CloudContext CLOUD_CONTEXT = CloudContext.Builder.builder().build();

    private static final CloudCredential CLOUD_CREDENTIAL = new CloudCredential();

    private static final CredentialResponse CREDENTIAL_RESPONSE = new CredentialResponse();

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final DetailedEnvironmentResponse ENVIRONMENT = DetailedEnvironmentResponse.builder()
            .withCrn(ENVIRONMENT_CRN)
            .withEnableSecretEncryption(true)
            .withCredential(CREDENTIAL_RESPONSE)
            .build();

    @Mock
    private EventBus eventBus;

    @Mock
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Mock
    private StackService stackService;

    @Mock
    private UserdataSecretsService userdataSecretsService;

    @Mock
    private CloudInformationDecoratorProvider cloudInformationDecoratorProvider;

    @Mock
    private CloudInformationDecorator cloudInformationDecorator;

    @Mock
    private EncryptionKeyService encryptionKeyService;

    @InjectMocks
    private UpscaleUpdateUserdataSecretsHandler underTest;

    @Captor
    private ArgumentCaptor<List<InstanceMetaData>> instanceMetaDataListCaptor;

    @Captor
    private ArgumentCaptor<Event<UpscaleUpdateUserdataSecretsSuccess>> successEventCaptor;

    @Captor
    private ArgumentCaptor<Event<UpscaleFailureEvent>> failureEventCaptor;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(UpscaleUpdateUserdataSecretsRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("test");

        UpscaleFailureEvent result = (UpscaleFailureEvent) underTest.defaultFailureEvent(STACK_ID, e, new Event<>(
                new UpscaleUpdateUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL, List.of())));

        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(e, result.getException());
    }

    @Test
    void testAcceptEvent() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        InstanceGroup instanceGroup = new InstanceGroup();
        Set<InstanceMetaData> correctInstances = getInstances(2, true, true);
        Set<InstanceMetaData> instancesWithoutSecretId = getInstances(2, false, true);
        Set<InstanceMetaData> instancesWithoutId = getInstances(2, true, false);
        instanceGroup.setInstanceMetaData(Stream.of(correctInstances, instancesWithoutSecretId, instancesWithoutId).flatMap(Set::stream)
                .collect(Collectors.toSet()));
        stack.setInstanceGroups(Set.of(instanceGroup));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(cloudInformationDecoratorProvider.getForStack(stack)).thenReturn(cloudInformationDecorator);
        when(cachedEnvironmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(ENVIRONMENT);
        when(cloudInformationDecorator.getAuthorizedClientForLuksEncryptionKey(eq(stack), argThat((correctInstances::contains))))
                .thenReturn("secretArn");

        underTest.accept(new Event<>(new UpscaleUpdateUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL, Arrays.asList(0L, 1L))));

        verify(stackService).getByIdWithListsInTransaction(STACK_ID);
        verify(cachedEnvironmentClientService).getByCrn(ENVIRONMENT_CRN);
        verify(encryptionKeyService).updateLuksEncryptionKeyAccess(stack, CLOUD_CONTEXT, CLOUD_CREDENTIAL, List.of("secretArn", "secretArn"), List.of());
        verify(userdataSecretsService)
                .updateUserdataSecrets(eq(stack), instanceMetaDataListCaptor.capture(), eq(CREDENTIAL_RESPONSE), eq(CLOUD_CONTEXT), eq(CLOUD_CREDENTIAL));
        verify(eventBus).notify(eq(EventSelectorUtil.selector(UpscaleUpdateUserdataSecretsSuccess.class)), successEventCaptor.capture());
        assertEquals(STACK_ID, successEventCaptor.getValue().getData().getResourceId());
        assertThat(instanceMetaDataListCaptor.getValue()).hasSameElementsAs(correctInstances);
    }

    private static Set<InstanceMetaData> getInstances(int count, boolean withSecretId, boolean withId) {
        return IntStream.range(0, count)
                .boxed()
                .map(i -> {
                    InstanceMetaData instanceMetaData = new InstanceMetaData();
                    if (withId) {
                        instanceMetaData.setId(Long.valueOf(i));
                    }
                    instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
                    if (withSecretId) {
                        instanceMetaData.setUserdataSecretResourceId(Long.valueOf(i));
                    }
                    return instanceMetaData;
                })
                .collect(Collectors.toSet());
    }

    @Test
    void testAcceptFailureEvent() {
        RuntimeException e = new RuntimeException("test");
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenThrow(e);

        underTest.accept(new Event<>(new UpscaleUpdateUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL, List.of())));

        verify(eventBus).notify(eq(EventSelectorUtil.selector(UpscaleFailureEvent.class)), failureEventCaptor.capture());
        UpscaleFailureEvent result = failureEventCaptor.getValue().getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(e, result.getException());
    }
}
