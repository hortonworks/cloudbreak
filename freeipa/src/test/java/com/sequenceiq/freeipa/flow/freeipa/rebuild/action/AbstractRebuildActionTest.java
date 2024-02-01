package com.sequenceiq.freeipa.flow.freeipa.rebuild.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.event.RebuildFailureEvent;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class AbstractRebuildActionTest {

    private static final Long STACK_ID = 4L;

    @Mock
    private StackService stackService;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private CredentialService credentialService;

    @InjectMocks
    private TestRebuildAction underTest;

    @Test
    void createFlowContext() {
        FlowParameters flowParameters = new FlowParameters("flowId", "flowUserCrn");
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName("stacktestname");
        stack.setResourceCrn("stackCrn");
        stack.setCloudPlatform("MOCK");
        stack.setPlatformvariant("MOCK");
        stack.setOwner("testOWner");
        stack.setAccountId("accId");
        stack.setEnvironmentCrn("envCrn");
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        Credential credential = mock(Credential.class);
        when(credentialService.getCredentialByEnvCrn(stack.getEnvironmentCrn())).thenReturn(credential);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        when(credentialConverter.convert(credential)).thenReturn(cloudCredential);
        CloudStack cloudStack = mock(CloudStack.class);
        when(cloudStackConverter.convert(stack)).thenReturn(cloudStack);

        StackContext result = underTest.createFlowContext(flowParameters, null, () -> STACK_ID);

        CloudContext cloudContext = result.getCloudContext();
        assertEquals(STACK_ID, cloudContext.getId());
        assertEquals(stack.getName(), cloudContext.getName());
        assertEquals(stack.getResourceCrn(), cloudContext.getCrn());
        assertEquals(stack.getCloudPlatform(), cloudContext.getPlatform().value());
        assertEquals(stack.getPlatformvariant(), cloudContext.getPlatformVariant().getVariant().value());
        assertEquals(stack.getOwner(), cloudContext.getUserName());
        assertEquals(stack.getAccountId(), cloudContext.getAccountId());

        assertEquals(flowParameters, result.getFlowParameters());
        assertEquals(stack, result.getStack());
        assertEquals(cloudStack, result.getCloudStack());
        assertEquals(cloudCredential, result.getCloudCredential());
    }

    @Test
    void getFailurePayload() {
        Exception test = new Exception("test");

        RebuildFailureEvent failurePayload = (RebuildFailureEvent) underTest.getFailurePayload(() -> STACK_ID, Optional.empty(), test);

        assertEquals(STACK_ID, failurePayload.getResourceId());
        assertEquals(test, failurePayload.getException());
    }

    @Test
    void setInstanceToRestoreFqdn() {
        Map<Object, Object> test = new HashMap<>();

        underTest.setInstanceToRestoreFqdn(test, "FQDN");

        assertEquals("FQDN", underTest.getInstanceToRestoreFqdn(test));
    }

    @Test
    void setFullBackupStorageLocation() {
        Map<Object, Object> test = new HashMap<>();

        underTest.setFullBackupStorageLocation(test, "backup");

        assertEquals("backup", underTest.getFullBackupStorageLocation(test));
    }

    @Test
    void setDataBackupStorageLocation() {
        Map<Object, Object> test = new HashMap<>();

        underTest.setDataBackupStorageLocation(test, "backupd");

        assertEquals("backupd", underTest.getDataBackupStorageLocation(test));
    }

    private static final class TestRebuildAction extends AbstractRebuildAction<Payload> {

        protected TestRebuildAction() {
            super(Payload.class);
        }

        @Override
        protected void doExecute(StackContext context, Payload payload, Map<Object, Object> variables) throws Exception {

        }
    }
}