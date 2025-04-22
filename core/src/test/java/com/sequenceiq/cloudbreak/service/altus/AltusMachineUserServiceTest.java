package com.sequenceiq.cloudbreak.service.altus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.model.CdpAccessKeyType;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.telemetry.TelemetryFeatureService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@ExtendWith(MockitoExtension.class)
public class AltusMachineUserServiceTest {

    private static final String TEST_CRN = "crn:cdp:iam:us-west-1:accountId:user:name";

    private AltusMachineUserService underTest;

    @Mock
    private AltusIAMService altusIAMService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private TelemetryFeatureService telemetryFeatureService;

    @Mock
    private StackDto stackDto;

    @Mock
    private Image image;

    private Stack stack;

    private Cluster cluster;

    private Telemetry telemetry;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        stack = new Stack();
        User creator = new User();
        creator.setUserCrn(TEST_CRN);
        stack.setCreator(creator);
        stack.setType(StackType.WORKLOAD);
        stack.setResourceCrn(TEST_CRN);
        cluster = new Cluster();
        cluster.setId(1L);
        stack.setCluster(cluster);
        telemetry = new Telemetry();
        cluster.setDatabusCredential("dbus");
        underTest = new AltusMachineUserService(altusIAMService, stackDtoService,
                clusterService, componentConfigProviderService, entitlementService, telemetryFeatureService);
    }

    @Test
    public void testCreateMachineUserAndGenerateKeys() {
        // GIVEN
        Optional<AltusCredential> altusCredential = Optional.of(new AltusCredential("accessKey", "secretKey".toCharArray()));
        when(altusIAMService.generateDatabusMachineUserWithAccessKey(any(), anyBoolean())).thenReturn(altusCredential);
        // WHEN
        underTest.generateDatabusMachineUserForFluent(stack, telemetry, CdpAccessKeyType.ED25519);

        // THEN
        assertEquals("secretKey", new String(altusCredential.get().getPrivateKey()));
        verify(altusIAMService, times(1)).generateDatabusMachineUserWithAccessKey(any(), anyBoolean());
    }

    @Test
    public void testCleanupMachineUser() {
        // GIVEN
        doNothing().when(altusIAMService).clearMachineUser(any(), any(), anyBoolean());
        // WHEN
        underTest.clearFluentMachineUser(stack, cluster, telemetry);

        // THEN
        verify(altusIAMService, times(1)).clearMachineUser(any(), any(), anyBoolean());
    }

    @Test
    public void testGetGovAndCdpAccessKeyButNotGov() {
        when(stackDto.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_VARIANT.variant().value());

        CdpAccessKeyType cdpAccessKeyType = underTest.getCdpAccessKeyType(stackDto);
        assertEquals(CdpAccessKeyType.ED25519, cdpAccessKeyType);
    }

    @Test
    public void testGetGovAndCdpAccessKeyTypeNoImage() {
        when(stackDto.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value());
        when(componentConfigProviderService.findImage(anyLong())).thenReturn(Optional.empty());

        CloudbreakRuntimeException cloudbreakRuntimeException = assertThrows(CloudbreakRuntimeException.class, () -> underTest.getCdpAccessKeyType(stackDto));
        assertEquals("ECDSA is mandatory on AWS gov deployment, but we could not get image package versions, " +
                        "so we can't decide if the selected image supports it.", cloudbreakRuntimeException.getMessage());
    }

    @Test
    public void testGetGovAndCdpAccessKeyButBadImage() {
        when(stackDto.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value());
        when(componentConfigProviderService.findImage(anyLong())).thenReturn(Optional.of(image));
        when(telemetryFeatureService.isECDSAAccessKeyTypeSupported(any())).thenReturn(false);

        CloudbreakRuntimeException cloudbreakRuntimeException = assertThrows(CloudbreakRuntimeException.class, () -> underTest.getCdpAccessKeyType(stackDto));
        assertEquals("ECDSA is mandatory on AWS gov deployment, but the image contains packages which can't support ECDSA key",
                cloudbreakRuntimeException.getMessage());
    }

    @Test
    public void testGetGovAndCdpAccessKeyAndGoodImage() {
        when(stackDto.getPlatformVariant()).thenReturn(AwsConstants.AwsVariant.AWS_NATIVE_GOV_VARIANT.variant().value());
        when(componentConfigProviderService.findImage(anyLong())).thenReturn(Optional.of(image));
        when(telemetryFeatureService.isECDSAAccessKeyTypeSupported(any())).thenReturn(true);

        CdpAccessKeyType cdpAccessKeyType = underTest.getCdpAccessKeyType(stackDto);
        assertEquals(CdpAccessKeyType.ECDSA, cdpAccessKeyType);
    }
}