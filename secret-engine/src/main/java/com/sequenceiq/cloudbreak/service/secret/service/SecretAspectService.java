package com.sequenceiq.cloudbreak.service.secret.service;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.dal.model.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.SecretOperationException;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretProxy;

@Service
public class SecretAspectService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretAspectService.class);

    @Inject
    private List<CrudRepository> crudRepositories;

    private SecretService secretService;

    @Inject
    public SecretAspectService(SecretService secretService) {
        this.secretService = secretService;
    }

    @PostConstruct
    public void init() {
        LOGGER.info("Preinvoke all repository methods to avoid Aspectj bug. More details under CB-28003");
        for (CrudRepository crudRepository : crudRepositories) {
            try {
                crudRepository.save(null);
            } catch (Exception ignored) {
            }
            try {
                crudRepository.saveAll(null);
            } catch (Exception ignored) {
            }
            try {
                crudRepository.delete(null);
            } catch (Exception ignored) {
            }
            try {
                crudRepository.deleteAll(null);
            } catch (Exception ignored) {
            }
        }
    }

    public Object proceedSave(ProceedingJoinPoint proceedingJoinPoint) {
        Collection<Object> entities = convertFirstArgToCollection(proceedingJoinPoint);
        for (Object entity : entities) {
            try {
                if (entity != null) {
                    for (Field field : entity.getClass().getDeclaredFields()) {
                        if (field.isAnnotationPresent(SecretValue.class)) {
                            LOGGER.debug("Found SecretValue annotation on {}", field);
                            ReflectionUtils.makeAccessible(field);
                            Secret value = (Secret) field.get(entity);
                            if (value != null && value.getRaw() != null && value.getSecret() == null) {
                                String accountId = findAccountId(entity);
                                String path = String.format("%s/%s/%s/%s-%s", accountId,
                                        entity.getClass().getSimpleName().toLowerCase(Locale.ROOT), field.getName().toLowerCase(Locale.ROOT),
                                        UUID.randomUUID(), Long.toHexString(System.currentTimeMillis()));
                                String secret = secretService.put(path, value.getRaw());
                                LOGGER.debug("Field: '{}' is saved at path: {}", field.getName(), path);
                                field.set(entity, new SecretProxy(secret));
                            }
                        }
                    }
                } else {
                    LOGGER.warn("We have received null as entity, there is nothing to do from vault perspective.");
                }
            } catch (IllegalArgumentException e) {
                LOGGER.error("Given entity isn't instance of {}. Secret is not updated!",
                        AccountIdAwareResource.class.getSimpleName(), e);
                throw new SecretOperationException(e.getMessage());
            } catch (Exception e) {
                LOGGER.warn("Looks like something went wrong with Secret store. Secret is not updated!", e);
                throw new SecretOperationException(e.getMessage());
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
                if (entity != null) {
                    for (Field field : entity.getClass().getDeclaredFields()) {
                        if (field.isAnnotationPresent(SecretValue.class)) {
                            LOGGER.debug("Found SecretValue annotation on {}", field);
                            ReflectionUtils.makeAccessible(field);
                            Secret path = (Secret) field.get(entity);
                            if (path != null && path.getSecret() != null) {
                                secretService.deleteByVaultSecretJson(path.getSecret());
                                LOGGER.debug("Secret deleted at path: {}", path);
                            } else {
                                LOGGER.debug("Secret is null for field: {}.{}", field.getDeclaringClass(), field.getName());
                            }
                        }
                    }
                } else {
                    LOGGER.warn("We have received null as entity, there is nothing to do from vault perspective.");
                }
            } catch (IllegalArgumentException e) {
                LOGGER.error("Given entity isn't instance of {}. Secret is not deleted!",
                        AccountIdAwareResource.class.getSimpleName(), e);
                throw new SecretOperationException(e.getMessage());
            } catch (Exception e) {
                LOGGER.warn("Looks like something went wrong with Secret store. Secret is not deleted!", e);
                throw new SecretOperationException(e.getMessage());
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
        if (proceedingJoinPoint.getArgs().length == 0) {
            return Collections.emptySet();
        }

        Object arg = proceedingJoinPoint.getArgs()[0];
        return arg instanceof Collection ? (Collection<Object>) arg : Collections.singleton(arg);
    }

    private String findAccountId(Object entity) throws IllegalArgumentException {
        if (entity != null) {
            if (entity instanceof AccountIdAwareResource) {
                String accountId = ((AccountIdAwareResource) entity).getAccountId();
                if (Strings.isNullOrEmpty(accountId)) {
                    throw new IllegalArgumentException("Account id is null on " + entity + " object.");
                }
                return accountId;
            } else {
                throw new IllegalArgumentException(
                        entity.getClass().getSimpleName() + " must be a subclass of " + AccountIdAwareResource.class.getSimpleName());
            }
        } else {
            throw new IllegalArgumentException("The entity is null");
        }
    }
}
