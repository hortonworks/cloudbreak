package com.sequenceiq.freeipa.service.rotation.executor;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.USER_DATA;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.executor.AbstractRotationExecutor;
import com.sequenceiq.cloudbreak.rotation.secret.userdata.UserDataRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.userdata.UserDataSecretModifier;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.util.UserDataReplacer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.image.userdata.UserDataService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class UserDataRotationExecutor extends AbstractRotationExecutor<UserDataRotationContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDataRotationExecutor.class);

    @Inject
    private StackService stackService;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Inject
    private UserDataService userDataService;

    @Override
    protected void rotate(UserDataRotationContext rotationContext) {
        LOGGER.info("Userdata rotation is requested {}", rotationContext);
        modifyUserData(rotationContext, useNewSecret());
        LOGGER.info("Userdata rotation is completed.");
    }

    @Override
    protected void rollback(UserDataRotationContext rotationContext) {
        LOGGER.info("Userdata rollback is requested {}", rotationContext);
        modifyUserData(rotationContext, useOldSecret());
        LOGGER.info("Userdata rollback is completed.");
    }

    @Override
    protected void finalize(UserDataRotationContext rotationContext) {

    }

    @Override
    protected void preValidate(UserDataRotationContext rotationContext) throws Exception {

    }

    @Override
    protected void postValidate(UserDataRotationContext rotationContext) throws Exception {

    }

    @Override
    public SecretRotationStep getType() {
        return USER_DATA;
    }

    @Override
    protected Class<UserDataRotationContext> getContextClass() {
        return UserDataRotationContext.class;
    }

    private void modifyUserData(UserDataRotationContext context, Function<RotationSecret, String> secretSelector) {
        Map<String, RotationSecret> secrets = getSecrets(context.getSecretModifierMap().stream().map(Pair::getRight).collect(Collectors.toList()));
        Crn environmentCrn = Crn.safeFromString(context.getResourceCrn());
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(context.getResourceCrn(), environmentCrn.getAccountId());
        userDataService.updateUserData(stack.getId(), userData -> modifyUserData(userData, secrets, context, secretSelector));
    }

    private Function<RotationSecret, String> useNewSecret() {
        return RotationSecret::getSecret;
    }

    private Function<RotationSecret, String> useOldSecret() {
        return RotationSecret::getBackupSecret;
    }

    private Map<String, RotationSecret> getSecrets(List<String> values) {
        return values
                .stream()
                .collect(Collectors.toMap(vaultPath -> vaultPath, vaultPath -> {
                    RotationSecret rotationSecret = uncachedSecretServiceForRotation.getRotation(vaultPath);
                    if (!rotationSecret.isRotation()) {
                        LOGGER.error("Secret {} is not in a rotated state. User data modification failed.", vaultPath);
                        throw new SecretRotationException("Secret is not in a rotated state. User data modification failed.");
                    }
                    return rotationSecret;
                }));
    }

    private String modifyUserData(String userData, Map<String, RotationSecret> secrets, UserDataRotationContext context,
            Function<RotationSecret, String> secretSelector) {
        UserDataReplacer userDataReplacer = new UserDataReplacer(userData);
        context.getSecretModifierMap()
                .forEach(pair -> {
                    UserDataSecretModifier modifier = pair.getLeft();
                    String vaultPath = pair.getRight();
                    modifier.modify(userDataReplacer, secretSelector.apply(secrets.get(vaultPath)));
                });
        return userDataReplacer.getUserData();
    }
}
