package com.sequenceiq.cloudbreak.aspect;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.service.secret.SecretValue;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.service.secret.domain.AccountIdAwareResource;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.domain.SecretProxy;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.workspace.model.TenantAwareResource;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

@Component
@Aspect
public class SecretAspects {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretAspects.class);

    @Inject
    private SecretService secretService;

    @Inject
    private Clock clock;

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.repository..*.save(..)) ")
    public void onRepositorySave() {
    }

    @Pointcut("execution(public void com.sequenceiq.cloudbreak.repository..*.delete(..)) ")
    public void onRepositoryDelete() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.repository..*.saveAll(..)) ")
    public void onRepositorySaveAll() {
    }

    @Pointcut("execution(public void com.sequenceiq.cloudbreak.repository..*.deleteAll(..)) ")
    public void onRepositoryDeleteAll() {
    }

    @Around("onRepositorySave()")
    public Object proceedOnRepositorySave(ProceedingJoinPoint proceedingJoinPoint) {
        return proceedSave(proceedingJoinPoint);
    }

    @Around("onRepositoryDelete()")
    public Object proceedOnRepositoryDelete(ProceedingJoinPoint proceedingJoinPoint) {
        return proceedDelete(proceedingJoinPoint);
    }

    @Around("onRepositorySaveAll()")
    public Object proceedOnRepositorySaveAll(ProceedingJoinPoint proceedingJoinPoint) {
        return proceedSave(proceedingJoinPoint);
    }

    @Around("onRepositoryDeleteAll()")
    public Object proceedOnRepositoryDeleteAll(ProceedingJoinPoint proceedingJoinPoint) {
        return proceedDelete(proceedingJoinPoint);
    }

    private Object proceedSave(ProceedingJoinPoint proceedingJoinPoint) {
        Collection<Object> entities = convertFirstArgToCollection(proceedingJoinPoint);
        for (Object entity : entities) {
            String tenant = null;
            try {
                for (Field field : entity.getClass().getDeclaredFields()) {
                    if (field.isAnnotationPresent(SecretValue.class)) {
                        LOGGER.debug("Found SecretValue annotation on {}", field);
                        field.setAccessible(true);
                        Secret value = (Secret) field.get(entity);
                        if (value != null && value.getRaw() != null && value.getSecret() == null) {
                            tenant = Optional.ofNullable(tenant).orElseGet(() -> findTenant(entity));
                            String path = String.format("%s/%s/%s/%s-%s", tenant,
                                    entity.getClass().getSimpleName().toLowerCase(), field.getName().toLowerCase(),
                                    UUID.randomUUID().toString(), Long.toHexString(clock.getCurrentTimeMillis()));
                            String secret = secretService.put(path, value.getRaw());
                            LOGGER.debug("Field: '{}' is saved at path: {}", field.getName(), path);
                            field.set(entity, new SecretProxy(secretService, secret));
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                LOGGER.error("Given entity isn't instance of TenantAwareResource or AccountIdAwareResource. Secret is not saved!", e);
                throw new CloudbreakServiceException(e);
            } catch (Exception e) {
                LOGGER.warn("Looks like something went wrong with Secret store. Secret is not saved!", e);
                throw new CloudbreakServiceException(e);
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
            throw new CloudbreakServiceException(throwable);
        }

        return proceed;
    }

    private Object proceedDelete(ProceedingJoinPoint proceedingJoinPoint) {
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
                LOGGER.error("Given entity isn't instance of TenantAwareResource or AccountIdAwareResource. Secret is not deleted!", e);
                throw new CloudbreakServiceException(e);
            } catch (Exception e) {
                LOGGER.warn("Looks like something went wrong with Secret store. Secret is not deleted!", e);
                throw new CloudbreakServiceException(e);
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
            throw new CloudbreakServiceException(throwable);
        }
        return proceed;
    }

    private Collection<Object> convertFirstArgToCollection(ProceedingJoinPoint proceedingJoinPoint) {
        Object arg = proceedingJoinPoint.getArgs()[0];
        return arg instanceof Collection ? (Collection<Object>) arg : Collections.singleton(arg);
    }

    private String findTenant(Object entity) {
        if (entity != null) {
            if (entity instanceof TenantAwareResource) {
                String tenant = ((TenantAwareResource) entity).getTenant().getName();
                if (Strings.isNullOrEmpty(tenant)) {
                    throw new IllegalArgumentException(String.format("Tenant name is null on %s object", entity));
                }
                return tenant;
            } else if (entity instanceof AccountIdAwareResource) {
                String accountId = ((AccountIdAwareResource) entity).getAccountId();
                if (Strings.isNullOrEmpty(accountId)) {
                    throw new IllegalArgumentException(String.format("Account Id is null on %s object", entity));
                }
                return accountId;
            } else {
                throw new IllegalArgumentException(
                        String.format("%s must be a subclass of %s or %s", entity.getClass().getSimpleName(), TenantAwareResource.class.getSimpleName(),
                                AccountIdAwareResource.class.getSimpleName()));
            }
        } else {
            throw new IllegalArgumentException("The entity is null");
        }
    }
}
