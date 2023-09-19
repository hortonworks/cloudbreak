package com.sequenceiq.cloudbreak.cloud.azure.connector.resource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.storage.models.StorageAccountSkuType;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorageAccountTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

@ExtendWith(MockitoExtension.class)
class AzureStorageAccountBuilderServiceTest {

    private static final String POLICY_NAME = "dbajzath-azure-restricted-repro";

    private static final String SOURCE_DESCRIPTION = "(String)\"{\"policyDefinitionDisplayName\":\"" + POLICY_NAME + "\"," +
            "\"evaluationDetails\":{\"evaluatedExpressions\":[{\"result\":\"True\",\"expressionKind\":\"Field\",\"expression\":\"type\",\"path\":\"type\"," +
            "\"expressionValue\":\"Microsoft.Storage/storageAccounts\",\"targetValue\":\"Microsoft.Storage/storageAccounts\",\"operator\":\"Equals\"}," +
            "{\"result\":\"False\",\"expressionKind\":\"Field\",\"expression\":\"Microsoft.Storage/storageAccounts/minimumTlsVersion\",\"path\":" +
            "\"properties.minimumTlsVersion\",\"targetValue\":\"TLS1_2\",\"operator\":\"Equals\"[truncated 1005 chars]";

    private static final StorageAccountParameters STORAGE_ACCOUNT_PARAMETERS = new StorageAccountParameters("resourceGroupName", "storageAccountName", "",
            StorageAccountSkuType.STANDARD_LRS, Map.of());

    @InjectMocks
    private AzureStorageAccountBuilderService underTest;

    @Mock
    private AzureStorageAccountTemplateBuilder azureStorageAccountTemplateBuilder;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureClient client;

    @BeforeEach
    void setUp() {
        when(azureStorageAccountTemplateBuilder.build(any())).thenReturn("");
        when(client.getTemplateDeploymentStatus(anyString(), anyString())).thenReturn(ResourceStatus.CREATED);
    }

    @Test
    void shouldAddPolicyNameToException() {
        RuntimeException mismatchedInputException = createMismatchedInputException();
        when(client.createTemplateDeployment(anyString(), anyString(), anyString(), anyString())).thenThrow(mismatchedInputException);

        assertThatThrownBy(() -> underTest.buildStorageAccount(client, STORAGE_ACCOUNT_PARAMETERS))
                .isInstanceOf(CloudConnectorException.class)
                .hasMessage("Could not create storage account %s in resource group %s, because it was denied by policy '%s'",
                        "storageAccountName", "resourceGroupName", POLICY_NAME);
    }

    private RuntimeException createMismatchedInputException() {
        JsonLocation jsonLocation = mock(JsonLocation.class);
        when(jsonLocation.sourceDescription()).thenReturn(SOURCE_DESCRIPTION);
        MismatchedInputException mismatchedInputException = new InvalidFormatException("msg", jsonLocation, "", String.class);
        return new RuntimeException("Unknown error with status code 400", mismatchedInputException);
    }

}
