package com.sequenceiq.freeipa.service.stack.instance;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.ArchitectureVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.DefaultVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.freeipa.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.service.CredentialService;

@ExtendWith(MockitoExtension.class)
class DefaultInstanceTypeProviderTest {

    private static final String CREDENTIAL_CRN = "crn:cdp:credential:us-west-1:acc:credential:1234";

    private static final Platform PLATFORM = platform("AWS");

    private static final Region REGION = region("eu-central-1");

    private static final List<String> X86_INSTANCE_TYPE = List.of("m5.2xlarge");

    private static final List<String> ARM_INSTANCE_TYPE = List.of("m6g.2xlarge");

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialToExtendedCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private PlatformResources platformResources;

    @Mock
    private ExtendedCloudCredential extendedCloudCredential;

    @Mock
    private Credential credential;

    @InjectMocks
    private DefaultInstanceTypeProvider underTest;

    @Test
    void getForPlatformShouldReturnX86InstanceTypeWhenArchitectureIsX86() throws Exception {
        DefaultVmTypes vmTypes = new DefaultVmTypes(null, new ArchitectureVmTypes(X86_INSTANCE_TYPE, ARM_INSTANCE_TYPE));
        CloudRegions cloudRegions = new CloudRegions(
                Map.of(REGION, java.util.List.of()),
                Map.of(REGION, "EU (Frankfurt)"),
                Map.of(),
                Map.of(REGION, vmTypes),
                "eu-central-1",
                true);

        when(credentialService.getCredentialByCredCrn(CREDENTIAL_CRN)).thenReturn(credential);
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(extendedCloudCredential);
        when(cloudPlatformConnectors.getDefault(PLATFORM)).thenReturn(cloudConnector);
        when(cloudConnector.platformResources()).thenReturn(platformResources);
        when(platformResources.regions(eq(extendedCloudCredential), eq(REGION), any(), eq(false))).thenReturn(cloudRegions);

        List<String> result = underTest.getForPlatform(CREDENTIAL_CRN, PLATFORM, REGION, Architecture.X86_64);

        assertEquals(X86_INSTANCE_TYPE, result);
    }

    @Test
    void getForPlatformShouldReturnArmInstanceTypeWhenArchitectureIsArm64() throws Exception {
        DefaultVmTypes vmTypes = new DefaultVmTypes(null, new ArchitectureVmTypes(X86_INSTANCE_TYPE, ARM_INSTANCE_TYPE));
        CloudRegions cloudRegions = new CloudRegions(
                Map.of(REGION, java.util.List.of()),
                Map.of(REGION, "EU (Frankfurt)"),
                Map.of(),
                Map.of(REGION, vmTypes),
                "eu-central-1",
                true);

        when(credentialService.getCredentialByCredCrn(CREDENTIAL_CRN)).thenReturn(credential);
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(extendedCloudCredential);
        when(cloudPlatformConnectors.getDefault(PLATFORM)).thenReturn(cloudConnector);
        when(cloudConnector.platformResources()).thenReturn(platformResources);
        when(platformResources.regions(eq(extendedCloudCredential), eq(REGION), any(), eq(false))).thenReturn(cloudRegions);

        List<String> result = underTest.getForPlatform(CREDENTIAL_CRN, PLATFORM, REGION, Architecture.ARM64);

        assertEquals(ARM_INSTANCE_TYPE, result);
    }

    @Test
    void getForPlatformShouldReturnX86InstanceTypeWhenArchitectureIsUnknown() throws Exception {
        DefaultVmTypes vmTypes = new DefaultVmTypes(null, new ArchitectureVmTypes(X86_INSTANCE_TYPE, ARM_INSTANCE_TYPE));
        CloudRegions cloudRegions = new CloudRegions(
                Map.of(REGION, java.util.List.of()),
                Map.of(REGION, "EU (Frankfurt)"),
                Map.of(),
                Map.of(REGION, vmTypes),
                "eu-central-1",
                true);

        when(credentialService.getCredentialByCredCrn(CREDENTIAL_CRN)).thenReturn(credential);
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(extendedCloudCredential);
        when(cloudPlatformConnectors.getDefault(PLATFORM)).thenReturn(cloudConnector);
        when(cloudConnector.platformResources()).thenReturn(platformResources);
        when(platformResources.regions(eq(extendedCloudCredential), eq(REGION), any(), eq(false))).thenReturn(cloudRegions);

        List<String> result = underTest.getForPlatform(CREDENTIAL_CRN, PLATFORM, REGION, Architecture.UNKNOWN);

        assertEquals(X86_INSTANCE_TYPE, result);
    }

    @Test
    void getForPlatformShouldThrowCloudbreakRuntimeExceptionWhenUnderlyingCallFails() throws Exception {
        when(credentialService.getCredentialByCredCrn(CREDENTIAL_CRN)).thenReturn(credential);
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(extendedCloudCredential);
        when(cloudPlatformConnectors.getDefault(PLATFORM)).thenReturn(cloudConnector);
        when(cloudConnector.platformResources()).thenReturn(platformResources);
        when(platformResources.regions(eq(extendedCloudCredential), eq(REGION), any(), eq(false)))
                .thenThrow(new RuntimeException("connection failed"));

        assertThrows(CloudbreakRuntimeException.class,
                () -> underTest.getForPlatform(CREDENTIAL_CRN, PLATFORM, REGION, Architecture.X86_64));
    }
}
