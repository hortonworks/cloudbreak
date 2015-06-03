package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.instance;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.connector.CloudConnectorException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.X509Certificate;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureCreateResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourcePollerObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;

import groovyx.net.http.HttpResponseDecorator;

@Component
@Order(2)
public class AzureServiceCertificateResourceBuilder extends AzureSimpleInstanceResourceBuilder {
    @Inject
    private StackRepository stackRepository;

    @Inject
    private AzureStackUtil azureStackUtil;

    @Inject
    private AzureCreateResourceStatusCheckerTask azureCreateResourceStatusCheckerTask;

    @Inject
    private PollingService<AzureResourcePollerObject> azureResourcePollerObjectPollingService;

    @Override
    public Boolean create(final CreateResourceRequest createResourceRequest, final String region) throws Exception {
        AzureServiceCertificateCreateRequest aCSCR = (AzureServiceCertificateCreateRequest) createResourceRequest;
        try {
            Map<String, String> props = aCSCR.getProps();
            AzureClient azureClient = aCSCR.getAzureClient();
            HttpResponseDecorator serviceCertificate = (HttpResponseDecorator) azureClient.createServiceCertificate(props);
            AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(
                    azureClient, ResourceType.AZURE_SERVICE_CERTIFICATE, props.get(NAME), aCSCR.getStack(), serviceCertificate);
            azureResourcePollerObjectPollingService.pollWithTimeout(azureCreateResourceStatusCheckerTask, azureResourcePollerObject,
                    POLLING_INTERVAL, MAX_POLLING_ATTEMPTS, MAX_FAILURE_COUNT);
        } catch (Exception ex) {
            throw checkException(ex);
        }
        return true;
    }

    @Override
    public List<Resource> buildResources(AzureProvisionContextObject provisionContextObject, int index, List<Resource> resources,
            Optional<InstanceGroup> instanceGroup) {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        String name = filterResourcesByType(resources, ResourceType.AZURE_CLOUD_SERVICE).get(0).getResourceName();
        return Arrays.asList(new Resource(resourceType(), name, stack, instanceGroup.orNull().getGroupName()));
    }

    @Override
    public CreateResourceRequest buildCreateRequest(AzureProvisionContextObject provisionContextObject, List<Resource> resources,
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup, Optional<String> userData) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        AzureCredential azureCredential = (AzureCredential) stack.getCredential();
        Map<String, String> props = new HashMap<>();
        props.put(NAME, buildResources.get(0).getResourceName());
        X509Certificate sshCert = null;
        try {
            sshCert = azureStackUtil.createX509Certificate(azureCredential);
            props.put(DATA, new String(sshCert.getPem()));
        } catch (Exception e) {
            throw new CloudConnectorException(e);
        }
        return new AzureServiceCertificateCreateRequest(props,
                azureStackUtil.createAzureClient(azureCredential), resources, buildResources, stack, instanceGroup.orNull());
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_SERVICE_CERTIFICATE;
    }

    @Override
    public Boolean delete(Resource resource, AzureDeleteContextObject azureDeleteContextObject, String region) throws Exception {
        return true;
    }

    public class AzureServiceCertificateCreateRequest extends CreateResourceRequest {
        private Map<String, String> props = new HashMap<>();
        private AzureClient azureClient;
        private List<Resource> resources;
        private Stack stack;
        private InstanceGroup instanceGroup;

        public AzureServiceCertificateCreateRequest(Map<String, String> props, AzureClient azureClient, List<Resource> resources, List<Resource> buildNames,
                Stack stack, InstanceGroup instanceGroup) {
            super(buildNames);
            this.props = props;
            this.azureClient = azureClient;
            this.resources = resources;
            this.stack = stack;
            this.instanceGroup = instanceGroup;
        }

        public Stack getStack() {
            return stack;
        }

        public InstanceGroup getInstanceGroup() {
            return instanceGroup;
        }

        public Map<String, String> getProps() {
            return props;
        }

        public AzureClient getAzureClient() {
            return azureStackUtil.createAzureClient((AzureCredential) stack.getCredential());
        }

        public List<Resource> getResources() {
            return resources;
        }
    }

}
