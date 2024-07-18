package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.common.model.AzureDatabaseType;

public interface AzureDatabaseTemplateModelBuilder {
    Map<String, Object> buildModel(AzureDatabaseServerView azureDatabaseServerView, AzureNetworkView azureNetworkView,
            DatabaseStack databaseStack);

    AzureDatabaseType azureDatabaseType();
}