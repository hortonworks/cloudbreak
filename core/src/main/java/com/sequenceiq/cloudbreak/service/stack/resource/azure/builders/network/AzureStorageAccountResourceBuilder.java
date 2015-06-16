package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.network;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudRegion;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureResourceException;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureCreateResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureDeleteResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourcePollerObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleNetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@Component
@Order(2)
public class AzureStorageAccountResourceBuilder extends AzureSimpleNetworkResourceBuilder {

    @Inject
    private StackRepository stackRepository;
    @Inject
    private AzureStackUtil azureStackUtil;
    @Inject
    private AzureCreateResourceStatusCheckerTask azureCreateResourceStatusCheckerTask;
    @Inject
    private AzureDeleteResourceStatusCheckerTask azureDeleteResourceStatusCheckerTask;
    @Inject
    private PollingService<AzureResourcePollerObject> azureResourcePollerObjectPollingService;

    @Override
    public Boolean create(CreateResourceRequest createResourceRequest, String region) throws Exception {
        AzureStorageAccountCreateRequest aCSCR = (AzureStorageAccountCreateRequest) createResourceRequest;
        Stack stack = stackRepository.findById(aCSCR.stackId);
        AzureClient azureClient = aCSCR.getAzureClient();
        for (Map<String, String> properties : aCSCR.getProps()) {
            String storageName = properties.get(NAME);
            try {
                azureClient.getStorageAccount(storageName);
            } catch (Exception ex) {
                if (ex instanceof HttpResponseException) {
                    HttpResponseException httpResponseException = (HttpResponseException) ex;
                    if (httpResponseException.getStatusCode() == NOT_FOUND) {
                        HttpResponseDecorator storageResponse = (HttpResponseDecorator) azureClient.createStorageAccount(properties);
                        AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(
                                azureClient, ResourceType.AZURE_STORAGE, storageName, stack, storageResponse);
                        azureResourcePollerObjectPollingService.pollWithTimeout(azureCreateResourceStatusCheckerTask, azureResourcePollerObject,
                                POLLING_INTERVAL, MAX_POLLING_ATTEMPTS, MAX_FAILURE_COUNT);
                    } else {
                        LOGGER.error("Error creating storage: {}", storageName, httpResponseException);
                        throw new AzureResourceException(httpResponseException.getResponse().toString());
                    }
                } else {
                    LOGGER.error("Error creating storage: {} for stack: {}", storageName, aCSCR.getStackId(), ex);
                    throw new AzureResourceException(ex);
                }
            }
        }
        return true;
    }

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
    public List<Resource> buildResources(AzureProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        CloudRegion location = CloudRegion.valueOf(stack.getRegion());
        List<Resource> accounts = new ArrayList<>();
        int storageAccountNum = azureStackUtil.getNumOfStorageAccounts(stack);
        if (storageAccountNum == AzureStackUtil.GLOBAL_STORAGE) {
            accounts.add(new Resource(resourceType(), azureStackUtil.getOSStorageName(stack, location, storageAccountNum), stack, null));
        } else {
            for (int i = 0; i < storageAccountNum; i++) {
                accounts.add(new Resource(resourceType(), azureStackUtil.getOSStorageName(stack, location, i), stack, null));
            }
        }
        return accounts;
    }

    @Override
    public CreateResourceRequest buildCreateRequest(AzureProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureClient azureClient = azureStackUtil.createAzureClient(credential);
        List<Map<String, String>> propertyList = new ArrayList<>(buildResources.size());
        for (Resource resource : buildResources) {
            Map<String, String> props = new HashMap<>();
            props.put(NAME, resource.getResourceName());
            props.put(DESCRIPTION, "description");
            props.put(AFFINITYGROUP, provisionContextObject.getAffinityGroupName());
            propertyList.add(props);
        }
        return new AzureStorageAccountCreateRequest(provisionContextObject.getStackId(), propertyList, azureClient, resources, buildResources);
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_STORAGE;
    }

    public class AzureStorageAccountCreateRequest extends CreateResourceRequest {
        private List<Map<String, String>> props = new ArrayList<>();
        private AzureClient azureClient;
        private Long stackId;
        private List<Resource> resources;

        public AzureStorageAccountCreateRequest(Long stackId, List<Map<String, String>> props, AzureClient azureClient, List<Resource> resources,
                List<Resource> buildNames) {
            super(buildNames);
            this.stackId = stackId;
            this.props = props;
            this.azureClient = azureClient;
            this.resources = resources;
        }

        public Long getStackId() {
            return stackId;
        }

        public List<Map<String, String>> getProps() {
            return props;
        }

        public void setProps(List<Map<String, String>> props) {
            this.props = props;
        }

        public AzureClient getAzureClient() {
            return azureClient;
        }

        public List<Resource> getResources() {
            return resources;
        }
    }

}
