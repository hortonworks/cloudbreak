package com.sequenceiq.cloudbreak.service.stack.resource.azure.builders;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureDescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.model.AzureStartStopContextOBject;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

@Component
public class AzureResourceBuilderInit implements
        ResourceBuilderInit<AzureProvisionContextObject, AzureDeleteContextObject, AzureDescribeContextObject, AzureStartStopContextOBject> {

    private static final String IMAGE_NAME = "ambari-docker-v1";

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    public AzureProvisionContextObject provisionInit(Stack stack, String userData) throws Exception {
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureClient azureClient = createAzureClient(credential, AzureCertificateService.getUserJksFileName(credential, emailAsFolder(stack.getOwner())));
        AzureProvisionContextObject azureProvisionContextObject =
                new AzureProvisionContextObject(stack.getId(), credential.getCommonName(), azureClient, emailAsFolder(stack.getOwner()),
                        getOsImageName(credential), userData);
        return azureProvisionContextObject;
    }

    @Override
    public AzureDeleteContextObject deleteInit(Stack stack) throws Exception {
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureClient azureClient = createAzureClient(credential, AzureCertificateService.getUserJksFileName(credential, emailAsFolder(stack.getOwner())));
        AzureDeleteContextObject azureDeleteContextObject =
                new AzureDeleteContextObject(stack.getId(), credential.getCommonName(), azureClient, emailAsFolder(stack.getOwner()));
        return azureDeleteContextObject;
    }

    @Override
    public AzureStartStopContextOBject startStopInit(Stack stack) throws Exception {
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureClient azureClient = createAzureClient(credential, AzureCertificateService.getUserJksFileName(credential, emailAsFolder(stack.getOwner())));
        return new AzureStartStopContextOBject(stack.getId(), azureClient, emailAsFolder(stack.getOwner()));
    }

    @Override
    public AzureDescribeContextObject describeInit(Stack stack) throws Exception {
        AzureCredential credential = (AzureCredential) stack.getCredential();
        AzureClient azureClient = createAzureClient(credential, AzureCertificateService.getUserJksFileName(credential, emailAsFolder(stack.getOwner())));
        AzureDescribeContextObject azureDescribeContextObject =
                new AzureDescribeContextObject(stack.getId(), credential.getCommonName(), azureClient, emailAsFolder(stack.getOwner()));
        return azureDescribeContextObject;
    }

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.RESOURCE_BUILDER_INIT;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AZURE;
    }


    protected AzureClient createAzureClient(AzureCredential credential, String filePath) {
        File file = new File(filePath);
        return new AzureClient(credential.getSubscriptionId(), file.getAbsolutePath(), credential.getJks());
    }

    protected String emailAsFolder(String userId) {
        String email = userDetailsService.getDetails(userId, UserFilterField.USERID).getUsername();
        return email.replaceAll("@", "_").replace(".", "_");
    }

    public String getOsImageName(Credential credential) {
        return String.format("%s-%s", ((AzureCredential) credential).getCommonName(), IMAGE_NAME);
    }

}
