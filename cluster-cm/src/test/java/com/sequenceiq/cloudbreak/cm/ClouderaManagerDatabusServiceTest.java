package com.sequenceiq.cloudbreak.cm;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.workspace.model.User;

public class ClouderaManagerDatabusServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:accountId:user:name";

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
    public void testGetAltusCredential() {
        // GIVEN
        AltusCredential credential = new AltusCredential("accessKey", "secretKey".toCharArray());
        UserManagementProto.MachineUser machineUser = UserManagementProto.MachineUser.newBuilder()
                .setMachineUserName("machineUser")
                .setCrn(USER_CRN)
                .build();
        when(umsClient.createMachineUser(any(), any(), any())).thenReturn(Optional.of(machineUser.getCrn()));
        doNothing().when(umsClient).assignMachineUserRole(any(), any(), any(), any());
        when(umsClient.createMachineUserAndGenerateKeys(any(), any(), any())).thenReturn(credential);
        // WHEN
        AltusCredential result = underTest.getAltusCredential(stack);
        // THEN
        assertEquals("secretKey", new String(result.getPrivateKey()));
    }

    @Test
    public void testTrimAndReplace() {
        // GIVEN
        String rawPrivateKey = "BEGIN\nline1\nline2\nlastline";
        // WHEN
        String result = underTest.trimAndReplacePrivateKey(rawPrivateKey.toCharArray());
        // THEN
        assertEquals("BEGIN\\nline1\\nline2\\nlastline", result);
    }
}
