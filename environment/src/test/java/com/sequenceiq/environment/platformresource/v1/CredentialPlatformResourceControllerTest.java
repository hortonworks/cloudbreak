package com.sequenceiq.environment.platformresource.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.authorization.service.CustomCheckUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNoSqlTablesResponse;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;
import com.sequenceiq.environment.platformresource.v1.converter.CloudAccessConfigsToPlatformAccessConfigsV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudEncryptionKeysToPlatformEncryptionKeysV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudGatewayssToPlatformGatewaysV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudIpPoolsToPlatformIpPoolsV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudNetworksToPlatformNetworksV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudSecurityGroupsToPlatformSecurityGroupsV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudSshKeysToPlatformSshKeysV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.CloudVmTypesToPlatformVmTypesV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.PlatformDisksToPlatformDisksV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.PlatformRegionsToRegionV1ResponseConverter;
import com.sequenceiq.environment.platformresource.v1.converter.TagSpecificationsToTagSpecificationsV1ResponseConverter;

@ExtendWith(MockitoExtension.class)
class CredentialPlatformResourceControllerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:123:user:123";

    private static final String CREDENTIAL_CRN = "crn:cdp:environments:us-west-1:123:credential:123";

    @Mock
    private PlatformParameterService platformParameterService;

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private CloudVmTypesToPlatformVmTypesV1ResponseConverter cloudVmTypesToPlatformVmTypesV1ResponseConverter;

    @Mock
    private PlatformRegionsToRegionV1ResponseConverter platformRegionsToRegionV1ResponseConverter;

    @Mock
    private PlatformDisksToPlatformDisksV1ResponseConverter platformDisksToPlatformDisksV1ResponseConverter;

    @Mock
    private CloudNetworksToPlatformNetworksV1ResponseConverter cloudNetworksToPlatformNetworksV1ResponseConverter;

    @Mock
    private CloudIpPoolsToPlatformIpPoolsV1ResponseConverter cloudIpPoolsToPlatformIpPoolsV1ResponseConverter;

    @Mock
    private CloudGatewayssToPlatformGatewaysV1ResponseConverter cloudGatewayssToPlatformGatewaysV1ResponseConverter;

    @Mock
    private CloudEncryptionKeysToPlatformEncryptionKeysV1ResponseConverter cloudEncryptionKeysToPlatformEncryptionKeysV1ResponseConverter;

    @Mock
    private CloudSecurityGroupsToPlatformSecurityGroupsV1ResponseConverter cloudSecurityGroupsToPlatformSecurityGroupsV1ResponseConverter;

    @Mock
    private CloudSshKeysToPlatformSshKeysV1ResponseConverter cloudSshKeysToPlatformSshKeysV1ResponseConverter;

    @Mock
    private CloudAccessConfigsToPlatformAccessConfigsV1ResponseConverter cloudAccessConfigsToPlatformAccessConfigsV1ResponseConverter;

    @Mock
    private TagSpecificationsToTagSpecificationsV1ResponseConverter tagSpecificationsToTagSpecificationsV1ResponseConverter;

    @Mock
    private CloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverter cloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverter;

    @Mock
    private CustomCheckUtil customCheckUtil;

    @InjectMocks
    private CredentialPlatformResourceController underTest;

    @BeforeEach
    public void init() {
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(customCheckUtil).run(any(Runnable.class));
    }

    @Test
    void getNoSqlTables() {
        CloudNoSqlTables noSqlTables = new CloudNoSqlTables();
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        PlatformNoSqlTablesResponse response = new PlatformNoSqlTablesResponse();
        when(platformParameterService.getPlatformResourceRequest(any(), any(), any(), any(), any(), any())).thenReturn(platformResourceRequest);
        when(platformParameterService.getNoSqlTables(any(PlatformResourceRequest.class))).thenReturn(noSqlTables);
        when(cloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverter.convert(noSqlTables)).thenReturn(response);
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), any(), any());

        PlatformNoSqlTablesResponse result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getNoSqlTables(null,
                CREDENTIAL_CRN, "region", "aws", "az"));

        verify(platformParameterService).getNoSqlTables(platformResourceRequest);
        verify(cloudNoSqlTablesToPlatformNoSqlTablesV1ResponseConverter).convert(noSqlTables);
        assertEquals(response, result);

    }

    @Test
    void testGetNoSqlTablesShouldThrowBadRequestExceptionWhenTheSpecifiedCredentialCrnIsInvalid() {
        assertThrows(BadRequestException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getNoSqlTables(null,
                "new", "region", "aws", "az")));

    }
}
