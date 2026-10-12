package com.sequenceiq.environment.environment.flow.encryptionprofile.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.ForbiddenException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileValidatorTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:accountId:environment:ac5ba74b-c35e-45e9-9f47-123456789876";

    private static final boolean DATALAKE = true;

    private static final boolean DATAHUB = false;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private EncryptionProfileClusterResolver encryptionProfileClusterResolver;

    @InjectMocks
    private EncryptionProfileValidator underTest;

    @Test
    void testEntitlement() {
        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(false);

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(ENVIRONMENT_CRN)));

        assertEquals("Account not entitled for encryption profile. Please contact your CDP administrator to enable it.", ex.getMessage());
    }

    @Test
    void testRuntimeVersion() {
        StackViewV4Response stack1 = new StackViewV4Response();
        stack1.setName("stack1");
        stack1.setStackVersion("7.3.1");
        StackViewV4Response stack2 = new StackViewV4Response();
        stack2.setName("stack2");
        stack2.setStackVersion("7.3.2");
        StackViewV4Response stack3 = new StackViewV4Response();
        stack3.setName("stack3");
        stack3.setStackVersion("7.2.18");
        StackViewV4Response stack4 = new StackViewV4Response();
        stack4.setName("stack4");
        stack4.setStackVersion("7.3.3");
        List<StackViewV4Response> stacks = List.of(stack1, stack2, stack3, stack4);

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(stacks);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(ENVIRONMENT_CRN)));

        assertEquals("All clusters runtime need to be 7.3.2 or above to enable encryption profile. Upgrade cluster(s): stack1,stack3", ex.getMessage());
    }

    @Test
    void testStackStatus() {
        StackViewV4Response stack1 = stack("stack1", "7.3.3", Status.AVAILABLE, Status.AVAILABLE, DATAHUB);
        StackViewV4Response stack2 = stack("stack2", "7.3.4", Status.STOPPED, Status.STOPPED, DATAHUB);
        StackViewV4Response stack3 = stack("stack3", "7.3.2", Status.DELETE_FAILED, Status.AVAILABLE, DATALAKE);
        StackViewV4Response stack4 = stack("stack4", "7.4.0", Status.AVAILABLE, Status.UPDATE_FAILED, DATAHUB);
        StackViewV4Response stack5 = stack("stack5", "7.4.0", Status.MAINTENANCE_MODE_ENABLED, Status.AVAILABLE, DATAHUB);
        StackViewV4Response stack6 = stack("stack6", "7.4.0", Status.AVAILABLE, Status.MAINTENANCE_MODE_ENABLED, DATAHUB);
        List<StackViewV4Response> stacks = List.of(stack1, stack2, stack3, stack4, stack5, stack6);

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(stacks);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(ENVIRONMENT_CRN)));

        assertEquals("All Data Lake and Data Hub clusters that inherit the environment encryption profile need to be available "
                + "and not in maintenance mode to enable encryption profile. Cluster(s) not available: stack2,stack3,stack4,stack5,stack6", ex.getMessage());
    }

    @Test
    void testNotAvailableStackWithOwnEncryptionProfileIsIgnored() {
        StackViewV4Response datalake = stack("dl", "7.3.3", Status.AVAILABLE, Status.AVAILABLE, DATALAKE);
        StackViewV4Response stoppedWithOverride = stack("dh-override", "7.3.3", Status.STOPPED, Status.STOPPED, DATAHUB);
        StackViewV4Response stoppedWithoutOverride = stack("dh-inherit", "7.3.3", Status.STOPPED, Status.STOPPED, DATAHUB);

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENVIRONMENT_CRN))
                .thenReturn(List.of(datalake, stoppedWithOverride, stoppedWithoutOverride));
        when(encryptionProfileClusterResolver.hasOwnEncryptionProfile(stoppedWithOverride)).thenReturn(true);
        when(encryptionProfileClusterResolver.hasOwnEncryptionProfile(stoppedWithoutOverride)).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(ENVIRONMENT_CRN)));

        assertEquals("All Data Lake and Data Hub clusters that inherit the environment encryption profile need to be available "
                + "and not in maintenance mode to enable encryption profile. Cluster(s) not available: dh-inherit", ex.getMessage());
    }

    @Test
    void testAllNotAvailableStacksHaveOwnEncryptionProfile() {
        StackViewV4Response datalake = stack("dl", "7.3.3", Status.AVAILABLE, Status.AVAILABLE, DATALAKE);
        StackViewV4Response stoppedWithOverride = stack("dh-override", "7.3.3", Status.STOPPED, Status.STOPPED, DATAHUB);

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(List.of(datalake, stoppedWithOverride));
        when(encryptionProfileClusterResolver.hasOwnEncryptionProfile(stoppedWithOverride)).thenReturn(true);

        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(ENVIRONMENT_CRN)));
    }

    @Test
    void testEncryptionProfileLookupFailure() {
        StackViewV4Response stopped = stack("dh1", "7.3.3", Status.STOPPED, Status.STOPPED, DATAHUB);

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(List.of(stopped));
        when(encryptionProfileClusterResolver.hasOwnEncryptionProfile(stopped)).thenThrow(new DatahubOperationFailedException("boom"));

        DatahubOperationFailedException ex = assertThrows(DatahubOperationFailedException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(ENVIRONMENT_CRN)));

        assertEquals("boom", ex.getMessage());
    }

    @Test
    void testMultipleDatalakes() {
        StackViewV4Response datalake1 = stack("dl1", "7.3.3", Status.AVAILABLE, Status.AVAILABLE, DATALAKE);
        StackViewV4Response datalake2 = stack("dl2", "7.3.3", Status.AVAILABLE, Status.AVAILABLE, DATALAKE);
        StackViewV4Response datahub = stack("dh1", "7.3.3", Status.AVAILABLE, Status.AVAILABLE, DATAHUB);

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(List.of(datalake1, datalake2, datahub));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(ENVIRONMENT_CRN)));

        assertEquals("Encryption profile can be enabled only on environments with a single Data Lake. Data Lakes found: dl1,dl2", ex.getMessage());
    }

    @Test
    void testSingleDatalakeAndAvailableDatahubs() {
        StackViewV4Response datalake = stack("dl1", "7.3.3", Status.AVAILABLE, Status.AVAILABLE, DATALAKE);
        StackViewV4Response datahub1 = stack("dh1", "7.3.3", Status.AVAILABLE, Status.AVAILABLE, DATAHUB);
        StackViewV4Response datahub2 = stack("dh2", "7.3.3", Status.AVAILABLE, Status.AVAILABLE, DATAHUB);

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(List.of(datalake, datahub1, datahub2));

        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(ENVIRONMENT_CRN)));
        verify(encryptionProfileClusterResolver, never()).hasOwnEncryptionProfile(any());
    }

    @Test
    void testFreeIpaOnCentos7ThrowsException() {
        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        ImageSettingsResponse imageSettings = new ImageSettingsResponse();
        imageSettings.setOs("centos7");
        freeIpaResponse.setImage(imageSettings);
        when(freeIpaService.describe(ENVIRONMENT_CRN)).thenReturn(Optional.of(freeIpaResponse));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(ENVIRONMENT_CRN)));

        assertEquals("FreeIPA running on CentOS 7 which does not support to apply encryption profile.", ex.getMessage());
    }

    @Test
    void testFreeIpaOnRedhat8DoesNotThrow() {
        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        DescribeFreeIpaResponse freeIpaResponse = new DescribeFreeIpaResponse();
        ImageSettingsResponse imageSettings = new ImageSettingsResponse();
        imageSettings.setOs("redhat8");
        freeIpaResponse.setImage(imageSettings);
        when(freeIpaService.describe(ENVIRONMENT_CRN)).thenReturn(Optional.of(freeIpaResponse));
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(List.of());
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setName("env1");

        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(ENVIRONMENT_CRN)));
    }

    @Test
    void testFreeIpaNotFoundDoesNotThrow() {
        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(freeIpaService.describe(ENVIRONMENT_CRN)).thenReturn(Optional.empty());
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(List.of());
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setName("env1");

        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(ENVIRONMENT_CRN)));
    }

    private StackViewV4Response stack(String name, String version, Status stackStatus, Status clusterStatus, boolean datalake) {
        StackViewV4Response stack = new StackViewV4Response();
        stack.setName(name);
        stack.setStackVersion(version);
        stack.setStatus(stackStatus);
        ClusterViewV4Response cluster = new ClusterViewV4Response();
        cluster.setStatus(clusterStatus);
        stack.setCluster(cluster);
        lenient().when(encryptionProfileClusterResolver.isDatalake(stack)).thenReturn(datalake);
        return stack;
    }
}