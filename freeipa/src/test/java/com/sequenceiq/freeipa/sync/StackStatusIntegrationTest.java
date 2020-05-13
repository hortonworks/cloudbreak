package com.sequenceiq.freeipa.sync;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.RPCMessage;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = StackStatusIntegrationTest.TestAppContext.class)
class StackStatusIntegrationTest {

    private static final Long STACK_ID = 123L;

    private static final String INSTANCE_1 = "i1";

    @Inject
    private StackStatusCheckerJob underTest;

    @MockBean
    private StackService stackService;

    @MockBean
    private FlowLogService flowLogService;

    @MockBean
    private FreeIpaClientFactory freeIpaClientFactory;

    @MockBean
    private StackInstanceProviderChecker stackInstanceProviderChecker;

    @MockBean
    private InstanceMetaDataService instanceMetaDataService;

    @MockBean
    private StackUpdater stackUpdater;

    @Mock
    private FreeIpaClient freeIpaClient;

    @Mock
    private JobExecutionContext jobExecutionContext;

    private Stack stack;

    private Set<InstanceMetaData> notTerminatedInstances;

    private RPCResponse<Boolean> rpcResponse;

    @BeforeEach
    void setUp() throws Exception {
        underTest.setLocalId(STACK_ID.toString());

        stack = new Stack();
        stack.setId(STACK_ID);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setDetailedStackStatus(DetailedStackStatus.PROVISIONED);
        stack.setStackStatus(stackStatus);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);

        when(flowLogService.isOtherFlowRunning(STACK_ID)).thenReturn(false);

        notTerminatedInstances = Set.of(createInstance(INSTANCE_1));
        when(instanceMetaDataService.findNotTerminatedForStack(STACK_ID)).thenReturn(notTerminatedInstances);

        setUpFreeIpaClient();
    }

    private InstanceMetaData createInstance(String instanceName) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        instanceMetaData.setInstanceId(instanceName);
        instanceMetaData.setDiscoveryFQDN(instanceName);
        return instanceMetaData;
    }

    private void setUpFreeIpaClient() throws Exception {
        when(freeIpaClientFactory.getFreeIpaClientForStackWithPing(stack, INSTANCE_1)).thenReturn(freeIpaClient);
        String freeIpaClientHostname = "freeIpaClientHostname";
        when(freeIpaClient.getHostname()).thenReturn(freeIpaClientHostname);

        rpcResponse = new RPCResponse<>();
        when(freeIpaClient.serverConnCheck(freeIpaClientHostname, INSTANCE_1)).thenReturn(rpcResponse);
    }

    @Test
    @DisplayName(
            "GIVEN an available stack " +
            "WHEN FreeIpa instance is available " +
            "THEN stack status should not change"
    )
    void available() throws JobExecutionException {
        setUpFreeIpaAvailabilityResponse(true);
        when(stackInstanceProviderChecker.checkStatus(stack, notTerminatedInstances)).thenReturn(List.of(
                createCloudVmInstanceStatus(INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.STARTED)
        ));

        underTest.executeInternal(jobExecutionContext);

        verify(stackUpdater, never()).updateStackStatus(eq(stack), any(), any());
    }

    @Test
    @DisplayName(
            "GIVEN an available stack " +
                "WHEN FreeIpa instance is deleted " +
                "THEN stack status should change"
    )
    void deleted() throws JobExecutionException {
        setUpFreeIpaAvailabilityResponse(false);
        when(stackInstanceProviderChecker.checkStatus(stack, notTerminatedInstances)).thenReturn(List.of(
                createCloudVmInstanceStatus(INSTANCE_1, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus.TERMINATED_BY_PROVIDER)
        ));

        underTest.executeInternal(jobExecutionContext);

        verify(stackUpdater).updateStackStatus(eq(stack), eq(DetailedStackStatus.DELETED_ON_PROVIDER_SIDE), any());
    }

    private void setUpFreeIpaAvailabilityResponse(boolean value) {
        rpcResponse.setResult(value);
        RPCMessage mess = new RPCMessage();
        mess.setName("name");
        mess.setMessage("message");
        rpcResponse.setMessages(List.of(mess));
    }

    private CloudVmInstanceStatus createCloudVmInstanceStatus(String instanceId, com.sequenceiq.cloudbreak.cloud.model.InstanceStatus instanceStatus) {
        return new CloudVmInstanceStatus(new CloudInstance(instanceId, null, null), instanceStatus);
    }

    @Configuration
    @Import({
            StackStatusCheckerJob.class,
            FreeipaChecker.class,
            ProviderChecker.class,
            AutoSyncConfig.class
    })
    @PropertySource("classpath:application.yml")
    static class TestAppContext {
    }
}
