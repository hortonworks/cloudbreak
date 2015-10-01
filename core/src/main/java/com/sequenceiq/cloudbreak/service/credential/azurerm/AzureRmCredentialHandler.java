package com.sequenceiq.cloudbreak.service.credential.azurerm;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureRMClient;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.AzureRmCredential;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.service.credential.CredentialHandler;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;

import groovyx.net.http.HttpResponseException;

@Component
public class AzureRmCredentialHandler implements CredentialHandler<AzureRmCredential> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureRmCredentialHandler.class);

    @Inject
    private AzureStackUtil azureStackUtil;

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE_RM;
    }

    @Override
    public AzureRmCredential init(AzureRmCredential azureCredential) {
        try {
            AzureRMClient azureRMClient = new AzureRMClient(azureCredential.getTenantId(), azureCredential.getAccesKey(),
                    azureCredential.getSecretKey(), azureCredential.getSubscriptionId());
            azureRMClient.getToken();
        } catch (HttpResponseException ex) {
            LOGGER.error(ex.getResponse().getData().toString(), ex);
            throw new BadRequestException(ex.getResponse().getData().toString(), ex);
        } catch (NullPointerException ex) {
            LOGGER.error("Invalid App ID or Tenant ID or Password.", ex);
            throw new BadRequestException("Invalid App ID or Tenant ID or Password.", ex);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            throw new BadRequestException(ex.getMessage(), ex);
        }
        return azureCredential;
    }

    @Override
    public boolean delete(AzureRmCredential credential) {
        return true;
    }

    @Override
    public AzureRmCredential update(AzureRmCredential credential) throws Exception {
        return credential;
    }

}
