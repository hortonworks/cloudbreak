package com.sequenceiq.environment.credential.aspect;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.secret.service.SecretAspectService;

@Component
@Aspect
public class SecretAspects {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretAspects.class);

    @Inject
    private SecretAspectService secretAspectService;

    @Pointcut("execution(public * com.sequenceiq.environment.credential.repository..*.save(..)) ")
    public void onRepositorySave() {
    }

    @Pointcut("execution(public void com.sequenceiq.environment.credential.repository..*.delete(..)) ")
    public void onRepositoryDelete() {
    }

    @Pointcut("execution(public * com.sequenceiq.environment.credential.repository..*.saveAll(..)) ")
    public void onRepositorySaveAll() {
    }

    @Pointcut("execution(public void com.sequenceiq.environment.credential.repository..*.deleteAll(..)) ")
    public void onRepositoryDeleteAll() {
    }

    @Around("onRepositorySave()")
    public Object proceedOnRepositorySave(ProceedingJoinPoint proceedingJoinPoint) {
        return secretAspectService.proceedSave(proceedingJoinPoint);
    }

    @Around("onRepositoryDelete()")
    public Object proceedOnRepositoryDelete(ProceedingJoinPoint proceedingJoinPoint) {
        return secretAspectService.proceedDelete(proceedingJoinPoint);
    }

    @Around("onRepositorySaveAll()")
    public Object proceedOnRepositorySaveAll(ProceedingJoinPoint proceedingJoinPoint) {
        return secretAspectService.proceedSave(proceedingJoinPoint);
    }

    @Around("onRepositoryDeleteAll()")
    public Object proceedOnRepositoryDeleteAll(ProceedingJoinPoint proceedingJoinPoint) {
        return secretAspectService.proceedDelete(proceedingJoinPoint);
    }
}
