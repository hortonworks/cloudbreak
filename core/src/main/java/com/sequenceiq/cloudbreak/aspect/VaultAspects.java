package com.sequenceiq.cloudbreak.aspect;

import java.lang.reflect.Field;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.vault.VaultIdentifier;
import com.sequenceiq.cloudbreak.aspect.vault.VaultValue;
import com.sequenceiq.cloudbreak.service.vault.VaultService;

@Component
@Aspect
public class VaultAspects {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultAspects.class);

    @Inject
    private VaultService vaultService;

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.repository.*.save(..)) ")
    public void onRepositorySave() {
    }

    @Pointcut("execution(public void com.sequenceiq.cloudbreak.repository.*.delete(..)) ")
    public void onRepositoryDelete() {
    }

    @Around("onRepositorySave()")
    public Object saveToVault(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object entity = proceedingJoinPoint.getArgs()[0];
        try {
            for (Field field : entity.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(VaultValue.class)) {
                    LOGGER.info("Found VaultValue annotation on {}", field);
                    field.setAccessible(true);
                    VaultIdentifier vaultIdentifier = (VaultIdentifier) entity;
                    String resourceId = vaultIdentifier.getUniqueIdentifier();
                    //TODO: should create the map of all properties beforehand
                    String value = (String) field.get(entity);
                    if (value == null || !value.startsWith("pepsi")) {
                        String resourceType = entity.getClass().getSimpleName().toLowerCase();
                        String path = vaultService.addFieldToSecret("pepsi/cb/" + resourceType, resourceId, field.getName(), value);
                        LOGGER.info("Field: {} saved at path: {}", field.getName(), path);
                        field.set(entity, path);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Looks like something went wrong with Vault. Data is not encrypted!", e);
        }

        Object proceed = proceedingJoinPoint.proceed();
        return proceed;
    }

    @Around("onRepositoryDelete()")
    public Object removeFromVault(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object entity = proceedingJoinPoint.getArgs()[0];
        try {
            for (Field field : entity.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(VaultValue.class)) {
                    LOGGER.info("Found VaultValue annotation on {}", field);
                    field.setAccessible(true);
                    String path = (String) field.get(entity);
                    vaultService.deleteSecret(path);
                    LOGGER.info("Secret deleted at path: {}", path);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Looks like something went wrong with Vault. Secret is not deleted!", e);
        }
        Object proceed = proceedingJoinPoint.proceed();
        return proceed;
    }

}
