package com.sequenceiq.provisioning.conf;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.domain.AwsCloudInstance;
import com.sequenceiq.provisioning.domain.AwsInfra;
import com.sequenceiq.provisioning.domain.AzureCloudInstance;
import com.sequenceiq.provisioning.domain.AzureInfra;
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
        awsInfra.setUser(user2);

        AwsCloudInstance awsCloudInstance = new AwsCloudInstance();
        awsCloudInstance.setAwsInfra(awsInfra);
        awsCloudInstance.setClusterSize(CLUSTER_SIZE);
        awsCloudInstance.setUser(user2);

        user2.getAwsInfraList().add(awsInfra);
        user2.getAwsCloudInstanceList().add(awsCloudInstance);

        AzureInfra azureInfra = new AzureInfra();
        azureInfra.setDeploymentSlot("slot");
        azureInfra.setDescription("azure desc");
        azureInfra.setDisableSshPasswordAuthentication(false);
        azureInfra.setImageName("image");
        azureInfra.setLocation("location");
        azureInfra.setName("azurename");
        azureInfra.setPassword("pass");
        azureInfra.setSubnetAddressPrefix("prefix");
        azureInfra.setVmType("small");
        azureInfra.setUser(user2);

        AzureCloudInstance azureCloudInstance = new AzureCloudInstance();
        azureCloudInstance.setAzureInfra(azureInfra);
        azureCloudInstance.setClusterSize(CLUSTER_SIZE);
        azureCloudInstance.setUser(user2);

        user2.getAzureInfraList().add(azureInfra);
        user2.getAzureCloudInstanceList().add(azureCloudInstance);

        userRepository.save(user2);

    }
}
