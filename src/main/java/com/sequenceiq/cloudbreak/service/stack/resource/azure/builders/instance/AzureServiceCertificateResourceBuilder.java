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
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.X509Certificate;
import com.sequenceiq.cloudbreak.service.stack.flow.AzureResourcePollerObject;
import com.sequenceiq.cloudbreak.service.stack.flow.AzureResourceStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.AzureSimpleInstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDescribeContextObject;
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
    private PollingService<AzureResourcePollerObject> pollingService;


    @Override
    public List<Resource> create(AzureProvisionContextObject po, int index, List<Resource> resources) throws Exception {
        Stack stack = stackRepository.findById(po.getStackId());
        AzureCredential azureCredential = (AzureCredential) stack.getCredential();
        Map<String, String> props = new HashMap<>();
        String name = filterResourcesByType(resources, ResourceType.AZURE_CLOUD_SERVICE).get(0).getResourceName();
        props.put(NAME, name);
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
        AzureClient azureClient = po.getNewAzureClient(azureCredential);
        HttpResponseDecorator serviceCertificate = (HttpResponseDecorator) azureClient.createServiceCertificate(props);
        AzureResourcePollerObject pollerObject = new AzureResourcePollerObject(azureClient, serviceCertificate, stack);
        pollingService.pollWithTimeout(azureResourceStatusCheckerTask, pollerObject, POLLING_INTERVAL, MAX_POLLING_ATTEMPTS);
        return Arrays.asList(new Resource(resourceType(), name, stack));
    }

    @Override
    public Boolean delete(Resource resource, AzureDeleteContextObject azureDeleteContextObject) throws Exception {
        return true;
    }

    @Override
    public Optional<String> describe(Resource resource, AzureDescribeContextObject azureDescribeContextObject) throws Exception {
        return Optional.absent();
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AZURE_SERVICE_CERTIFICATE;
    }
}
