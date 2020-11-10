package com.sequenceiq.cloudbreak.job.altus;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.domain.projection.StackTtlView;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.quartz.cleanup.UMSCleanupConfig;
import com.sequenceiq.cloudbreak.service.altus.AltusMachineUserService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

import io.opentracing.Tracer;

@ExtendWith(MockitoExtension.class)
public class CloudbreakUMSCleanupJobTest {

    @InjectMocks
    private CloudbreakUMSCleanupJob underTest;

    @Mock
    private UMSCleanupConfig umsCleanupConfig;

    @Mock
    private StackService stackService;

    @Mock
    private AltusMachineUserService altusMachineUserService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @Mock
    private Tracer tracer;

    @BeforeEach
    public void setUp() {
        underTest = new CloudbreakUMSCleanupJob(umsCleanupConfig, altusMachineUserService, stackService, tracer);
    }

    @Test
    public void testExecuteInternal() throws JobExecutionException {
        // GIVEN
        given(stackService.getAllAlive()).willReturn(createStacks());
        fluentMachineUserMocks();
        given(altusMachineUserService.getAllInternalMachineUsers("acc1")).willReturn(getMachineUsers("acc1"));
        given(altusMachineUserService.getAllInternalMachineUsers("acc2")).willReturn(getMachineUsers("acc2"));
        given(altusMachineUserService.getAllInternalMachineUsers("acc3")).willReturn(getMachineUsers("acc3"));
        given(umsCleanupConfig.getMaxAgeDays()).willReturn(100);
        // WHEN
        underTest.executeTracedJob(jobExecutionContext);
        // THEN
        verify(altusMachineUserService, times(6)).getFluentDatabusMachineUserName(anyString(), anyString());
        verify(altusMachineUserService, times(5)).cleanupMachineUser(anyString(), anyString());
    }

    @Test
    public void testExecuteInternalWithoutStacks() throws JobExecutionException {
        // GIVEN
        given(stackService.getAllAlive()).willReturn(new ArrayList<>());
        // WHEN
        underTest.executeTracedJob(jobExecutionContext);
        // THEN
        verify(altusMachineUserService, times(0)).getFluentDatabusMachineUserName(anyString(), anyString());
        verify(altusMachineUserService, times(0)).cleanupMachineUser(anyString(), anyString());
    }

    @Test
    public void testExecuteInternalWithoutMachineUsers() throws JobExecutionException {
        // GIVEN
        given(stackService.getAllAlive()).willReturn(createStacks());
        fluentMachineUserMocks();
        given(altusMachineUserService.getAllInternalMachineUsers(anyString())).willReturn(new ArrayList<>());
        // WHEN
        underTest.executeTracedJob(jobExecutionContext);
        // THEN
        verify(altusMachineUserService, times(6)).getFluentDatabusMachineUserName(anyString(), anyString());
        verify(altusMachineUserService, times(0)).cleanupMachineUser(anyString(), anyString());
    }

    private List<UserManagementProto.MachineUser> getMachineUsers(String accountId) {
        List<UserManagementProto.MachineUser> machineUsers = new ArrayList<>();
        machineUsers.add(createMachineUser("do-not-delete", old()));
        machineUsers.add(createMachineUser("datahub-fluent-databus-uploader-deleteme", old()));
        if ("acc1".equals(accountId)) {
            machineUsers.add(createMachineUser("datalake-fluent-databus-uploader-cluster1", now()));
            machineUsers.add(createMachineUser("datahub-fluent-databus-uploader-cluster2", now()));
            machineUsers.add(createMachineUser("datahub-wa-publisher-cluster2", now()));
            machineUsers.add(createMachineUser("datahub-fluent-databus-uploader-cluster3", now()));
        } else if ("acc2".equals(accountId)) {
            machineUsers.add(createMachineUser("datahub-fluent-databus-uploader-cluster4", now()));
            machineUsers.add(createMachineUser("datahub-fluent-databus-uploader-cluster5", old()));
        } else if ("acc3".equals(accountId)) {
            machineUsers.add(createMachineUser("datalake-fluent-databus-uploader-cluster6", old()));
            machineUsers.add(createMachineUser("datahub-wa-publisher-cluster6", old()));
            machineUsers.add(createMachineUser("datahub-fluent-databus-uploader-cluster6", old()));
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

    private List<StackTtlView> createStacks() {
        List<StackTtlView> stacks = new ArrayList<>();
        stacks.add(new StackTtlViewImpl("crn:cdp:datalake:us-west-1:acc1:datalake:cluster1"));
        stacks.add(new StackTtlViewImpl("crn:cdp:datahub:us-west-1:acc1:stack:cluster2"));
        stacks.add(new StackTtlViewImpl("crn:cdp:datahub:us-west-1:acc1:stack:cluster3"));
        stacks.add(new StackTtlViewImpl("crn:cdp:datalake:us-west-1:acc2:datalake:cluster4"));
        stacks.add(new StackTtlViewImpl("crn:cdp:datahub:us-west-1:acc2:stack:cluster5"));
        stacks.add(new StackTtlViewImpl("crn:cdp:datalake:us-west-1:acc3:datalake:cluster6"));
        return stacks;
    }

    private void fluentMachineUserMocks() {
        given(altusMachineUserService.getFluentDatabusMachineUserName("datalake", "cluster1"))
                .willReturn("datalake-fluent-databus-uploader-cluster1");
        given(altusMachineUserService.getFluentDatabusMachineUserName("datahub", "cluster2"))
                .willReturn("datahub-fluent-databus-uploader-cluster2");
        given(altusMachineUserService.getFluentDatabusMachineUserName("datahub", "cluster3"))
                .willReturn("datahub-fluent-databus-uploader-cluster3");
        given(altusMachineUserService.getFluentDatabusMachineUserName("datalake", "cluster4"))
                .willReturn("datalake-fluent-databus-uploader-cluster4");
        given(altusMachineUserService.getFluentDatabusMachineUserName("datahub", "cluster5"))
                .willReturn("datahub-fluent-databus-uploader-cluster5");
        given(altusMachineUserService.getFluentDatabusMachineUserName("datalake", "cluster6"))
                .willReturn("datalake-fluent-databus-uploader-cluster6");
    }

    private static class StackTtlViewImpl implements StackTtlView {

        private final String crn;

        StackTtlViewImpl(String crn) {
            this.crn = crn;
        }

        @Override
        public Long getId() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getCrn() {
            return this.crn;
        }

        @Override
        public Workspace getWorkspace() {
            return null;
        }

        @Override
        public StackStatus getStatus() {
            return null;
        }

        @Override
        public Long getCreationFinished() {
            return null;
        }
    }
}
