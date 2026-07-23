package com.sequenceiq.redbeams.service.security;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

import jakarta.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.service.secret.SecretOperationException;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretProxy;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

@Service
public class RecursiveSecretAspectService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecursiveSecretAspectService.class);

    private SecretService secretService;

    @Inject
    public RecursiveSecretAspectService(SecretService secretService) {
        this.secretService = secretService;
    }

    public Object proceedSave(ProceedingJoinPoint proceedingJoinPoint) {
        Queue<Object> entities = convertFirstArgToQueue(proceedingJoinPoint);
        while (!entities.isEmpty()) {
            Object entity = entities.poll();
            if (entity != null) {
                try {
                    for (Field field : entity.getClass().getDeclaredFields()) {
                        if (field.isAnnotationPresent(SecretValue.class)) {
                            LOGGER.info("Found SecretValue annotation on {} in entity of type {}", field, entity.getClass());
                            ReflectionUtils.makeAccessible(field);
                            Object fieldValue = field.get(entity);
                            if (fieldValue instanceof Secret value) {
                                if (value.getRaw() != null && value.getSecret() == null) {
                                    String path = String.format("%s/%s/%s-%s",
                                            entity.getClass().getSimpleName().toLowerCase(), field.getName().toLowerCase(),
                                            UUID.randomUUID().toString(), Long.toHexString(System.currentTimeMillis()));
                                    String secret = secretService.put(path, value.getRaw());
                                    LOGGER.info("Field: '{}' is saved at path: {}", field.getName(), path);
                                    field.set(entity, new SecretProxy(secret));
                                }
                            } else if (fieldValue != null) {
                                entities.add(fieldValue);
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Given entity isn't instance of TenantAwareResource. Secret is not saved!", e);
                    throw new SecretOperationException(e);
                } catch (Exception e) {
                    LOGGER.warn("Looks like something went wrong with Secret store. Secret is not saved!", e);
                    throw new SecretOperationException(e);
                }
            } else {
                LOGGER.warn("We have received null as entity, there is nothing to do from vault perspective.");
            }
        }

        Object proceed;
        try {
            proceed = proceedingJoinPoint.proceed();
        } catch (IllegalArgumentException | UnsupportedOperationException | InvalidDataAccessApiUsageException ex) {
            LOGGER.warn("Exception during repository save: {}: {}", ex.getClass(), ex.getMessage());
            throw ex;
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
        Queue<Object> entities = convertFirstArgToQueue(proceedingJoinPoint);
        while (!entities.isEmpty()) {
            Object entity = entities.poll();
            if (entity != null) {
                try {
                    for (Field field : entity.getClass().getDeclaredFields()) {
                        if (field.isAnnotationPresent(SecretValue.class)) {
                            LOGGER.info("Found SecretValue annotation on {} in entity of type {}", field, entity.getClass());
                            ReflectionUtils.makeAccessible(field);
                            Object fieldValue = field.get(entity);
                            if (fieldValue instanceof Secret path) {
                                if (path.getSecret() != null) {
                                    secretService.deleteByVaultSecretJson(path.getSecret());
                                    LOGGER.info("Secret deleted at path: {}", path);
                                } else {
                                    LOGGER.info("Secret is null for field: {}.{}", field.getDeclaringClass(), field.getName());
                                }
                            } else if (fieldValue != null) {
                                entities.add(fieldValue);
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
            } else {
                LOGGER.warn("We have received null as entity, there is nothing to do from vault perspective.");
            }
        }

        Object proceed;
        try {
            proceed = proceedingJoinPoint.proceed();
        } catch (IllegalArgumentException | UnsupportedOperationException | InvalidDataAccessApiUsageException ex) {
            LOGGER.warn("Exception during repository delete: {}: {}", ex.getClass(), ex.getMessage());
            throw ex;
        } catch (RuntimeException re) {
            LOGGER.warn("Failed to invoke repository delete", re);
            throw re;
        } catch (Throwable throwable) {
            LOGGER.error("Failed to invoke repository delete", throwable);
            throw new SecretOperationException(throwable);
        }
        return proceed;
    }

    public Queue<Object> convertFirstArgToQueue(ProceedingJoinPoint proceedingJoinPoint) {
        Object arg = proceedingJoinPoint.getArgs()[0];
        if (arg instanceof Collection) {
            return new LinkedList<Object>((Collection<Object>) arg);
        } else {
            LinkedList<Object> l = new LinkedList<>();
            l.add(arg);
            return l;
        }
    }
}
