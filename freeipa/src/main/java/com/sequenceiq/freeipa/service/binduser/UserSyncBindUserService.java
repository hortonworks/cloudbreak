package com.sequenceiq.freeipa.service.binduser;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.model.Role;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.ldap.LdapConfig;
import com.sequenceiq.freeipa.ldap.LdapConfigConverter;
import com.sequenceiq.freeipa.ldap.LdapConfigService;
import com.sequenceiq.freeipa.ldap.v1.LdapConfigV1Service;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@Service
public class UserSyncBindUserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncBindUserService.class);

    private static final String USERSYNC_USER_POSTFIX = "usersync";

    private static final String USER_ADMINISTRATOR_ROLE = "User Administrator";

    @Inject
    private LdapConfigV1Service ldapConfigV1Service;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private LdapBindUserNameProvider userNameProvider;

    @Inject
    private FreeIpaClientFactory ipaClientFactory;

    @Inject
    private LdapConfigConverter ldapConfigConverter;

    public void createUserAndLdapConfig(Stack stack) throws FreeIpaClientException {
        cleanupAlreadyExistingLdapConfig(stack.getEnvironmentCrn(), stack.getAccountId());
        ldapConfigV1Service.createNewLdapConfig(stack.getEnvironmentCrn(), createUserSyncBindUserPostfix(stack.getEnvironmentCrn()), stack, true);
        addUserAdminRoleForBindUser(stack);
    }

    public boolean doesBindUserAndConfigAlreadyExist(Stack stack) throws FreeIpaClientException {
        Optional<LdapConfig> ldapConfig =
                ldapConfigService.find(stack.getEnvironmentCrn(), stack.getAccountId(), createUserSyncBindUserPostfix(stack.getEnvironmentCrn()));
        if (ldapConfig.isPresent()) {
            LOGGER.info("LdapConfig for usersync bind user exists");
            FreeIpaClient client = ipaClientFactory.getFreeIpaClientForStack(stack);
            String bindUserName = getUserSyncBindUserName(stack.getEnvironmentCrn());
            Optional<User> user = client.userFind(bindUserName);
            LOGGER.info("Bind user in FreeIPA {}", user.isPresent() ? "exists" : "doesn't exist");
            Optional<Role> role = FreeIpaClientExceptionUtil.ignoreNotFoundExceptionWithValue(
                    () -> client.showRole(USER_ADMINISTRATOR_ROLE), "[{}] role is missing", USER_ADMINISTRATOR_ROLE);
            return user.isPresent() && role.isPresent() && role.get().getMemberUser().contains(bindUserName);
        } else {
            LOGGER.info("LdapConfig for usersync bind user doesn't exist");
            return false;
        }
    }

    private String getUserSyncBindUserName(String environmentCrn) {
        return userNameProvider.createBindUserName(createUserSyncBindUserPostfix(environmentCrn));
    }

    private String createUserSyncBindUserPostfix(String environmentCrn) {
        String[] splittedEnvCrn = StringUtils.split(environmentCrn, "-");
        String envCrnPostfix = splittedEnvCrn[splittedEnvCrn.length - 1];
        return USERSYNC_USER_POSTFIX + "-" + envCrnPostfix;
    }

    public DescribeLdapConfigResponse getUserSyncLdapConfigIfExistsOrThrowNotFound(String environmentCrn, String accountId) {
        Optional<LdapConfig> ldapConfig =
                ldapConfigService.find(environmentCrn, accountId, createUserSyncBindUserPostfix(environmentCrn));
        if (ldapConfig.isPresent()) {
            return ldapConfigConverter.convertLdapConfigToDescribeLdapConfigResponse(ldapConfig.get());
        } else {
            throw new NotFoundException("User sync LDAP config not found");
        }
    }

    private void addUserAdminRoleForBindUser(Stack stack) throws FreeIpaClientException {
        String bindUserName = getUserSyncBindUserName(stack.getEnvironmentCrn());
        LOGGER.info("Add [{}] role for [{}]", USER_ADMINISTRATOR_ROLE, bindUserName);
        FreeIpaClient client = ipaClientFactory.getFreeIpaClientForStack(stack);
        FreeIpaClientExceptionUtil.ignoreEmptyModExceptionWithValue(
                () -> client.addRoleMember(USER_ADMINISTRATOR_ROLE, Set.of(bindUserName), Set.of(), Set.of(), Set.of(), Set.of()), null);
    }

    private void cleanupAlreadyExistingLdapConfig(String environmentCrn, String accountId) {
        try {
            ldapConfigService.delete(environmentCrn, accountId, createUserSyncBindUserPostfix(environmentCrn));
        } catch (NotFoundException e) {
            LOGGER.debug("Ignoring not found LdapConfig for usersync bind user");
        }
    }
}
