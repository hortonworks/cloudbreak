package com.sequenceiq.cloudbreak.aspect;

import java.lang.reflect.Field;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
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
    public void allRepositories() {
    }

    @Around("allRepositories()")
    public Object hasPermission(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object entity = proceedingJoinPoint.getArgs()[0];
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        try{
            for (Field field : methodSignature.getReturnType().getDeclaredFields()) {
                if (field.isAnnotationPresent(VaultValue.class)) {
                    LOGGER.info("Found annotation on {}", field);
                    field.setAccessible(true);
                    //TODO: get type of secret from Class annotation
                    VaultIdentifier vaultIdentifier = (VaultIdentifier) entity;
                    String resourceId = vaultIdentifier.getUniqueIdentifier();
                    //TODO: should create the map of all properties beforehand
                    String path = vaultService.addFieldToSecret("pepsi/cb/clusters", resourceId, field.getName(), (String) field.get(entity));
                    LOGGER.info("Field: {} saved at path: {}", field.getName(), path);
                    field.set(entity, path);
                    //TODO: should override original value of field with path of resource
                }
            }
        } catch (Exception e){
            LOGGER.warn("Looks like something went wrong with Vault. Data is not encrypted!", e);
        }

        Object proceed = proceedingJoinPoint.proceed();
        return proceed;
    }

}
