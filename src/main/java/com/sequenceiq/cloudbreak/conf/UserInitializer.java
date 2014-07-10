package com.sequenceiq.cloudbreak.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.InstanceType;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserStatus;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.service.blueprint.DefaultBlueprintLoaderService;

@Component
public class UserInitializer implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserInitializer.class);

    @Value("${hbm2ddl.strategy}")
    private String hbm2ddlStrategy;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DefaultBlueprintLoaderService defaultBlueprintLoaderService;

    @Override
    public void afterPropertiesSet() throws Exception {
        if ("create".equals(hbm2ddlStrategy) || "create-drop".equals(hbm2ddlStrategy)) {

            User user2 = new User();
            user2.setEmail("cbuser@sequenceiq.com");
            user2.setFirstName("seq");
            user2.setLastName("test");
            user2.setPassword(passwordEncoder.encode("test123"));
            user2.setStatus(UserStatus.ACTIVE);
            user2.setCompany("SequenceIQ");

            AwsCredential awsCredential = new AwsCredential();
            awsCredential.setRoleArn("arn:aws:iam::755047402263:role/seq-self-cf");
            awsCredential.setAwsCredentialOwner(user2);
            awsCredential.setName("aws_credential");

            user2.getAwsCredentials().add(awsCredential);

            AwsTemplate awsTemplate = new AwsTemplate();
            awsTemplate.setKeyName("sequence-eu");
            awsTemplate.setName("Aws development environment");
            awsTemplate.setDescription("description sample");
            awsTemplate.setRegion(Regions.EU_WEST_1);
            awsTemplate.setAmiId("ami-25dc0852");
            awsTemplate.setInstanceType(InstanceType.T2Small);
            awsTemplate.setSshLocation("0.0.0.0/0");
            awsTemplate.setUser(user2);

            user2.getAwsTemplates().add(awsTemplate);

            user2.setBlueprints(defaultBlueprintLoaderService.loadBlueprints(user2));

            userRepository.save(user2);
        }
    }
}
