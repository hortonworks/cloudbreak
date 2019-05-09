package com.sequenceiq.secret.service;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.secret.SecretOperationException;
import com.sequenceiq.secret.SecretValue;
import com.sequenceiq.secret.domain.Secret;
import com.sequenceiq.secret.domain.SecretProxy;

@Service
public class SecretAspectService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretAspectService.class);

    private SecretService secretService;

    @Inject
    public SecretAspectService(SecretService secretService) {
        this.secretService = secretService;
    }

    public Object proceedSave(ProceedingJoinPoint proceedingJoinPoint) {
        Collection<Object> entities = convertFirstArgToCollection(proceedingJoinPoint);
        for (Object entity : entities) {
            try {
                for (Field field : entity.getClass().getDeclaredFields()) {
                    if (field.isAnnotationPresent(SecretValue.class)) {
                        LOGGER.debug("Found SecretValue annotation on {}", field);
                        field.setAccessible(true);
                        Secret value = (Secret) field.get(entity);
                        if (value != null && value.getRaw() != null && value.getSecret() == null) {
                            String path = String.format("%s/%s/%s-%s",
                                    entity.getClass().getSimpleName().toLowerCase(), field.getName().toLowerCase(),
                                    UUID.randomUUID().toString(), Long.toHexString(System.currentTimeMillis()));
                            String secret = secretService.put(path, value.getRaw());
                            LOGGER.debug("Field: '{}' is saved at path: {}", field.getName(), path);
                            field.set(entity, new SecretProxy(secretService, secret));
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                LOGGER.error("Given entity isn't instance of TenantAwareResource. Secret is not deleted!", e);
                throw new SecretOperationException(e);
            } catch (Exception e) {
                LOGGER.warn("Looks like something went wrong with Secret store. Secret is not deleted!", e);
                throw new SecretOperationException(e);
            }
        }

        Object proceed;
        try {
            proceed = proceedingJoinPoint.proceed();
        } catch (RuntimeException re) {
            LOGGER.warn("Failed to invoke repository save", re);
            throw re;
        } catch (Throwable throwable) {
            LOGGER.error("Failed to invoke repository save", throwable);
            throw new SecretOperationException(throwable);
        }

        return proceed;
    }

    public Object proceedDelete(ProceedingJoinPoint proceedingJoinPoint) {
        Collection<Object> entities = convertFirstArgToCollection(proceedingJoinPoint);
        for (Object entity : entities) {
            try {
                for (Field field : entity.getClass().getDeclaredFields()) {
                    if (field.isAnnotationPresent(SecretValue.class)) {
                        LOGGER.debug("Found SecretValue annotation on {}", field);
                        field.setAccessible(true);
                        Secret path = (Secret) field.get(entity);
                        if (path != null && path.getSecret() != null) {
                            secretService.delete(path.getSecret());
                            LOGGER.debug("Secret deleted at path: {}", path);
                        } else {
                            LOGGER.debug("Secret is null for field: {}.{}", field.getDeclaringClass(), field.getName());
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                LOGGER.error("Given entity isn't instance of TenantAwareResource. Secret is not deleted!", e);
                throw new SecretOperationException(e);
            } catch (Exception e) {
                LOGGER.warn("Looks like something went wrong with Secret store. Secret is not deleted!", e);
                throw new SecretOperationException(e);
            }
        }

        Object proceed;
        try {
            proceed = proceedingJoinPoint.proceed();
        } catch (RuntimeException re) {
            LOGGER.warn("Failed to invoke repository delete", re);
            throw re;
        } catch (Throwable throwable) {
            LOGGER.error("Failed to invoke repository delete", throwable);
            throw new SecretOperationException(throwable);
        }
        return proceed;
    }

    public Collection<Object> convertFirstArgToCollection(ProceedingJoinPoint proceedingJoinPoint) {
        Object arg = proceedingJoinPoint.getArgs()[0];
        return arg instanceof Collection ? (Collection<Object>) arg : Collections.singleton(arg);
    }
}
