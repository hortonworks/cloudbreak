package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import com.google.common.collect.Lists;
import com.microsoft.azure.management.resources.Deployment;
import com.sequenceiq.cloudbreak.cloud.azure.AzureContextService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.context.AzureContextBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.common.api.type.ResourceType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

@Service
public class AzureDatabaseResourceService {

    // PostgreSQL server port is fixed for now
    private static final int POSTGRESQL_SERVER_PORT = 5432;

    private static final String DATABASE_SERVER_FQDN = "databaseServerFQDN";

    @Inject
    private AzureContextBuilder contextBuilder;

    @Inject
    private AzureContextService azureContextService;

    @Inject
    private AzureTemplateBuilder azureTemplateBuilder;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private DefaultCostTaggingService defaultCostTaggingService;

    public List<CloudResourceStatus> buildDatabaseResourcesForLaunch(AuthenticatedContext ac, DatabaseStack stack) {
        CloudContext cloudContext = ac.getCloudContext();
        AzureClient client = ac.getParameter(AzureClient.class);

        String stackName = azureUtils.getStackName(cloudContext);
        String resourceGroupName = azureUtils.getResourceGroupName(cloudContext, stack);
        String template = azureTemplateBuilder.build(stackName, cloudContext, stack);
        String region = ac.getCloudContext().getLocation().getRegion().value();

        try {
            if (!client.resourceGroupExists(resourceGroupName)) {
                client.createResourceGroup(resourceGroupName, region, stack.getTags(), defaultCostTaggingService.prepareTemplateTagging());
            }
        } catch (Exception ex) {
            throw new CloudConnectorException(ex);
        }

        client.createTemplateDeployment(resourceGroupName, stackName, template, "");

        Deployment deployment = client.getTemplateDeployment(resourceGroupName, stackName);

        String fqdn = (String) ((Map) ((Map) deployment.outputs()).get(DATABASE_SERVER_FQDN)).get("value");

        List<CloudResource> databaseResources = Lists.newArrayList();

        databaseResources.add(CloudResource.builder()
                .type(ResourceType.RDS_HOSTNAME)
                .name(fqdn)
                .build());
        databaseResources.add(CloudResource.builder()
                .type(ResourceType.RDS_PORT)
                .name(Integer.toString(POSTGRESQL_SERVER_PORT))
                .build());

        return databaseResources.stream()
                .map(resource -> new CloudResourceStatus(resource, ResourceStatus.CREATED))
                .collect(Collectors.toList());
    }
}

