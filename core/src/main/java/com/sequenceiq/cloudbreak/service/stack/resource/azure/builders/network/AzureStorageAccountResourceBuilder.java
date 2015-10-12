package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.network;

import javax.inject.Inject;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureResourceException;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureDeleteResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourcePollerObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@Component
@Order(2)
// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public class AzureStorageAccountResourceBuilder extends AzureSimpleNetworkResourceBuilder {

    @Inject
    private StackRepository stackRepository;
    @Inject
    private AzureStackUtil azureStackUtil;
    @Inject
    private AzureDeleteResourceStatusCheckerTask azureDeleteResourceStatusCheckerTask;
    @Inject
    private PollingService<AzureResourcePollerObject> azureResourcePollerObjectPollingService;

    @Override
    public Boolean delete(Resource resource, AzureDeleteContextObject deleteContextObject, String region) throws Exception {
        Stack stack = stackRepository.findById(deleteContextObject.getStackId());
        if (AzureStackUtil.GLOBAL_STORAGE != azureStackUtil.getNumOfStorageAccounts(stack)) {
            AzureClient azureClient = deleteContextObject.getAzureClient();
            String storageName = resource.getResourceName();
            String osImageName = azureStackUtil.getOsImageName(stack, storageName);
            if (azureClient.isImageAvailable(osImageName)) {
                HttpResponseDecorator imageResponse = (HttpResponseDecorator) azureClient.deleteOsImage(osImageName);
                AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(
                        azureClient, ResourceType.AZURE_STORAGE, storageName, stack, imageResponse);
                azureResourcePollerObjectPollingService.pollWithTimeout(azureDeleteResourceStatusCheckerTask, azureResourcePollerObject,
                        POLLING_INTERVAL, MAX_POLLING_ATTEMPTS, MAX_FAILURE_COUNT);
            }
            try {
                azureClient.deleteStorageAccount(storageName);
            } catch (Exception e) {
                if (e instanceof HttpResponseException) {
                    HttpResponseException httpResponseException = (HttpResponseException) e;
                    if (httpResponseException.getStatusCode() == NOT_FOUND) {
                        LOGGER.info("Storage Account: {} has already been deleted", storageName);
                    } else {
                        throw new AzureResourceException(httpResponseException.getResponse().toString());
                    }
                } else {
                    throw new AzureResourceException(e);
                }
            }
        }
        return true;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_STORAGE;
    }
}
