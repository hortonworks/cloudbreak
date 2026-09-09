package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.DatabaseInstance;
import com.google.api.services.sqladmin.model.Settings;
import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpSQLAdminFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class GcpDatabaseTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDatabaseTagUpdateStrategy.class);

    @Inject
    private GcpSQLAdminFactory gcpSQLAdminFactory;

    @Inject
    private GcpContextBuilder gcpContextBuilder;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(ResourceType.GCP_DATABASE);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> labels) throws IOException {
        SQLAdmin sqlAdmin = sqlAdmin(authenticatedContext);
        String project = projectId(authenticatedContext);
        String instanceName = cloudResource.getName();
        updateInstanceLabels(sqlAdmin, project, instanceName, labels);
    }

    @Override
    public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) throws IOException {
        SQLAdmin sqlAdmin = sqlAdmin(authenticatedContext);
        String project = projectId(authenticatedContext);
        String instanceName = cloudResource.getName();
        deleteInstanceLabels(sqlAdmin, project, instanceName, tagKeys);
    }

    private void updateInstanceLabels(SQLAdmin sqlAdmin, String project, String instanceName, Map<String, String> newLabels) throws IOException {
        Settings existingSettings = fetchSettings(sqlAdmin, project, instanceName);
        Map<String, String> existingLabels = existingSettings.getUserLabels();
        if (tagsAlreadyUpToDate(existingLabels, newLabels)) {
            LOGGER.debug("Labels for database {} are already up to date, skipping update.", instanceName);
        } else {
            Map<String, String> mergedLabels = mergeTags(existingLabels, newLabels);
            logTagUpdate(LOGGER, instanceName, mergedLabels);
            patchUserLabels(sqlAdmin, project, instanceName, mergedLabels, existingSettings.getSettingsVersion());
        }
    }

    private void deleteInstanceLabels(SQLAdmin sqlAdmin, String project, String instanceName, Set<String> labelKeys) throws IOException {
        Settings existingSettings = fetchSettings(sqlAdmin, project, instanceName);
        Map<String, String> existingLabels = existingSettings.getUserLabels();
        if (hasTagKeysToDelete(existingLabels, labelKeys)) {
            Map<String, String> remainingLabels = removeTagKeys(existingLabels, labelKeys);
            logTagDeletion(LOGGER, instanceName, labelKeys, existingLabels, remainingLabels.keySet());
            patchUserLabels(sqlAdmin, project, instanceName, remainingLabels, existingSettings.getSettingsVersion());
        } else {
            LOGGER.debug("No labels to delete for database {}, skipping.", instanceName);
        }
    }

    private Settings fetchSettings(SQLAdmin sqlAdmin, String project, String instanceName) throws IOException {
        return sqlAdmin.instances().get(project, instanceName).execute().getSettings();
    }

    private void patchUserLabels(SQLAdmin sqlAdmin, String project, String instanceName, Map<String, String> labels, Long settingsVersion)
            throws IOException {
        DatabaseInstance patch = new DatabaseInstance()
                .setSettings(new Settings()
                        .setUserLabels(labels)
                        .setSettingsVersion(settingsVersion));
        sqlAdmin.instances().patch(project, instanceName, patch).execute();
    }

    private SQLAdmin sqlAdmin(AuthenticatedContext authenticatedContext) {
        return gcpSQLAdminFactory.buildSQLAdmin(authenticatedContext.getCloudCredential(), authenticatedContext.getCloudCredential().getName());
    }

    private String projectId(AuthenticatedContext authenticatedContext) throws IOException {
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        return gcpContext.getProjectId();
    }
}
