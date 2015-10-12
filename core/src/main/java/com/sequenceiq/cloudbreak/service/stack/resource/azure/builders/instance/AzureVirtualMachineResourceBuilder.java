package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.instance;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.SERVICENAME;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureResourceException;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureDeleteResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourcePollerObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@Component
@Order(3)
// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public class AzureVirtualMachineResourceBuilder extends AzureSimpleInstanceResourceBuilder {

    @Inject
    private StackRepository stackRepository;

    @Inject
    private AzureStackUtil azureStackUtil;

    @Inject
    private AzureDeleteResourceStatusCheckerTask azureDeleteResourceStatusCheckerTask;

    @Inject
    private PollingService<AzureResourcePollerObject> azureResourcePollerObjectPollingService;

    @Override
    public Boolean delete(Resource resource, AzureDeleteContextObject aDCO, String region) throws Exception {
        Stack stack = stackRepository.findByIdLazy(aDCO.getStackId());
        AzureCredential credential = (AzureCredential) stack.getCredential();
        try {
            Map<String, String> props = new HashMap<>();
            props.put(SERVICENAME, resource.getResourceName());
            props.put(NAME, resource.getResourceName());
            AzureClient azureClient = azureStackUtil.createAzureClient(credential);
            HttpResponseDecorator deleteVirtualMachineResult = (HttpResponseDecorator) azureClient.deleteVirtualMachine(props);
            AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(
                    azureClient, ResourceType.AZURE_VIRTUAL_MACHINE, props.get(NAME), stack, deleteVirtualMachineResult);
            azureResourcePollerObjectPollingService.pollWithTimeout(azureDeleteResourceStatusCheckerTask, azureResourcePollerObject,
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS, MAX_FAILURE_COUNT);
        } catch (HttpResponseException ex) {
            httpResponseExceptionHandler(ex, resource.getResourceName(), stack.getOwner());
        } catch (Exception ex) {
            throw new AzureResourceException(ex);
        }
        return true;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_VIRTUAL_MACHINE;
    }
}
