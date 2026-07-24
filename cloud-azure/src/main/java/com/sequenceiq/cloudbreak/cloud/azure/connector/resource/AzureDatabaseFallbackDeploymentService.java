package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.azure.core.management.exception.ManagementException;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDatabaseTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.InsufficientCapacityException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.retry.Retry;
import com.sequenceiq.common.model.AzureDatabaseType;

@Service
public class AzureDatabaseFallbackDeploymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDatabaseFallbackDeploymentService.class);

    @Inject
    private AzureDatabaseTemplateBuilder azureDatabaseTemplateBuilder;

    @Inject
    private AzureExceptionHandler azureExceptionHandler;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    public String deployWithFallback(String stackName, String resourceGroupName, AzureClient client,
            CloudContext cloudContext, DatabaseStack databaseStack) {
        DatabaseServer databaseServer = databaseStack.getDatabaseServer();
        List<String> fallbackTypes = databaseServer.getFallbackInstanceTypes();
        AzureDatabaseServerView view = new AzureDatabaseServerView(databaseServer);

        boolean fallbackEnabled = !CollectionUtils.isEmpty(fallbackTypes)
                && AzureDatabaseType.FLEXIBLE_SERVER.equals(view.getAzureDatabaseType());

        if (!fallbackEnabled) {
            String template = azureDatabaseTemplateBuilder.build(cloudContext, databaseStack);
            submitDeploymentWithConflictRetry(stackName, resourceGroupName, template, client);
            return null;
        }

        int maxAttempts = 1 + fallbackTypes.size();
        ManagementException lastException = null;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            String skuForAttempt = attempt == 0 ? databaseServer.getFlavor() : fallbackTypes.get(attempt - 1);
            DatabaseStack stackForAttempt = attempt == 0 ? databaseStack : buildDatabaseStackWithFlavor(databaseStack, skuForAttempt);
            String template = azureDatabaseTemplateBuilder.build(cloudContext, stackForAttempt);

            try {
                submitDeploymentWithConflictRetry(stackName, resourceGroupName, template, client);
                if (attempt > 0) {
                    LOGGER.info("DB ARM deployment {}/{} succeeded on fallback attempt {} with SKU {}.",
                            resourceGroupName, stackName, attempt, skuForAttempt);
                    return skuForAttempt;
                }
                return null;
            } catch (ManagementException e) {
                lastException = e;
                if (!azureExceptionHandler.isCapacityError(e)) {
                    LOGGER.debug("DB ARM deployment {}/{} failed with non-capacity error on attempt {} for SKU {}; not retrying.",
                            resourceGroupName, stackName, attempt, skuForAttempt);
                    throw e;
                }
                LOGGER.info("DB ARM deployment {}/{} failed with capacity error on attempt {} for SKU {}; will try next fallback.",
                        resourceGroupName, stackName, attempt, skuForAttempt);
            }
        }

        throw new InsufficientCapacityException(
                String.format("DB ARM deployment %s/%s: all %d fallback instance types exhausted.",
                        resourceGroupName, stackName, fallbackTypes.size()),
                lastException);
    }

    private DatabaseStack buildDatabaseStackWithFlavor(DatabaseStack original, String flavor) {
        DatabaseServer dbServer = DatabaseServer.builder(original.getDatabaseServer())
                .withFlavor(flavor)
                .build();
        return new DatabaseStack(original.getNetwork(), dbServer, original.getTags(), original.getTemplate());
    }

    private void submitDeploymentWithConflictRetry(String stackName, String resourceGroupName, String template, AzureClient client) {
        String parameters = new Json(java.util.Map.of()).getValue();
        try {
            retryService.testWith2SecDelayMax5Times(() -> {
                try {
                    client.createTemplateDeployment(resourceGroupName, stackName, template, parameters);
                } catch (ManagementException e) {
                    if (azureExceptionHandler.isExceptionCodeConflict(e)) {
                        LOGGER.info("DB deployment {}/{} conflict (409), retrying.", resourceGroupName, stackName);
                        throw Retry.ActionFailedException.ofCause(e);
                    }
                    throw e;
                }
            });
        } catch (Retry.ActionFailedException e) {
            throw (ManagementException) e.getCause();
        }
    }

}
