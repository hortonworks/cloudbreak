package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureLocation;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.AzureVmType;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.service.stack.connector.ConnectorTestUtil;

public class AzureConnectorTestUtil extends ConnectorTestUtil {

    public static final String DUMMY_NAME = "dummyName";
    public static final String DUMMY_PASSWORD = "dummyPassword";
    public static final String IMAGE_NAME = "ambari-docker-v1";

    private AzureConnectorTestUtil() {
    }

    public static Stack createStack(User user, Credential credential, AzureTemplate azureTemplate, Set<Resource> resources) {
        Stack stack = new Stack();
        stack.setId(DEFAULT_ID);
        stack.setName(STACK_NAME);
        stack.setAmbariIp(AMBARI_IP);
        stack.setCredential(credential);
        stack.setUser(user);
        stack.setTemplate(azureTemplate);
        stack.setNodeCount(NODE_COUNT);
        stack.setStatus(Status.REQUESTED);
        stack.setResources(resources);
        return stack;
    }

    public static AzureCredential createAzureCredential() {
        AzureCredential credential = new AzureCredential();
        credential.setId(DEFAULT_ID);
        credential.setCloudPlatform(CloudPlatform.AZURE);
        credential.setDescription(AZURE_DESCRIPTION);
        credential.setName(DUMMY_NAME);
        return credential;
    }

    public static AzureTemplate createAzureTemplate(User user) {
        AzureTemplate azureTemplate = new AzureTemplate();
        azureTemplate.setId(DEFAULT_ID);
        azureTemplate.setOwner(user);
        azureTemplate.setLocation(AzureLocation.NORTH_EUROPE);
        azureTemplate.setVmType(AzureVmType.MEDIUM.toString());
        azureTemplate.setDescription(AZURE_DESCRIPTION);
        azureTemplate.setImageName(IMAGE_NAME);
        azureTemplate.setVolumeCount(0);
        azureTemplate.setVolumeSize(0);
        return azureTemplate;
    }
}
