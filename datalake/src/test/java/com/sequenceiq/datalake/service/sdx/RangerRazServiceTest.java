package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.YARN;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.CLUSTER_NAME;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.ENVIRONMENT_CRN;
import static com.sequenceiq.datalake.service.sdx.SdxTestUtil.USER_CRN;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RangerRazEnabledV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class RangerRazServiceTest {
    private static final String ACCOUNT_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:cloudera@hortonworks.com";

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private SdxVersionRuleEnforcer sdxVersionRuleEnforcer;

    @Mock
    private SdxService sdxService;

    @Mock
    private StackService stackService;

    @Mock
    private PlatformConfig platformConfig;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private RangerRazService underTest;

    static Object[][] razCloudPlatform720DataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform expectedErrorMsg
                {"CloudPlatform.AWS", AWS},
                {"CloudPlatform.AZURE", AZURE},
                {"CloudPlatform.GCP", GCP}
        };
    }

    static Object[][] razCloudPlatform710DataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform expectedErrorMsg
                {"CloudPlatform.AWS", AWS, "7.2.2",
                        "Provisioning Ranger Raz on Amazon Web Services is only valid for Cloudera Runtime version " +
                                "greater than or equal to 7.2.2 and not 7.1.0"},
                {"CloudPlatform.AZURE", AZURE, "7.2.2",
                        "Provisioning Ranger Raz on Microsoft Azure is only valid for Cloudera Runtime version " +
                                "greater than or equal to 7.2.2 and not 7.1.0"},
                {"CloudPlatform.GCP", GCP, "7.2.17",
                        "Provisioning Ranger Raz on GCP is only valid for Cloudera Runtime version " +
                                "greater than or equal to 7.2.17 and not 7.1.0"}
        };
    }

    static Object[][] razCloudPlatformAndRuntimeDataProvider() {
        return new Object[][]{
                // testCaseName cloudPlatform runtime
                {"CloudPlatform.AWS", AWS, "7.2.2"},
                {"CloudPlatform.AZURE", AZURE, "7.2.2"},
                {"CloudPlatform.GCP", GCP, "7.2.17"}
        };
    }

    @BeforeEach
    void initMocks() {
        lenient().when(platformConfig.getRazSupportedPlatforms())
                .thenReturn(List.of(AWS, AZURE, GCP));
        lenient().when(entitlementService.isRazForGcpEnabled(anyString())).thenReturn(true);
    }

    @Test
    void testUpdateRangerRazEnabledForSdxClusterWhenRangerRazIsPresent() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setEnvName("env");
        sdxCluster.setEnvCrn(ENVIRONMENT_CRN);
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setCrn("test-crn");
        sdxCluster.setRuntime("7.2.11");

        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("AWS");

        RangerRazEnabledV4Response response = mock(RangerRazEnabledV4Response.class);
        when(stackService.rangerRazEnabledInternal(anyString())).thenReturn(response);
        when(response.isRangerRazEnabled()).thenReturn(true);
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);
        when(sdxVersionRuleEnforcer.isRazSupported(any(), any())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateRangerRazEnabled(sdxCluster));

        assertTrue(sdxCluster.isRangerRazEnabled());
        verify(sdxService, times(1)).save(sdxCluster);
    }

    @Test
    void testUpdateRangerRazThrowsExceptionForSdxClusterWhenRangerRazIsNotPresent() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setEnvName("env");
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setCrn("test-crn");
        sdxCluster.setRuntime("7.2.11");

        RangerRazEnabledV4Response response = mock(RangerRazEnabledV4Response.class);
        when(stackService.rangerRazEnabledInternal(anyString())).thenReturn(response);
        when(response.isRangerRazEnabled()).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateRangerRazEnabled(sdxCluster)));

        assertEquals(String.format("Ranger raz is not installed on the datalake: %s!", CLUSTER_NAME), exception.getMessage());
        verify(sdxService, times(0)).save(sdxCluster);
    }

    @Test
    void testUpdateRangerRazIsIgnoredIfRangerRazIsInstalledAndFlagAlreadySet() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setEnvName("env");
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setRangerRazEnabled(true);
        sdxCluster.setCrn("test-crn");
        sdxCluster.setRuntime("7.2.11");

        RangerRazEnabledV4Response response = mock(RangerRazEnabledV4Response.class);
        when(stackService.rangerRazEnabledInternal(anyString())).thenReturn(response);
        when(response.isRangerRazEnabled()).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.updateRangerRazEnabled(sdxCluster));

        verify(sdxService, times(0)).save(sdxCluster);
    }

    @Test
    void testValidateRazEnablementForGcpEntitilementNotEnabled() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("GCP");
        environmentResponse.setCreator(ACCOUNT_CRN);

        when(entitlementService.isRazForGcpEnabled(anyString())).thenReturn(false);
        when(sdxVersionRuleEnforcer.isRazSupported(any(), any())).thenReturn(true);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateRazEnablement("7.2.17", true, environmentResponse));

        assertEquals("Provisioning Ranger Raz on GCP is not enabled for this account", badRequestException.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatform720DataProvider")
    void testValidateRazEnablement720Runtime(String testCaseName, CloudPlatform cloudPlatform) throws IOException {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(cloudPlatform.toString());
        environmentResponse.setCreator(ACCOUNT_CRN);

        when(sdxVersionRuleEnforcer.isRazSupported(any(), any())).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateRazEnablement("7.2.17", true, environmentResponse));
    }

    @Test
    void testValidateRazEnablementdWithRazNotEnabledCloud() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(YARN.toString());
        environmentResponse.setCreator(ACCOUNT_CRN);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateRazEnablement("7.2.17", true, environmentResponse));

        assertEquals("Provisioning Ranger Raz is only valid for Amazon Web Services, Microsoft Azure, GCP", badRequestException.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatform710DataProvider")
    void testSdxCreateRazEnabled710Runtime(String testCaseName, CloudPlatform cloudPlatform, String expectedVersion, String expectedErrorMsg) {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(cloudPlatform.toString());
        environmentResponse.setCreator(ACCOUNT_CRN);

        when(sdxVersionRuleEnforcer.getSupportedRazVersionForPlatform(cloudPlatform)).thenReturn(expectedVersion);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateRazEnablement("7.1.0", true, environmentResponse)));

        assertEquals(expectedErrorMsg, badRequestException.getMessage());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("razCloudPlatformAndRuntimeDataProvider")
    void testValidateRazEnablement(String testCaseName, CloudPlatform cloudPlatform, String runtime) throws IOException, TransactionExecutionException {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform(cloudPlatform.toString());
        environmentResponse.setCreator(ACCOUNT_CRN);

        when(sdxVersionRuleEnforcer.isRazSupported(runtime, cloudPlatform)).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateRazEnablement(runtime, true, environmentResponse));
    }
}