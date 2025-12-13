package com.sequenceiq.cloudbreak.reactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.DownscaleRemoveUserdataSecretsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.DownscaleRemoveUserdataSecretsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.DownscaleRemoveUserdataSecretsSuccess;
import com.sequenceiq.cloudbreak.service.encryption.CloudInformationDecorator;
import com.sequenceiq.cloudbreak.service.encryption.CloudInformationDecoratorProvider;
import com.sequenceiq.cloudbreak.service.encryption.EncryptionKeyService;
import com.sequenceiq.cloudbreak.service.encryption.UserdataSecretsService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;

@ExtendWith(MockitoExtension.class)
class DownscaleRemoveUserdataSecretsHandlerTest {

    private static final long STACK_ID = 1L;

    private static final CloudContext CLOUD_CONTEXT = CloudContext.Builder.builder().build();

    private static final CloudCredential CLOUD_CREDENTIAL = new CloudCredential();

    private static final Long INVALID_PRIVATE_ID = -1L;

    @Mock
    private EventBus eventBus;

    @Mock
    private StackService stackService;

    @Mock
    private CloudInformationDecoratorProvider cloudInformationDecoratorProvider;

    @Mock
    private CloudInformationDecorator cloudInformationDecorator;

    @Mock
    private ResourceService resourceService;

    @Mock
    private EncryptionKeyService encryptionKeyService;

    @Mock
    private UserdataSecretsService userdataSecretsService;

    @InjectMocks
    private DownscaleRemoveUserdataSecretsHandler underTest;

    @Captor
    private ArgumentCaptor<List<InstanceMetaData>> instanceMetaDataListCaptor;

    @Captor
    private ArgumentCaptor<Event<DownscaleRemoveUserdataSecretsSuccess>> successEventCaptor;

    @Captor
    private ArgumentCaptor<Event<DownscaleRemoveUserdataSecretsFailed>> failureEventCaptor;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(DownscaleRemoveUserdataSecretsRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("test");

        DownscaleRemoveUserdataSecretsFailed result = (DownscaleRemoveUserdataSecretsFailed) underTest.defaultFailureEvent(STACK_ID, e, new Event<>(
                new DownscaleRemoveUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL, List.of())));

        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(e, result.getException());
    }

    @Test
    void testAccept() {
        Stack stack = new Stack();
        stack.setName("stackName");
        InstanceGroup instanceGroup = new InstanceGroup();
        Set<InstanceMetaData> correctInstances = getInstances(2, true, true);
        Set<InstanceMetaData> instancesWithoutSecretId = getInstances(2, false, true);
        Set<InstanceMetaData> instancesNotInDownscaleHosts = getInstances(2, true, false);
        instanceGroup.setInstanceMetaData(Stream.of(correctInstances, instancesWithoutSecretId, instancesNotInDownscaleHosts).
                flatMap(Set::stream)
                .collect(Collectors.toSet()));
        stack.setInstanceGroups(Set.of(instanceGroup));
        List<Resource> secretResources = getResources(2);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(cloudInformationDecoratorProvider.getForStack(stack)).thenReturn(cloudInformationDecorator);
        when(cloudInformationDecorator.getAuthorizedClientForLuksEncryptionKey(eq(stack), argThat(correctInstances::contains)))
                .thenReturn("secretArn");
        when(resourceService.findAllByResourceId(argThat(iterable -> StreamSupport.stream(iterable.spliterator(), false)
                .toList().containsAll(List.of(0L, 1L))))).thenReturn(secretResources);

        underTest.accept(new Event<>(new DownscaleRemoveUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL, List.of(0L, 1L))));

        verify(stackService).getByIdWithListsInTransaction(STACK_ID);
        verify(encryptionKeyService).updateCloudSecretManagerEncryptionKeyAccess(stack, CLOUD_CONTEXT, CLOUD_CREDENTIAL,
                List.of(), List.of("secret-0", "secret-1"));
        verify(encryptionKeyService).updateLuksEncryptionKeyAccess(stack, CLOUD_CONTEXT, CLOUD_CREDENTIAL, List.of(), List.of("secretArn", "secretArn"));
        verify(userdataSecretsService).deleteUserdataSecretsForInstances(instanceMetaDataListCaptor.capture(), eq(CLOUD_CONTEXT), eq(CLOUD_CREDENTIAL));
        verify(eventBus).notify(eq(EventSelectorUtil.selector(DownscaleRemoveUserdataSecretsSuccess.class)), successEventCaptor.capture());
        assertEquals(STACK_ID, successEventCaptor.getValue().getData().getResourceId());
        assertThat(instanceMetaDataListCaptor.getValue()).hasSameElementsAs(correctInstances);
    }

    private static Set<InstanceMetaData> getInstances(int count, boolean withSecretId, boolean withPrivateId) {
        return LongStream.range(0, count)
                .boxed()
                .map(i -> {
                    InstanceMetaData instanceMetaData = new InstanceMetaData();
                    if (withSecretId) {
                        instanceMetaData.setUserdataSecretResourceId(i);
                    }
                    if (withPrivateId) {
                        instanceMetaData.setPrivateId(i);
                    } else {
                        instanceMetaData.setPrivateId(INVALID_PRIVATE_ID);
                    }
                    return instanceMetaData;
                })
                .collect(Collectors.toSet());
    }

    private static List<Resource> getResources(int count) {
        return LongStream.range(0, count)
                .boxed()
                .map(i -> {
                    Resource resource = new Resource();
                    resource.setId(i);
                    resource.setResourceReference("secret-" + i);
                    return resource;
                })
                .toList();
    }

    @Test
    void testAcceptFailureEvent() {
        RuntimeException e = new RuntimeException("test");
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenThrow(e);

        underTest.accept(new Event<>(new DownscaleRemoveUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL, List.of())));

        verify(eventBus).notify(eq(EventSelectorUtil.selector(DownscaleRemoveUserdataSecretsFailed.class)), failureEventCaptor.capture());
        DownscaleRemoveUserdataSecretsFailed result = failureEventCaptor.getValue().getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(e, result.getException());
    }
}
