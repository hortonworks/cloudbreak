package com.sequenceiq.cloudbreak.conf;

import java.util.Set;

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
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Company;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.domain.UserStatus;
import com.sequenceiq.cloudbreak.repository.CompanyRepository;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.service.blueprint.DefaultBlueprintLoaderService;

import reactor.core.Reactor;

@Component
public class UserInitializer implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserInitializer.class);

    @Value("${cb.hbm2ddl.strategy:update}")
    private String hbm2ddlStrategy;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;


    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DefaultBlueprintLoaderService defaultBlueprintLoaderService;

    @Autowired
    private Reactor reactor;


    @Override
    public void afterPropertiesSet() throws Exception {
        if ("create".equals(hbm2ddlStrategy) || "create-drop".equals(hbm2ddlStrategy)) {

            Company company = new Company();
            company.setName("SequenceIQ");
            company = companyRepository.save(company);


            User user2 = new User();
            user2.setEmail("cbuser@sequenceiq.com");
            user2.setFirstName("seq");
            user2.setLastName("test");
            user2.setPassword(passwordEncoder.encode("test123"));
            user2.setStatus(UserStatus.ACTIVE);
            user2.setCompany(company);
            user2.getUserRole().add(UserRole.COMPANY_ADMIN);

            AwsCredential awsCredential = new AwsCredential();
            awsCredential.setRoleArn("arn:aws:iam::755047402263:role/seq-self-cf");
            awsCredential.setAwsCredentialOwner(user2);
            awsCredential.setPublicKey("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF"
                    + "3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuTQxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgp"
                    + "UWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/VUYQ/yi0imocARlOiFdPRlZGTN0"
                    + "XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaKpMxSG76XWhuz"
                    + "FpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5r"
                    + "sYKR495VFmuOepLYz5I8Dn sequence-eu");
            awsCredential.setName("aws_credential");
            awsCredential.setKeyPairName("cloudbreak-key");

            user2.getAwsCredentials().add(awsCredential);

            AwsTemplate awsTemplate = new AwsTemplate();
            awsTemplate.setName("Aws development environment");
            awsTemplate.setDescription("description sample");
            awsTemplate.setRegion(Regions.EU_WEST_1);
            awsTemplate.setAmiId("ami-7778af00");
            awsTemplate.setInstanceType(InstanceType.T2Small);
            awsTemplate.setSshLocation("0.0.0.0/0");
            awsTemplate.setUser(user2);

            user2.getAwsTemplates().add(awsTemplate);

            Set<Blueprint> blueprints = defaultBlueprintLoaderService.loadBlueprints(user2);
            user2.setBlueprints(blueprints);

            userRepository.save(user2);

        }
    }
}
