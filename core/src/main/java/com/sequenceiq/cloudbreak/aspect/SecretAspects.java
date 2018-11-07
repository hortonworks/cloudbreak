package com.sequenceiq.cloudbreak.aspect;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.vault.SecretValue;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.secret.SecretService;

@Component
@Aspect
public class SecretAspects {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretAspects.class);

    @Inject
    private SecretService secretService;

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.repository.*.save(..)) ")
    public void onRepositorySave() {
    }

    @Pointcut("execution(public void com.sequenceiq.cloudbreak.repository.*.delete(..)) ")
    public void onRepositoryDelete() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.repository.*.saveAll(..)) ")
    public void onRepositorySaveAll() {
    }

    @Pointcut("execution(public void com.sequenceiq.cloudbreak.repository.*.deleteAll(..)) ")
    public void onRepositoryDeleteAll() {
    }

    @Around("onRepositorySave()")
    public Object saveToVault(ProceedingJoinPoint proceedingJoinPoint) throws CloudbreakException {
        return proceedSave(proceedingJoinPoint);
    }

    @Around("onRepositoryDelete()")
    public Object removeFromVault(ProceedingJoinPoint proceedingJoinPoint) throws CloudbreakException {
        return proceedDelete(proceedingJoinPoint);
    }

    @Around("onRepositorySaveAll()")
    public Object saveAllToVault(ProceedingJoinPoint proceedingJoinPoint) throws CloudbreakException {
        return proceedSave(proceedingJoinPoint);
    }

    @Around("onRepositoryDeleteAll()")
    public Object removeAllFromVault(ProceedingJoinPoint proceedingJoinPoint) throws CloudbreakException {
        return proceedDelete(proceedingJoinPoint);
    }

    private Object proceedSave(ProceedingJoinPoint proceedingJoinPoint) throws CloudbreakException {
        Collection<Object> entities = convertFirstArgToCollection(proceedingJoinPoint);
        for (Object entity : entities) {
            try {
                for (Field field : entity.getClass().getDeclaredFields()) {
                    if (field.isAnnotationPresent(SecretValue.class)) {
                        LOGGER.debug("Found SecretValue annotation on {}", field);
                        field.setAccessible(true);
                        String value = (String) field.get(entity);
                        if (value != null && !secretService.isSecret(value)) {
                            String resourceType = entity.getClass().getSimpleName().toLowerCase();
                            String resourceId = UUID.randomUUID().toString();
                            String fieldName = field.getName().toLowerCase();
                            String path = String.format("%s/%s/%s", resourceType, fieldName, resourceId);
                            path = secretService.put(path, value);
                            LOGGER.debug("Field: '{}' is saved at path: {}", field.getName(), path);
                            field.set(entity, path);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Looks like something went wrong with Vault. Data is not encrypted!", e);
                throw new CloudbreakException(e);
            }
        }

        Object proceed;
        try {
            proceed = proceedingJoinPoint.proceed();
        } catch (Throwable throwable) {
            LOGGER.error("Failed to invoke repository save", throwable);
            throw new CloudbreakException(throwable);
        }
        return proceed;
    }

    private Object proceedDelete(ProceedingJoinPoint proceedingJoinPoint) throws CloudbreakException {
        Collection<Object> entities = convertFirstArgToCollection(proceedingJoinPoint);
        for (Object entity : entities) {
            try {
                for (Field field : entity.getClass().getDeclaredFields()) {
                    if (field.isAnnotationPresent(SecretValue.class)) {
                        LOGGER.debug("Found SecretValue annotation on {}", field);
                        field.setAccessible(true);
                        String path = (String) field.get(entity);
                        secretService.delete(path);
                        LOGGER.debug("Secret deleted at path: {}", path);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Looks like something went wrong with Vault. Secret is not deleted!", e);
                throw new CloudbreakException(e);
            }
        }

        Object proceed;
        try {
            proceed = proceedingJoinPoint.proceed();
        } catch (Throwable throwable) {
            LOGGER.error("Failed to invoke repository delete", throwable);
            throw new CloudbreakException(throwable);
        }
        return proceed;
    }

    private Collection<Object> convertFirstArgToCollection(ProceedingJoinPoint proceedingJoinPoint) {
        Object arg = proceedingJoinPoint.getArgs()[0];
        return arg instanceof Collection ? (Collection<Object>) arg : Collections.singleton(arg);
    }
}
