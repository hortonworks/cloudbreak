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
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) throws IOException {
        GcpContext gcpContext = gcpContextBuilder.contextInit(authenticatedContext.getCloudContext(), authenticatedContext, null, true);
        String project = gcpContext.getProjectId();
        String instanceName = cloudResource.getName();

        SQLAdmin sqlAdmin = gcpSQLAdminFactory.buildSQLAdmin(authenticatedContext.getCloudCredential(), authenticatedContext.getCloudCredential().getName());
        DatabaseInstance databaseInstance = sqlAdmin.instances().get(project, instanceName).execute();
        Settings existingSettings = databaseInstance.getSettings();

        Map<String, String> existingLabels = existingSettings.getUserLabels();
        if (tagsAlreadyUpToDate(existingLabels, tags)) {
            LOGGER.debug("Tags for database {} are already up to date, skipping update.", cloudResource.getName());
            return;
        }

        Map<String, String> mergedTags = mergeTags(existingLabels, tags);
        LOGGER.debug("Updating tags for database {} with tags {}", instanceName, mergedTags);
        DatabaseInstance patch = new DatabaseInstance()
                .setSettings(new Settings()
                        .setUserLabels(mergedTags)
                        .setSettingsVersion(existingSettings.getSettingsVersion()));

        sqlAdmin.instances()
                .patch(project, instanceName, patch)
                .execute();
    }
}
