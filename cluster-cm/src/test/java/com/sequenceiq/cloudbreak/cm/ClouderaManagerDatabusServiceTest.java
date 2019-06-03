package com.sequenceiq.cloudbreak.cm;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.cloud.model.Telemetry;
import com.sequenceiq.cloudbreak.cloud.model.WorkloadAnalytics;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.workspace.model.User;

public class ClouderaManagerDatabusServiceTest {

    private static final String USER_CRN = "crn:altus:iam:us-west-1:accountId:user:name";

    @InjectMocks
    private ClouderaManagerDatabusService underTest;

    @Mock
    private GrpcUmsClient umsClient;

    private Stack stack;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        stack = new Stack();
        User creator = new User();
        creator.setUserCrn(USER_CRN);
        stack.setCreator(creator);
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        stack.setCluster(cluster);
    }

    @Test
    public void testCreateMachineUserAndGenerateKeys() {
        // GIVEN
        UserManagementProto.MachineUser machineUser = UserManagementProto.MachineUser.newBuilder()
                .setMachineUserName("machineUser")
                .setCrn(USER_CRN)
                .build();
        AltusCredential altusCredential = new AltusCredential("accessKey", "secretKey".toCharArray());
        when(umsClient.createMachineUser(any(), any(), any())).thenReturn(machineUser);
        doNothing().when(umsClient).assignMachineUserRole(any(), any(), any(), any());
        when(umsClient.generateAccessSecretKeyPair(any(), any(), any())).thenReturn(altusCredential);

        // WHEN
        underTest.createMachineUserAndGenerateKeys(stack);

        // THEN
        assertEquals("secretKey", new String(altusCredential.getPrivateKey()));
        verify(umsClient, times(1)).createMachineUser(any(), any(), any());
        verify(umsClient, times(1)).assignMachineUserRole(any(), any(), any(), any());
        verify(umsClient, times(1)).generateAccessSecretKeyPair(any(), any(), any());
    }

    @Test
    public void testCleanupMachineUser() {
        // GIVEN
        WorkloadAnalytics wa = new WorkloadAnalytics(true, null, null, null, null);
        Telemetry telemetry = new Telemetry(null, wa);

        doNothing().when(umsClient).unassignMachineUserRole(any(), any(), any(), any());
        doNothing().when(umsClient).deleteMachineUserAccessKeys(any(), any(), any());
        doNothing().when(umsClient).deleteMachineUser(any(), any(), any());

        // WHEN
        underTest.cleanUpMachineUser(stack, telemetry);

        // THEN
        verify(umsClient, times(1)).unassignMachineUserRole(any(), any(), any(), any());
        verify(umsClient, times(1)).deleteMachineUserAccessKeys(any(), any(), any());
        verify(umsClient, times(1)).deleteMachineUser(any(), any(), any());
    }

    @Test
    public void testCleanupMachineUserWithProvidedKeys() {
        // GIVEN
        WorkloadAnalytics wa = new WorkloadAnalytics(true, null, "access", "private", null);
        Telemetry telemetry = new Telemetry(null, wa);

        // WHEN
        underTest.cleanUpMachineUser(stack, telemetry);

        // THEN
        verify(umsClient, times(0)).unassignMachineUserRole(any(), any(), any(), any());
    }

    @Test
    public void testBuiltInDatabusUploaderRoleCrn() {
        // GIVEN
        // WHEN
        String result = underTest.getBuiltInDatabusCrn();
        // THEN
        assertEquals("crn:altus:iam:us-west-1:altus:role:DbusUploader", result);
    }
}
