package com.sequenceiq.environment.platformresource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CredentialToExtendedCloudCredentialConverter;

@ExtendWith(SpringExtension.class)
class PlatformParameterServiceTest {

    public static final String ACCOUNT_ID = "accid";

    public static final String CREDENTIAL_NAME = "credname";

    public static final String CREDENTIAL_CRN = "crn";

    public static final String REGION = "region";

    public static final String PLATFORM_VARIANT = "variant";

    public static final String AVAILIBILTY_ZONE = "availabilityZone";

    public static final String EMPTY = "";

    @MockBean
    private CloudParameterService cloudParameterService;

    @MockBean
    private CredentialService credentialService;

    @MockBean
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Inject
    private PlatformParameterService platformParameterServiceUnderTest;

    private PlatformResourceRequest request;

    @BeforeEach
    public void setup() {
        request = new PlatformResourceRequest();
        request.setPlatformVariant(PLATFORM_VARIANT);
        request.setRegion(REGION);
    }

    @Test
    void getPlatformResourceRequestWithoutCred() {
        assertThrows(BadRequestException.class, () -> platformParameterServiceUnderTest.getPlatformResourceRequest(ACCOUNT_ID,
                null, null, REGION, PLATFORM_VARIANT, AVAILIBILTY_ZONE));
    }

    @Test
    void getPlatformResourceRequestCredentialByName() {
        final Credential credential = new Credential();
        credential.setCloudPlatform("anotherVariant");
        when(credentialService.getByNameForAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID))).thenReturn(credential);
        PlatformResourceRequest result = platformParameterServiceUnderTest.getPlatformResourceRequest(ACCOUNT_ID, CREDENTIAL_NAME, null,
                REGION, PLATFORM_VARIANT, AVAILIBILTY_ZONE);
        assertEquals(credential, result.getCredential());
    }

    @Test
    void getPlatformResourceRequestCredentialByCrn() {
        final Credential credential = new Credential();
        credential.setCloudPlatform("anotherVariant");
        when(credentialService.getByCrnForAccountId(eq(CREDENTIAL_CRN), eq(ACCOUNT_ID))).thenReturn(credential);
        PlatformResourceRequest result = platformParameterServiceUnderTest.getPlatformResourceRequest(ACCOUNT_ID, null, CREDENTIAL_CRN,
                REGION, PLATFORM_VARIANT, AVAILIBILTY_ZONE);
        assertEquals(credential, result.getCredential());
    }

    @Test
    void getPlatformResourceRequestSetPlatformFromCredential() {
        final Credential credential = new Credential();
        String platformFromCredential = "anotherVariant";
        credential.setCloudPlatform(platformFromCredential);
        when(credentialService.getByNameForAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID))).thenReturn(credential);
        PlatformResourceRequest result = platformParameterServiceUnderTest.getPlatformResourceRequest(ACCOUNT_ID, CREDENTIAL_NAME, null,
                null, EMPTY, null);
        assertEquals(platformFromCredential, result.getCloudPlatform());
        assertEquals(platformFromCredential, result.getPlatformVariant());
        assertEquals(null, result.getRegion());
        assertEquals(null, result.getAvailabilityZone());
    }

    @Test
    void getPlatformResourceRequestSetPlatformFromArgument() {
        final Credential credential = new Credential();
        String platformFromCredential = "anotherVariant";
        credential.setCloudPlatform(platformFromCredential);
        when(credentialService.getByNameForAccountId(eq(CREDENTIAL_NAME), eq(ACCOUNT_ID))).thenReturn(credential);
        PlatformResourceRequest result = platformParameterServiceUnderTest.getPlatformResourceRequest(ACCOUNT_ID, CREDENTIAL_NAME, null,
                REGION, PLATFORM_VARIANT, AVAILIBILTY_ZONE);
        assertEquals(platformFromCredential, result.getCloudPlatform());
        assertEquals(null, result.getPlatformVariant());
        assertEquals(REGION, result.getRegion());
        assertEquals(AVAILIBILTY_ZONE, result.getAvailabilityZone());

    }

    @Test
    void getVmTypesByCredential() {
        platformParameterServiceUnderTest.getVmTypesByCredential(request);
        verify(cloudParameterService).getVmTypesV2(any(), eq(REGION), eq(PLATFORM_VARIANT), eq(CdpResourceType.DEFAULT), any());
    }

    @Test
    void getVmTypesByCredentialBadRequest() {
        request.setRegion(null);
        assertThrows(BadRequestException.class, () -> platformParameterServiceUnderTest.getVmTypesByCredential(request));
    }

    @Test
    void getRegionsByCredential() {
        platformParameterServiceUnderTest.getRegionsByCredential(request);
        verify(cloudParameterService).getRegionsV2(any(), eq(REGION), eq(PLATFORM_VARIANT), any());
    }

    @Test
    void getDiskTypes() {
        platformParameterServiceUnderTest.getDiskTypes();
        verify(cloudParameterService).getDiskTypes();
    }

    @Test
    void getCloudNetworks() {
        platformParameterServiceUnderTest.getCloudNetworks(request);
        verify(cloudParameterService).getCloudNetworks(any(), eq(REGION), eq(PLATFORM_VARIANT), any());
    }

    @Test
    void getIpPoolsCredentialId() {
        platformParameterServiceUnderTest.getIpPoolsCredentialId(request);
        verify(cloudParameterService).getPublicIpPools(any(), eq(REGION), eq(PLATFORM_VARIANT), any());
    }

    @Test
    void getGatewaysCredentialId() {
        platformParameterServiceUnderTest.getGatewaysCredentialId(request);
        verify(cloudParameterService).getGateways(any(), eq(REGION), eq(PLATFORM_VARIANT), any());
    }

    @Test
    void getEncryptionKeys() {
        platformParameterServiceUnderTest.getEncryptionKeys(request);
        verify(cloudParameterService).getCloudEncryptionKeys(any(), eq(REGION), eq(PLATFORM_VARIANT), any());
    }

    @Test
    void getSecurityGroups() {
        platformParameterServiceUnderTest.getSecurityGroups(request);
        verify(cloudParameterService).getSecurityGroups(any(), eq(REGION), eq(PLATFORM_VARIANT), any());
    }

    @Test
    void getCloudSshKeys() {
        platformParameterServiceUnderTest.getCloudSshKeys(request);
        verify(cloudParameterService).getCloudSshKeys(any(), eq(REGION), eq(PLATFORM_VARIANT), any());
    }

    @Test
    void getAccessConfigs() {
        platformParameterServiceUnderTest.getAccessConfigs(request);
        verify(cloudParameterService).getCloudAccessConfigs(any(), eq(REGION), eq(PLATFORM_VARIANT), any());
    }

    @Test
    void getPlatformParameters() {
        platformParameterServiceUnderTest.getPlatformParameters();
        verify(cloudParameterService).getPlatformParameters();
    }

    @Test
    void getNoSqlTables() {
        platformParameterServiceUnderTest.getNoSqlTables(request);
        verify(cloudParameterService).getNoSqlTables(any(), eq(REGION), eq(PLATFORM_VARIANT), any());
    }

    @Configuration
    @Import(PlatformParameterService.class)
    static class Config {

    }
}
