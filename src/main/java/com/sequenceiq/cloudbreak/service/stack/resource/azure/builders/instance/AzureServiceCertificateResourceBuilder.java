package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.instance;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;

import java.io.FileNotFoundException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;
import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.StackCreationFailureException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.X509Certificate;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourcePollerObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;

import groovyx.net.http.HttpResponseDecorator;

@Component
@Order(2)
public class AzureServiceCertificateResourceBuilder extends AzureSimpleInstanceResourceBuilder {
    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private AzureStackUtil azureStackUtil;

    @Autowired
    private AzureResourceStatusCheckerTask azureResourceStatusCheckerTask;

    @Autowired
    private PollingService<AzureResourcePollerObject> azureResourcePollerObjectPollingService;

    @Override
    public Boolean create(final CreateResourceRequest createResourceRequest, final String region) throws Exception {
        AzureServiceCertificateCreateRequest aCSCR = (AzureServiceCertificateCreateRequest) createResourceRequest;
        HttpResponseDecorator serviceCertificate = (HttpResponseDecorator) aCSCR.getAzureClient().createServiceCertificate(aCSCR.getProps());
        AzureResourcePollerObject azureResourcePollerObject = new AzureResourcePollerObject(aCSCR.getAzureClient(), serviceCertificate, aCSCR.getStack());
        azureResourcePollerObjectPollingService.pollWithTimeout(azureResourceStatusCheckerTask, azureResourcePollerObject,
                POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
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
            List<Resource> buildResources, int index, Optional<InstanceGroup> instanceGroup) throws Exception {
        Stack stack = stackRepository.findById(provisionContextObject.getStackId());
        AzureCredential azureCredential = (AzureCredential) stack.getCredential();
        Map<String, String> props = new HashMap<>();
        props.put(NAME, buildResources.get(0).getResourceName());
        X509Certificate sshCert = null;
        try {
            sshCert = azureStackUtil.createX509Certificate(azureCredential);
        } catch (FileNotFoundException e) {
            throw new StackCreationFailureException(e);
        } catch (CertificateException e) {
            throw new StackCreationFailureException(e);
        }
        try {
            props.put(DATA, new String(sshCert.getPem()));
        } catch (CertificateEncodingException e) {
            throw new StackCreationFailureException(e);
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
            return azureClient;
        }

        public List<Resource> getResources() {
            return resources;
        }
    }

}
