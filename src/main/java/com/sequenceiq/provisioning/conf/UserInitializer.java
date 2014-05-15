package com.sequenceiq.provisioning.conf;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.domain.User;
import com.sequenceiq.provisioning.repository.UserRepository;

@Component
public class UserInitializer implements InitializingBean {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void afterPropertiesSet() throws Exception {

        User user1 = new User();
        user1.setEmail("test@seq.com");
        user1.setFirstName("seq");
        user1.setLastName("test");
        user1.setPassword("password");

        userRepository.save(user1);

    }
}
