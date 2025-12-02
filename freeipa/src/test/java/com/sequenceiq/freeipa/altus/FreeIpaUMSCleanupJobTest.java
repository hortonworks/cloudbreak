package com.sequenceiq.freeipa.altus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.quartz.cleanup.UMSCleanupConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.AltusMachineUserService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class FreeIpaUMSCleanupJobTest {

    @InjectMocks
    private FreeIpaUMSCleanupJob underTest;

    @Mock
    private UMSCleanupConfig umsCleanupConfig;

    @Mock
    private StackService stackService;

    @Mock
    private AltusMachineUserService altusMachineUserService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Test
    public void testExecuteInternal() throws JobExecutionException {
        // GIVEN
        given(stackService.findAllRunning()).willReturn(createStacks());
        fluentMachineUserMocks();
        given(altusMachineUserService.getAllInternalMachineUsers("acc1")).willReturn(getMachineUsers("acc1"));
        given(altusMachineUserService.getAllInternalMachineUsers("acc2")).willReturn(getMachineUsers("acc2"));
        given(altusMachineUserService.getAllInternalMachineUsers("acc3")).willReturn(getMachineUsers("acc3"));
        given(umsCleanupConfig.getMaxAgeDays()).willReturn(100);
        // WHEN
        underTest.executeTracedJob(jobExecutionContext);
        // THEN
        verify(altusMachineUserService, times(4)).cleanupMachineUser(anyString(), anyString());
    }

    @Test
    public void testExecuteInternalWithoutStacks() throws JobExecutionException {
        // GIVEN
        given(stackService.findAllRunning()).willReturn(new ArrayList<>());
        // WHEN
        underTest.executeTracedJob(jobExecutionContext);
        // THEN
        verify(altusMachineUserService, times(0)).cleanupMachineUser(anyString(), anyString());
    }

    @Test
    public void testExecuteInternalWithoutMachineUsers() throws JobExecutionException {
        // GIVEN
        given(stackService.findAllRunning()).willReturn(createStacks());
        fluentMachineUserMocks();
        given(altusMachineUserService.getAllInternalMachineUsers(anyString())).willReturn(new ArrayList<>());
        // WHEN
        underTest.executeTracedJob(jobExecutionContext);
        // THEN
        verify(altusMachineUserService, times(0)).cleanupMachineUser(anyString(), anyString());
    }

    private void fluentMachineUserMocks() {
        given(altusMachineUserService.getFluentMachineUser(any()))
                .willReturn("freeipa-fluent-databus-uploader-cluster1")
                .willReturn("freeipa-fluent-databus-uploader-cluster2")
                .willReturn("freeipa-fluent-databus-uploader-cluster3")
                .willReturn("freeipa-fluent-databus-uploader-cluster4");
    }

    private List<Stack> createStacks() {
        List<Stack> stacks = new ArrayList<>();
        stacks.add(createStack("cluster1", "acc1"));
        stacks.add(createStack("cluster2", "acc1"));
        stacks.add(createStack("cluster3", "acc2"));
        stacks.add(createStack("cluster4", "acc3"));
        return stacks;
    }

    private Stack createStack(String resource, String accountId) {
        Stack stack = new Stack();
        stack.setResourceCrn(getCrn(accountId, resource));
        stack.setAccountId(accountId);
        return stack;
    }

    private String getCrn(String resource, String accountId) {
        return String.format("crn:cdp:freeipa:us-west-1:%s:freeipa:%s", accountId, resource);
    }

    private List<UserManagementProto.MachineUser> getMachineUsers(String accountId) {
        List<UserManagementProto.MachineUser> machineUsers = new ArrayList<>();
        machineUsers.add(createMachineUser("do-not-delete", old()));
        machineUsers.add(createMachineUser("freeipa-fluent-databus-uploader-deleteme", old()));
        if ("acc1".equals(accountId)) {
            machineUsers.add(createMachineUser("freeipa-fluent-databus-uploader-cluster1", now()));
        } else if ("acc2".equals(accountId)) {
            machineUsers.add(createMachineUser("freeipa-fluent-databus-uploader-cluster2", now()));
            machineUsers.add(createMachineUser("freeipa-fluent-databus-uploader-cluster3", old()));
        } else if ("acc3".equals(accountId)) {
            machineUsers.add(createMachineUser("freeipa-fluent-databus-uploader-cluster4", old()));
            machineUsers.add(createMachineUser("freeipa-fluent-databus-uploader-cluster5", old()));
        }
        return machineUsers;
    }

    private long now() {
        return Instant.now().toEpochMilli();
    }

    private long old() {
        return Instant.now().minus(1000, ChronoUnit.DAYS).toEpochMilli();
    }

    private UserManagementProto.MachineUser createMachineUser(String name, long createTs) {
        return UserManagementProto.MachineUser.newBuilder()
                .setMachineUserName(name)
                .setCreationDateMs(createTs)
                .build();
    }

}
