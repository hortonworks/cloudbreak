package com.sequenceiq.cloudbreak.service.altus;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.FeatureSetting;

public class AltusMachineUserServiceTest {

    private static final String TEST_CRN = "crn:cdp:iam:us-west-1:accountId:user:name";

    private AltusMachineUserService underTest;

    @Mock
    private AltusIAMService altusIAMService;

    private Stack stack;

    private Telemetry telemetry;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        stack = new Stack();
        User creator = new User();
        creator.setUserCrn(TEST_CRN);
        stack.setCreator(creator);
        stack.setType(StackType.WORKLOAD);
        stack.setResourceCrn(TEST_CRN);
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        stack.setCluster(cluster);
        telemetry = new Telemetry();
        FeatureSetting reportDeploymentLogs = new FeatureSetting();
        reportDeploymentLogs.setEnabled(true);
        Features features = new Features();
        features.setReportDeploymentLogs(reportDeploymentLogs);
        telemetry.setFeatures(features);
        underTest = new AltusMachineUserService(altusIAMService);
    }

    @Test
    public void testCreateMachineUserAndGenerateKeys() {
        // GIVEN
        Optional<AltusCredential> altusCredential = Optional.of(new AltusCredential("accessKey", "secretKey".toCharArray()));
        when(altusIAMService.generateMachineUserWithAccessKey(any(), any())).thenReturn(altusCredential);

        // WHEN
        underTest.generateDatabusMachineUserForFluent(stack, telemetry);

        // THEN
        assertEquals("secretKey", new String(altusCredential.get().getPrivateKey()));
        verify(altusIAMService, times(1)).generateMachineUserWithAccessKey(any(), any());
    }

    @Test
    public void testCleanupMachineUser() {
        // GIVEN
        doNothing().when(altusIAMService).clearMachineUser(any(), any());
        // WHEN
        underTest.clearFluentMachineUser(stack, telemetry);

        // THEN
        verify(altusIAMService, times(1)).clearMachineUser(any(), any());
    }
}