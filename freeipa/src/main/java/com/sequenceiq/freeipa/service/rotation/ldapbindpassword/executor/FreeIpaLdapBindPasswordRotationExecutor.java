package com.sequenceiq.freeipa.service.rotation.ldapbindpassword.executor;

import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_LDAP_BIND_PASSWORD;

import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.ldap.v1.LdapConfigV1Service;
import com.sequenceiq.freeipa.service.binduser.LdapBindUserNameProvider;
import com.sequenceiq.freeipa.service.binduser.UserSyncBindUserService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.rotation.ldapbindpassword.context.FreeIpaLdapBindPasswordRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaLdapBindPasswordRotationExecutor extends AbstractRotationExecutor<FreeIpaLdapBindPasswordRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaLdapBindPasswordRotationExecutor.class);

    @Inject
    private StackService stackService;

    @Inject
    private SecretService secretService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private LdapConfigV1Service ldapConfigService;

    @Inject
    private LdapBindUserNameProvider userNameProvider;

    @Inject
    private UserSyncBindUserService userSyncBindUserService;

    @Override
    protected void rotate(FreeIpaLdapBindPasswordRotationContext rotationContext) throws Exception {
        changePassword(rotationContext, rotationVaultSecret -> rotationVaultSecret.getSecret());
    }

    @Override
    protected void rollback(FreeIpaLdapBindPasswordRotationContext rotationContext) throws Exception {
        changePassword(rotationContext, rotationVaultSecret -> rotationVaultSecret.getBackupSecret());
    }

    private void changePassword(FreeIpaLdapBindPasswordRotationContext rotationContext, Function<RotationSecret, String> passwordSelector) {
        String environmentCrnAsString = rotationContext.getResourceCrn();
        Crn environmentCrn = Crn.safeFromString(environmentCrnAsString);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrnAsString, environmentCrn.getAccountId());
        RotationSecret bindPasswordRotationSecret = secretService.getRotation(rotationContext.getBindPasswordSecret());
        if (bindPasswordRotationSecret.isRotation()) {
            try {
                String newPassword = passwordSelector.apply(bindPasswordRotationSecret);
                String bindUserName = getBindUserName(rotationContext)
                        .orElseThrow(() -> new SecretRotationException(String.format("Cannot determine ldap bind user name " +
                                "from context: %s", rotationContext)));
                FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
                User user = freeIpaClient.userShow(bindUserName);
                ldapConfigService.setBindUserPassword(freeIpaClient, user, newPassword);
            } catch (FreeIpaClientException e) {
                String message = "Freeipa client can not be created for LDAP bind password update.";
                LOGGER.info(message, e);
                throw new CloudbreakRuntimeException(message, e);
            } catch (Exception e) {
                LOGGER.info("Rotation of freeipa bind password failed", e);
                throw new SecretRotationException("Freeipa bind password rotation failed", e);
            }
        } else {
            throw new SecretRotationException("Freeipa LDAP bind password is not in rotation state in Vault, thus rotation is not possible.");
        }
    }

    private Optional<String> getBindUserName(FreeIpaLdapBindPasswordRotationContext context) {
        String username = null;
        if (StringUtils.isBlank(context.getClusterName()) && context.rotateUserSyncUser()) {
            username = userSyncBindUserService.getUserSyncBindUserName(context.getResourceCrn());
        }
        if (StringUtils.isNotBlank(context.getClusterName()) && !context.rotateUserSyncUser()) {
            username = userNameProvider.createBindUserName(context.getClusterName());
        }
        return Optional.ofNullable(username);
    }

    @Override
    protected void finalize(FreeIpaLdapBindPasswordRotationContext rotationContext) throws Exception {

    }

    @Override
    protected void preValidate(FreeIpaLdapBindPasswordRotationContext rotationContext) throws Exception {

    }

    @Override
    protected void postValidate(FreeIpaLdapBindPasswordRotationContext rotationContext) throws Exception {
    }

    @Override
    protected Class<FreeIpaLdapBindPasswordRotationContext> getContextClass() {
        return FreeIpaLdapBindPasswordRotationContext.class;
    }

    @Override
    public SecretRotationStep getType() {
        return FREEIPA_LDAP_BIND_PASSWORD;
    }
}
