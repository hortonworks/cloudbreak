package com.sequenceiq.cloudbreak.conf;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Account;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.domain.UserStatus;
import com.sequenceiq.cloudbreak.repository.AccountRepository;
import com.sequenceiq.cloudbreak.service.blueprint.DefaultBlueprintLoaderService;
import com.sequenceiq.cloudbreak.util.UserRolesUtil;

@Component
public class UserInitializer implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserInitializer.class);

    @Value("${cb.hbm2ddl.strategy:update}")
    private String hbm2ddlStrategy;

    @Value("${cb.default.user.email}")
    private String defaultUserEmail;

    @Value("${cb.default.user.firstname}")
    private String defaultUserFirstName;

    @Value("${cb.default.user.lastname}")
    private String defaultUserLastName;

    @Value("${cb.default.user.password}")
    private String defaultUserPassword;

    @Value("${cb.default.company.name}")
    private String defaultAccountName;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DefaultBlueprintLoaderService defaultBlueprintLoaderService;

    @Override
    public void afterPropertiesSet() throws Exception {
        if ("create".equals(hbm2ddlStrategy) || "create-drop".equals(hbm2ddlStrategy)) {
            LOGGER.info("Creating default account with name: {}", defaultAccountName);
            Account account = new Account();
            account.setName(defaultAccountName);

            LOGGER.info("Creating default user: [email: '{}', firstName: '{}', lastName:'{}']", defaultUserEmail, defaultUserFirstName, defaultUserLastName);
            User user = new User();
            user.setEmail(defaultUserEmail);
            user.setFirstName(defaultUserFirstName);
            user.setLastName(defaultUserLastName);
            user.setPassword(passwordEncoder.encode(defaultUserPassword));
            user.setStatus(UserStatus.ACTIVE);
            user.setAccount(account);
            user.getUserRoles().addAll(UserRolesUtil.getGroupForRole(UserRole.DEPLOYER));

            Set<Blueprint> blueprints = defaultBlueprintLoaderService.loadBlueprints(user);
            user.setBlueprints(blueprints);
            account.getUsers().add(user);

            accountRepository.save(account);
        }
    }
}
