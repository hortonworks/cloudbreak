package com.sequenceiq.provisioning.conf;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.InstanceType;
import com.sequenceiq.provisioning.domain.AwsInfra;
import com.sequenceiq.provisioning.domain.AzureInfra;
import com.sequenceiq.provisioning.domain.CloudInstance;
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

        AwsInfra awsInfra = new AwsInfra();
        awsInfra.setName("userAzureStack");
        awsInfra.setKeyName("smaple_key");
        awsInfra.setName("template1");
        awsInfra.setRegion("region-1");
        awsInfra.setAmiId("ami-2918e35e");
        awsInfra.setInstanceType(InstanceType.M1Small);
        awsInfra.setSshLocation("0.0.0.0/0");
        awsInfra.setUser(user2);

        CloudInstance awsCloudInstance = new CloudInstance();
        awsCloudInstance.setInfra(awsInfra);
        awsCloudInstance.setClusterSize(CLUSTER_SIZE);
        awsCloudInstance.setUser(user2);

        user2.getAwsInfras().add(awsInfra);
        user2.getCloudInstances().add(awsCloudInstance);

        AzureInfra azureInfra = new AzureInfra();
        azureInfra.setDeploymentSlot("slot");
        azureInfra.setDescription("azure desc");
        azureInfra.setDisableSshPasswordAuthentication(false);
        azureInfra.setImageName("image");
        azureInfra.setLocation("location");
        azureInfra.setName("azurename");
        azureInfra.setUserName("username");
        azureInfra.setPassword("pass");
        azureInfra.setSubnetAddressPrefix("prefix");
        azureInfra.setVmType("small");
        azureInfra.setUser(user2);

        CloudInstance azureCloudInstance = new CloudInstance();
        azureCloudInstance.setInfra(azureInfra);
        azureCloudInstance.setClusterSize(CLUSTER_SIZE);
        azureCloudInstance.setUser(user2);

        user2.getAzureInfras().add(azureInfra);
        user2.getCloudInstances().add(azureCloudInstance);

        userRepository.save(user2);

    }
}
