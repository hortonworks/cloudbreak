package com.sequenceiq.thunderhead.aspect;

import jakarta.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.secret.service.SecretAspectService;

@Component
@Aspect
public class SecretAspects {

    @Inject
    private SecretAspectService secretAspectService;

    @Pointcut("execution(public * com.sequenceiq.thunderhead.repository..*.save(..)) ")
    public void onRepositorySave() {
    }

    @Pointcut("execution(public void com.sequenceiq.thunderhead.repository..*.delete(..)) ")
    public void onRepositoryDelete() {
    }

    @Pointcut("execution(public * com.sequenceiq.thunderhead.repository..*.saveAll(..)) ")
    public void onRepositorySaveAll() {
    }

    @Pointcut("execution(public void com.sequenceiq.thunderhead.repository..*.deleteAll(..)) ")
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
