package com.sequenceiq.freeipa.service.binduser;

import static com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants.ADMINS_GROUP;

import java.util.List;
import java.util.Optional;

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
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.client.operation.GroupAddMemberOperation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.ldap.LdapConfig;
import com.sequenceiq.freeipa.ldap.LdapConfigConverter;
import com.sequenceiq.freeipa.ldap.LdapConfigService;
import com.sequenceiq.freeipa.ldap.v1.LdapConfigV1Service;

@Service
public class UserSyncBindUserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserSyncBindUserService.class);

    private static final String USERSYNC_USER_POSTFIX = "usersync";

    @Inject
    private LdapConfigV1Service ldapConfigV1Service;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private LdapBindUserNameProvider userNameProvider;

    @Inject
    private LdapConfigConverter ldapConfigConverter;

    public void createUserAndLdapConfig(Stack stack, FreeIpaClient client) throws FreeIpaClientException {
        cleanupAlreadyExistingLdapConfig(stack.getEnvironmentCrn(), stack.getAccountId());
        ldapConfigV1Service.createNewLdapConfig(stack.getEnvironmentCrn(), createUserSyncBindUserPostfix(stack.getEnvironmentCrn()), stack, true);
        addBindUserToAdminGroup(stack, client);
    }

    public boolean doesBindUserAndConfigAlreadyExist(Stack stack, FreeIpaClient client) throws FreeIpaClientException {
        Optional<LdapConfig> ldapConfig =
                ldapConfigService.find(stack.getEnvironmentCrn(), stack.getAccountId(), createUserSyncBindUserPostfix(stack.getEnvironmentCrn()));
        if (ldapConfig.isPresent()) {
            LOGGER.info("LdapConfig for usersync bind user exists");
            String bindUserName = getUserSyncBindUserName(stack.getEnvironmentCrn());
            Optional<User> user = client.userFind(bindUserName);
            LOGGER.info("Bind user in FreeIPA {}", user.isPresent() ? "exists" : "doesn't exist");
            return user.isPresent();
        } else {
            LOGGER.info("LdapConfig for usersync bind user doesn't exist");
            return false;
        }
    }

    public String getUserSyncBindUserName(String environmentCrn) {
        return userNameProvider.createBindUserName(createUserSyncBindUserPostfix(environmentCrn));
    }

    public String createUserSyncBindUserPostfix(String environmentCrn) {
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

    public void addBindUserToAdminGroup(Stack stack, FreeIpaClient client) throws FreeIpaClientException {
        String bindUserName = getUserSyncBindUserName(stack.getEnvironmentCrn());
        LOGGER.info("Add [{}] user to [{}]", bindUserName, ADMINS_GROUP);
        FreeIpaClientExceptionUtil.ignoreEmptyModExceptionWithValue(
                () -> GroupAddMemberOperation.create(ADMINS_GROUP, List.of(bindUserName), null).invoke(client),
                "[{}] already added to [{}}", bindUserName, ADMINS_GROUP);
    }

    private void cleanupAlreadyExistingLdapConfig(String environmentCrn, String accountId) {
        try {
            ldapConfigService.delete(environmentCrn, accountId, createUserSyncBindUserPostfix(environmentCrn));
        } catch (NotFoundException e) {
            LOGGER.debug("Ignoring not found LdapConfig for usersync bind user");
        }
    }
}
