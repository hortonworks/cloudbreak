package com.sequenceiq.cloudbreak.reactor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

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
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpscaleCreateUserdataSecretsFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpscaleCreateUserdataSecretsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UpscaleCreateUserdataSecretsSuccess;
import com.sequenceiq.cloudbreak.service.encryption.EncryptionKeyService;
import com.sequenceiq.cloudbreak.service.encryption.UserdataSecretsService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;

@ExtendWith(MockitoExtension.class)
class UpscaleCreateUserdataSecretsHandlerTest {

    private static final long STACK_ID = 1L;

    private static final CloudContext CLOUD_CONTEXT = CloudContext.Builder.builder().build();

    private static final CloudCredential CLOUD_CREDENTIAL = new CloudCredential();

    private static final int NUMBER_OF_SECRETS = 2;

    @Mock
    private EventBus eventBus;

    @Mock
    private StackService stackService;

    @Mock
    private UserdataSecretsService userdataSecretsService;

    @Mock
    private EncryptionKeyService encryptionKeyService;

    @InjectMocks
    private UpscaleCreateUserdataSecretsHandler underTest;

    @Captor
    private ArgumentCaptor<Event<UpscaleCreateUserdataSecretsSuccess>> successEventCaptor;

    @Captor
    private ArgumentCaptor<Event<UpscaleCreateUserdataSecretsFailed>> failureEventCaptor;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(UpscaleCreateUserdataSecretsRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("test");

        UpscaleCreateUserdataSecretsFailed result = (UpscaleCreateUserdataSecretsFailed) underTest.defaultFailureEvent(STACK_ID, e,
                new Event<>(new UpscaleCreateUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL, List.of())));

        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(e, result.getException());
    }

    @Test
    void testAccept() {
        Stack stack = new Stack();
        List<Long> resourceIds = new ArrayList<>(NUMBER_OF_SECRETS);
        List<String> resourceReferences = new ArrayList<>(NUMBER_OF_SECRETS);
        List<Resource> resources = IntStream.range(0, NUMBER_OF_SECRETS)
                .boxed()
                .map(i -> {
                    Resource resource = new Resource();
                    long resourceId = i;
                    resource.setId(resourceId);
                    resource.setResourceReference("secret-" + i);
                    resourceIds.add(resourceId);
                    resourceReferences.add("secret-" + i);
                    return resource;
                })
                .toList();
        when(stackService.getById(STACK_ID)).thenReturn(stack);
        when(userdataSecretsService.createUserdataSecrets(stack, resourceIds, CLOUD_CONTEXT, CLOUD_CREDENTIAL)).thenReturn(resources);

        underTest.accept(new Event<>(new UpscaleCreateUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL, resourceIds)));

        verify(stackService).getById(STACK_ID);
        verify(userdataSecretsService).createUserdataSecrets(stack, resourceIds, CLOUD_CONTEXT, CLOUD_CREDENTIAL);
        verify(encryptionKeyService).updateCloudSecretManagerEncryptionKeyAccess(stack, CLOUD_CONTEXT, CLOUD_CREDENTIAL, resourceReferences, List.of());
        verify(eventBus).notify(eq(EventSelectorUtil.selector(UpscaleCreateUserdataSecretsSuccess.class)), successEventCaptor.capture());
        UpscaleCreateUserdataSecretsSuccess result = successEventCaptor.getValue().getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertThat(result.getCreatedSecretResourceIds()).hasSameElementsAs(resources.stream().map(Resource::getId).toList());
    }

    @Test
    void testAcceptEventZeroSecretsToCreate() {
        underTest.accept(new Event<>(new UpscaleCreateUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL, List.of())));

        verifyNoInteractions(stackService);
        verifyNoInteractions(userdataSecretsService);
        verify(eventBus).notify(eq(EventSelectorUtil.selector(UpscaleCreateUserdataSecretsSuccess.class)), successEventCaptor.capture());
        UpscaleCreateUserdataSecretsSuccess result = successEventCaptor.getValue().getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertThat(result.getCreatedSecretResourceIds()).isEmpty();
    }

    @Test
    void testAcceptFailureEvent() {
        RuntimeException e = new RuntimeException("test");
        when(stackService.getById(anyLong())).thenThrow(e);

        underTest.accept(new Event<>(new UpscaleCreateUserdataSecretsRequest(STACK_ID, CLOUD_CONTEXT, CLOUD_CREDENTIAL, List.of(0L))));

        verify(eventBus).notify(eq(EventSelectorUtil.selector(UpscaleCreateUserdataSecretsFailed.class)), failureEventCaptor.capture());
        UpscaleCreateUserdataSecretsFailed result = failureEventCaptor.getValue().getData();
        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(e, result.getException());
    }
}
