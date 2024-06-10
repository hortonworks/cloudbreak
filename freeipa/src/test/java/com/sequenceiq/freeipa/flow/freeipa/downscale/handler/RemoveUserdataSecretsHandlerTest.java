package com.sequenceiq.freeipa.flow.freeipa.downscale.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.DownscaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.userdatasecrets.RemoveUserdataSecretsRequest;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.userdatasecrets.RemoveUserdataSecretsSuccess;
import com.sequenceiq.freeipa.service.secret.UserdataSecretsService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class RemoveUserdataSecretsHandlerTest {

    private static final long STACK_ID = 1L;

    private static final CloudContext CLOUD_CONTEXT = CloudContext.Builder.builder().build();

    private static final CloudCredential CLOUD_CREDENTIAL = new CloudCredential();

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    @Mock
    private EventBus eventBus;

    @Mock
    private StackService stackService;

    @Mock
    private UserdataSecretsService userdataSecretsService;

    @InjectMocks
    private RemoveUserdataSecretsHandler underTest;

    @Captor
    private ArgumentCaptor<List<InstanceMetaData>> instanceMetaDataListCaptor;

    @Captor
    private ArgumentCaptor<Event<RemoveUserdataSecretsSuccess>> successEventCaptor;

    @Captor
    private ArgumentCaptor<Event<DownscaleFailureEvent>> failureEventCaptor;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(RemoveUserdataSecretsRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("test");

        DownscaleFailureEvent result = (DownscaleFailureEvent) underTest.defaultFailureEvent(STACK_ID, e, new Event<>(
                new RemoveUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL, List.of())));

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
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        underTest.accept(new Event<>(new RemoveUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL, List.of("fqdn-0", "fqdn-1"))));

        verify(stackService).getByIdWithListsInTransaction(STACK_ID);
        verify(userdataSecretsService).deleteUserdataSecretsForInstances(instanceMetaDataListCaptor.capture(), eq(CLOUD_CONTEXT), eq(CLOUD_CREDENTIAL));
        verify(eventBus).notify(eq(EventSelectorUtil.selector(RemoveUserdataSecretsSuccess.class)), successEventCaptor.capture());
        assertEquals(STACK_ID, successEventCaptor.getValue().getData().getResourceId());
        assertThat(instanceMetaDataListCaptor.getValue()).hasSameElementsAs(correctInstances);
    }

    private static Set<InstanceMetaData> getInstances(int count, boolean withSecretId, boolean withValidFqdn) {
        return IntStream.range(0, count)
                .boxed()
                .map(i -> {
                    InstanceMetaData instanceMetaData = new InstanceMetaData();
                    if (withSecretId) {
                        instanceMetaData.setUserdataSecretResourceId(Long.valueOf(i));
                    }
                    if (withValidFqdn) {
                        instanceMetaData.setDiscoveryFQDN("fqdn-" + i);
                    } else {
                        instanceMetaData.setDiscoveryFQDN("invalid");
                    }
                    return instanceMetaData;
                })
                .collect(Collectors.toSet());
    }

    @Test
    void testAcceptFailureEvent() {
        RuntimeException e = new RuntimeException("test");
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenThrow(e);

        underTest.accept(new Event<>(new RemoveUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL, List.of())));

        verify(eventBus).notify(eq(EventSelectorUtil.selector(DownscaleFailureEvent.class)), failureEventCaptor.capture());
        DownscaleFailureEvent result = failureEventCaptor.getValue().getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(e, result.getException());
    }
}
