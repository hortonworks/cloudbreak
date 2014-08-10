package com.sequenceiq.cloudbreak.conf;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Company;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.domain.UserStatus;
import com.sequenceiq.cloudbreak.repository.CompanyRepository;
import com.sequenceiq.cloudbreak.service.blueprint.DefaultBlueprintLoaderService;

@Component
public class UserInitializer implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserInitializer.class);

    @Value("${cb.hbm2ddl.strategy:update}")
    private String hbm2ddlStrategy;

    @Value("${cb.default.user.email:cbuser@sequenceiq.com}")
    private String defaultUserEmail;

    @Value("${cb.default.user.firstname:Firstname}")
    private String defaultUserFirstName;

    @Value("${cb.default.user.lastname:Lastname}")
    private String defaultUserLastName;

    @Value("${cb.default.user.password:test123}")
    private String defaultUserPassword;

    @Value("${cb.default.company.name:SequenceIQ}")
    private String defaultCompanyName;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DefaultBlueprintLoaderService defaultBlueprintLoaderService;

    @Override
    public void afterPropertiesSet() throws Exception {
        if ("create".equals(hbm2ddlStrategy) || "create-drop".equals(hbm2ddlStrategy)) {
            Company company = new Company();
            company.setName(defaultCompanyName);

            User user = new User();
            user.setEmail(defaultUserEmail);
            user.setFirstName(defaultUserFirstName);
            user.setLastName(defaultUserLastName);
            user.setPassword(passwordEncoder.encode(defaultUserPassword));
            user.setStatus(UserStatus.ACTIVE);
            user.setCompany(company);
            user.getUserRoles().add(UserRole.COMPANY_ADMIN);

            Set<Blueprint> blueprints = defaultBlueprintLoaderService.loadBlueprints(user);
            user.setBlueprints(blueprints);
            company.getUsers().add(user);

            companyRepository.save(company);
        }
    }
}
