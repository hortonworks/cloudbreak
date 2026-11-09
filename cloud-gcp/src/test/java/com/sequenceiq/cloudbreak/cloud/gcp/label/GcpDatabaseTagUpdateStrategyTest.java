package com.sequenceiq.cloudbreak.cloud.gcp.label;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Data;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.Settings;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.tag.GcpDatabaseTagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpDatabaseTagUpdateStrategyTest {

    private static final String CLOUD_CREDENTIAL_NAME = "cloudCredentialName";

    private static final String PROJECT_ID = "test-project";

    private static final String ZONE = "us-central1-a";

    private static final String RESOURCE_NAME = "test-resource";

    private static final Long SETTINGS_VERSION = 1L;

    private static final Map<String, String> EXISTING_LABELS = Map.of("existingKey", "existingValue");

    private static final Map<String, String> NEW_LABELS = Map.of("newKey", "newValue");

    private static final Map<String, String> MERGED_LABELS = Map.of("existingKey", "existingValue", "newKey", "newValue");

    @Mock
    private GcpContext gcpContext;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private SQLAdmin sqlAdmin;

    @Mock
    private SQLAdmin.Instances sqlAdminInstances;

    @Mock
    private SQLAdmin.Instances.Get sqlAdminInstancesGet;

    @Mock
    private SQLAdmin.Instances.Patch sqlAdminInstancesPatch;

    @Mock
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private GcpContextBuilder gcpContextBuilder;

    @InjectMocks
    private GcpDatabaseTagUpdateStrategy underTest;

    @BeforeEach
    void setUp() throws Exception {
        when(gcpContext.getProjectId()).thenReturn(PROJECT_ID);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(gcpContextBuilder.contextInit(cloudContext, authenticatedContext, null, true)).thenReturn(gcpContext);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudCredential.getName()).thenReturn(CLOUD_CREDENTIAL_NAME);
        when(gcpSQLAdminFactory.buildSQLAdmin(cloudCredential, CLOUD_CREDENTIAL_NAME)).thenReturn(sqlAdmin);
        when(sqlAdmin.instances()).thenReturn(sqlAdminInstances);
        when(sqlAdminInstances.get(PROJECT_ID, RESOURCE_NAME)).thenReturn(sqlAdminInstancesGet);
    }

    @Test
    void testDeleteDatabaseLabelsSendsNullValueForDeletedKeysOnly() throws Exception {
        mockExistingLabels(Map.of("existingKey", "existingValue", "keptKey", "keptValue"));
        when(sqlAdminInstances.patch(eq(PROJECT_ID), eq(RESOURCE_NAME), any())).thenReturn(sqlAdminInstancesPatch);

        underTest.deleteTags(authenticatedContext, cloudResource(), Set.of("existingKey", "absentKey"));

        DatabaseInstance capturedPatch = capturePatch();
        Map<String, String> patchedLabels = capturedPatch.getSettings().getUserLabels();
        assertEquals(Set.of("existingKey"), patchedLabels.keySet());
        assertTrue(Data.isNull(patchedLabels.get("existingKey")));
        assertEquals(SETTINGS_VERSION, capturedPatch.getSettings().getSettingsVersion());
        String patchJson = JacksonFactory.getDefaultInstance().toString(capturedPatch);
        assertTrue(patchJson.contains("\"userLabels\":{\"existingKey\":null}"), patchJson);
    }

    @Test
    void testDeleteTagsSkipWhenKeyNotPresent() throws Exception {
        mockExistingLabels(Map.of("otherKey", "otherValue"));

        underTest.deleteTags(authenticatedContext, cloudResource(), Set.of("existingKey"));

        verify(sqlAdminInstances, never()).patch(any(), any(), any());
    }

    @Test
    void testDeleteTagsSkipWhenInstanceHasNoLabels() throws Exception {
        mockExistingLabels(null);

        underTest.deleteTags(authenticatedContext, cloudResource(), Set.of("existingKey"));

        verify(sqlAdminInstances, never()).patch(any(), any(), any());
    }

    @Test
    void testUpdateDatabaseLabels() throws Exception {
        mockExistingLabels(EXISTING_LABELS);
        when(sqlAdminInstances.patch(eq(PROJECT_ID), eq(RESOURCE_NAME), any())).thenReturn(sqlAdminInstancesPatch);

        underTest.updateTags(authenticatedContext, cloudResource(), NEW_LABELS);

        DatabaseInstance capturedPatch = capturePatch();
        assertEquals(MERGED_LABELS, capturedPatch.getSettings().getUserLabels());
        assertEquals(SETTINGS_VERSION, capturedPatch.getSettings().getSettingsVersion());
    }

    private void mockExistingLabels(Map<String, String> labels) throws Exception {
        Settings existingSettings = new Settings()
                .setUserLabels(labels)
                .setSettingsVersion(SETTINGS_VERSION);
        when(sqlAdminInstancesGet.execute()).thenReturn(new DatabaseInstance().setSettings(existingSettings));
    }

    private DatabaseInstance capturePatch() throws Exception {
        ArgumentCaptor<DatabaseInstance> patchCaptor = ArgumentCaptor.forClass(DatabaseInstance.class);
        verify(sqlAdminInstances).patch(eq(PROJECT_ID), eq(RESOURCE_NAME), patchCaptor.capture());
        verify(sqlAdminInstancesPatch).execute();
        return patchCaptor.getValue();
    }

    private CloudResource cloudResource() {
        return CloudResource.builder()
                .withType(ResourceType.GCP_DATABASE)
                .withName(RESOURCE_NAME)
                .withAvailabilityZone(ZONE)
                .build();
    }
}
