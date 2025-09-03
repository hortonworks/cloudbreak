package com.sequenceiq.cloudbreak.rotation.secret.vault;

import java.lang.reflect.Method;
import java.util.Collection;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.cloudbreak.app.StaticApplicationContext;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.secret.SecretGetter;
import com.sequenceiq.cloudbreak.service.secret.SecretMarker;
import com.sequenceiq.cloudbreak.service.secret.SecretSetter;
import com.sequenceiq.cloudbreak.service.secret.VaultRotationAwareRepository;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;

public class VaultRotationReflectionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultRotationReflectionUtil.class);

    private VaultRotationReflectionUtil() {

    }

    public static void saveEntity(Object entity) {
        Collection<CrudRepository> repositories = StaticApplicationContext.getAllMatchingBeans(CrudRepository.class);
        CrudRepository repository = repositories.stream()
                .filter(repo -> repo instanceof VaultRotationAwareRepository)
                .filter(repo -> ((VaultRotationAwareRepository) repo).getEntityClass().equals(entity.getClass()))
                .findFirst()
                .orElseThrow(() ->
                        new SecretRotationException(String.format("Failed to look up JPA repository for entity class %s", entity.getClass())));
        LOGGER.info("Repository {} found for entity {}.", repository.getClass(), entity.getClass());
        repository.save(entity);
    }

    public static String getVaultSecretJson(Object entity, SecretMarker secretMarker) {
        try {
            Method getterMethod = MethodUtils.getMethodsListWithAnnotation(entity.getClass(), SecretGetter.class).stream()
                    .filter(method -> method.getAnnotation(SecretGetter.class).marker().equals(secretMarker))
                    .findFirst()
                    .orElseThrow(() ->
                            new SecretRotationException(String.format("Failed to look up for vault secret by secret marker %s", secretMarker)));
            Object result = getterMethod.invoke(entity);
            if (result instanceof Secret) {
                return ((Secret) result).getSecret();
            } else if (result instanceof String) {
                return (String) result;
            } else {
                throw new CloudbreakServiceException("Secret get method is not annotated correctly, cannot extract secret json.");
            }
        } catch (Exception e) {
            throw new SecretRotationException(e);
        }
    }

    public static void setNewSecret(Object targetEntity, SecretMarker secretMarker, Secret newSecret) {
        try {
            Method setterMethod = MethodUtils.getMethodsListWithAnnotation(targetEntity.getClass(), SecretSetter.class).stream()
                    .filter(method -> method.getAnnotation(SecretSetter.class).marker().equals(secretMarker))
                    .filter(method -> method.getParameterCount() == 1)
                    .filter(method -> method.getParameterTypes()[0].isAssignableFrom(Secret.class))
                    .findFirst()
                    .orElseThrow(() ->
                            new SecretRotationException(String.format("Failed to update vault secret by secret marker %s", secretMarker)));
            setterMethod.invoke(targetEntity, newSecret);
        } catch (Exception e) {
            throw new SecretRotationException(e);
        }
    }
}
