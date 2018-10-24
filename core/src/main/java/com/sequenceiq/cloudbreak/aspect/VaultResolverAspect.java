package com.sequenceiq.cloudbreak.aspect;

import java.lang.reflect.Method;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.vault.VaultResolver;
import com.sequenceiq.cloudbreak.service.vault.VaultService;

@Component
@Aspect
public class VaultResolverAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultResolverAspect.class);

    @Inject
    private VaultService vaultService;

    @Around("@annotation(com.sequenceiq.cloudbreak.aspect.vault.VaultResolver)")
    public Object resolveVaultPath(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = signature.getMethod();
        VaultResolver vaultResolver = method.getAnnotation(VaultResolver.class);
        String path = (String) proceedingJoinPoint.proceed();
        return vaultService.resolveSingleValue(path, vaultResolver.value());
    }

}
