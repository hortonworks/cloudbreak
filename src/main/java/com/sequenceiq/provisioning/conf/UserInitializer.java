package com.sequenceiq.provisioning.conf;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.InstanceType;
import com.sequenceiq.provisioning.domain.AwsTemplate;
import com.sequenceiq.provisioning.domain.AzureTemplate;
import com.sequenceiq.provisioning.domain.Stack;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.UserRepository;

@Component
public class UserInitializer implements InitializingBean {

    private static final Integer CLUSTER_SIZE = 3;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void afterPropertiesSet() throws Exception {

        User user2 = new User();
        user2.setEmail("user@seq.com");
        user2.setFirstName("seq");
        user2.setLastName("test");
        user2.setPassword("test123");
        user2.setSubscriptionId("1234-45567-123213-12312");
        user2.setJks("test123");
        user2.setRoleArn("arnrole");

        AwsTemplate awsTemplate = new AwsTemplate();
        awsTemplate.setName("userAzureStack");
        awsTemplate.setKeyName("smaple_key");
        awsTemplate.setName("template1");
        awsTemplate.setRegion("eu-west-1");
        awsTemplate.setAmiId("ami-2918e35e");
        awsTemplate.setInstanceType(InstanceType.M1Small);
        awsTemplate.setSshLocation("0.0.0.0/0");
        awsTemplate.setUser(user2);

        Stack awsStack = new Stack();
        awsStack.setTemplate(awsTemplate);
        awsStack.setClusterSize(CLUSTER_SIZE);
        awsStack.setUser(user2);

        user2.getAwsTemplates().add(awsTemplate);
        user2.getStacks().add(awsStack);

        AzureTemplate azureTemplate = new AzureTemplate();
        azureTemplate.setDeploymentSlot("slot");
        azureTemplate.setDescription("azure desc");
        azureTemplate.setDisableSshPasswordAuthentication(false);
        azureTemplate.setImageName("image");
        azureTemplate.setLocation("location");
        azureTemplate.setName("azurename");
        azureTemplate.setUserName("username");
        azureTemplate.setPassword("pass");
        azureTemplate.setSubnetAddressPrefix("prefix");
        azureTemplate.setVmType("small");
        azureTemplate.setUser(user2);

        Stack azureStack = new Stack();
        azureStack.setTemplate(azureTemplate);
        azureStack.setClusterSize(CLUSTER_SIZE);
        azureStack.setUser(user2);

        user2.getAzureTemplates().add(azureTemplate);
        user2.getStacks().add(azureStack);

        userRepository.save(user2);

    }
}
