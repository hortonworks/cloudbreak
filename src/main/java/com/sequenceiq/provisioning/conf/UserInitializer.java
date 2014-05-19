package com.sequenceiq.provisioning.conf;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.domain.AwsInfra;
import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.UserRepository;

@Component
public class UserInitializer implements InitializingBean {

    private static final Integer CLUSTER_SIZE = 3;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void afterPropertiesSet() throws Exception {

        User user1 = new User();
        user1.setEmail("test@seq.com");
        user1.setFirstName("seq");
        user1.setLastName("test");
        user1.setPassword("password");

        User user2 = new User();
        user2.setEmail("user@seq.com");
        user2.setFirstName("seq");
        user2.setLastName("test");
        user2.setPassword("test123");
        user2.setSubscriptionId("");
        user2.setJks("test123");
        AwsInfra awsInfra = new AwsInfra();
        awsInfra.setName("userAzureStack");
        awsInfra.setKeyName("smaple_key");
        awsInfra.setName("template1");
        awsInfra.setRegion("region-1");

        user2.getAwsInfraList().add(awsInfra);

        userRepository.save(user1);
        userRepository.save(user2);

    }
}
