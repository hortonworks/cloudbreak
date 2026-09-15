package com.sequenceiq.cloudbreak.cloud.gcp.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.cloudresourcemanager.v3.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.v3.model.TestIamPermissionsRequest;
import com.google.api.services.cloudresourcemanager.v3.model.TestIamPermissionsResponse;
import com.sequenceiq.cloudbreak.cloud.exception.TagUpdatePermissionMissingException;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpCloudResourceManagerFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@ExtendWith(MockitoExtension.class)
public class GcpEnvironmentTagUpdatePermissionValidatorTest {

    private static final String PROJECT_ID = "gcp-project-1";

    private static final List<String> REQUIRED_PERMISSIONS = List.of(
            "compute.instances.get",
            "compute.instances.setLabels",
            "compute.disks.get",
            "compute.disks.setLabels",
            "compute.addresses.get",
            "compute.addresses.setLabels",
            "compute.forwardingRules.get",
            "compute.forwardingRules.setLabels",
            "cloudsql.instances.get",
            "cloudsql.instances.update");

    @Mock
    private GcpCloudResourceManagerFactory cloudResourceManagerFactory;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudResourceManager cloudResourceManager;

    @Mock
    private CloudResourceManager.Projects projects;

    @Mock
    private CloudResourceManager.Projects.TestIamPermissions testIamPermissions;

    @InjectMocks
    private GcpEnvironmentTagUpdatePermissionValidator underTest;

    @Test
    void validatePassesWhenAllRequiredPermissionsAreGranted() throws Exception {
        stubCommonMocks();
        TestIamPermissionsResponse response = new TestIamPermissionsResponse().setPermissions(REQUIRED_PERMISSIONS);
        when(testIamPermissions.execute()).thenReturn(response);

        underTest.validate(cloudCredential);

        ArgumentCaptor<TestIamPermissionsRequest> requestCaptor = ArgumentCaptor.forClass(TestIamPermissionsRequest.class);
        verify(projects).testIamPermissions(eq("projects/" + PROJECT_ID), requestCaptor.capture());
        assertEquals(REQUIRED_PERMISSIONS, requestCaptor.getValue().getPermissions());
    }

    @Test
    void validateThrowsWithMissingPermissionsListWhenSomeAreDenied() throws Exception {
        stubCommonMocks();
        TestIamPermissionsResponse response = new TestIamPermissionsResponse().setPermissions(List.of(
                "compute.instances.get",
                "compute.instances.setLabels",
                "compute.disks.get",
                "compute.disks.setLabels"));
        when(testIamPermissions.execute()).thenReturn(response);

        TagUpdatePermissionMissingException thrown = assertThrows(TagUpdatePermissionMissingException.class,
                () -> underTest.validate(cloudCredential));

        assertEquals(List.of(
                "compute.addresses.get",
                "compute.addresses.setLabels",
                "compute.forwardingRules.get",
                "compute.forwardingRules.setLabels",
                "cloudsql.instances.get",
                "cloudsql.instances.update"), thrown.getFailedActions());
        assertTrue(thrown.getMessage().contains(PROJECT_ID));
        assertTrue(thrown.getMessage().contains("compute.addresses.setLabels"));
        assertNull(thrown.getCause());
    }

    @Test
    void validateThrowsWhenOnlyAReadPermissionIsMissing() throws Exception {
        stubCommonMocks();
        List<String> everythingButDiskRead = REQUIRED_PERMISSIONS.stream()
                .filter(permission -> !"compute.disks.get".equals(permission))
                .toList();
        when(testIamPermissions.execute()).thenReturn(new TestIamPermissionsResponse().setPermissions(everythingButDiskRead));

        TagUpdatePermissionMissingException thrown = assertThrows(TagUpdatePermissionMissingException.class,
                () -> underTest.validate(cloudCredential));

        assertEquals(List.of("compute.disks.get"), thrown.getFailedActions());
        assertTrue(thrown.getMessage().contains("compute.disks.get"));
    }

    @Test
    void validateRequiresAReadPermissionForEveryWritePermission() throws Exception {
        stubCommonMocks();
        when(testIamPermissions.execute()).thenReturn(new TestIamPermissionsResponse().setPermissions(REQUIRED_PERMISSIONS));

        underTest.validate(cloudCredential);

        ArgumentCaptor<TestIamPermissionsRequest> requestCaptor = ArgumentCaptor.forClass(TestIamPermissionsRequest.class);
        verify(projects).testIamPermissions(eq("projects/" + PROJECT_ID), requestCaptor.capture());
        List<String> requested = requestCaptor.getValue().getPermissions();
        assertTrue(requested.contains("compute.instances.get"), "GcpInstanceTagUpdateStrategy reads the instance before setLabels");
        assertTrue(requested.contains("compute.disks.get"), "GcpDiskTagUpdateStrategy reads the disk before setLabels");
        assertTrue(requested.contains("compute.addresses.get"), "GcpAddressTagUpdateStrategy reads the address before setLabels");
        assertTrue(requested.contains("compute.forwardingRules.get"), "GcpForwardingRuleTagUpdateStrategy reads the rule before setLabels");
        assertTrue(requested.contains("cloudsql.instances.get"), "GcpDatabaseTagUpdateStrategy reads the instance before patch");
    }

    @Test
    void validateTreatsNullPermissionsResponseAsAllMissing() throws Exception {
        stubCommonMocks();
        when(testIamPermissions.execute()).thenReturn(new TestIamPermissionsResponse());

        TagUpdatePermissionMissingException thrown = assertThrows(TagUpdatePermissionMissingException.class,
                () -> underTest.validate(cloudCredential));

        assertEquals(REQUIRED_PERMISSIONS, thrown.getFailedActions());
    }

    @Test
    void validateWrapsIoExceptionFromTestIamPermissions() throws Exception {
        stubCommonMocks();
        IOException ioe = new IOException("permission denied by org policy");
        when(testIamPermissions.execute()).thenThrow(ioe);

        TagUpdatePermissionMissingException thrown = assertThrows(TagUpdatePermissionMissingException.class,
                () -> underTest.validate(cloudCredential));

        assertSame(ioe, thrown.getCause());
        assertTrue(thrown.getMessage().contains(PROJECT_ID));
        assertTrue(thrown.getMessage().contains("permission denied by org policy"));
    }

    @Test
    void supportedPlatformIsGcp() {
        assertEquals("GCP", underTest.supportedPlatform());
    }

    private void stubCommonMocks() throws IOException {
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn(PROJECT_ID);
        when(cloudResourceManagerFactory.buildCloudResourceManager(cloudCredential)).thenReturn(cloudResourceManager);
        when(cloudResourceManager.projects()).thenReturn(projects);
        when(projects.testIamPermissions(any(String.class), any(TestIamPermissionsRequest.class))).thenReturn(testIamPermissions);
    }
}
