package com.sequenceiq.cloudbreak.service.history;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.HistoryEvent;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

@Service
@Aspect
public class HistoryAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryAspect.class);

    @Autowired
    private HistoryService historyService;

    @Pointcut("execution(* com.sequenceiq.cloudbreak.repository.*Repository.save(..))")
    private void repositorySave() {
    }

    @Pointcut("execution(* com.sequenceiq.cloudbreak.repository.*Repository.delete(..))")
    private void repositoryDelete() {
    }

    @Pointcut("execution(* com.sequenceiq.cloudbreak.repository.*HistoryRepository.*(..) )")
    private void historyRepository() {
    }

    @Around("repositorySave() && !historyRepository()")
    public ProvisionEntity aroundSave(ProceedingJoinPoint pjp) {
        LOGGER.info("Advice around save:  {}", pjp.getSignature());
        ProvisionEntity ret = null;
        // save always has a single argument
        ProvisionEntity toBeSaved = (ProvisionEntity) pjp.getArgs()[0];
        HistoryEvent event = historyService.getEventType(toBeSaved);
        try {
            ret = (ProvisionEntity) pjp.proceed();
        } catch (Throwable throwable) {
            throw new RuntimeException("Failed to save ...", throwable);
        }
        historyService.notify(toBeSaved, event);
        return ret;
    }

    @Around("repositoryDelete() && !historyRepository()")
    void aroundDelete(ProceedingJoinPoint pjp) {
        LOGGER.info("Advice around delete:  {}", pjp.getSignature());
        ProvisionEntity toBeDeleted = (ProvisionEntity) pjp.getArgs()[0];
        try {
            pjp.proceed();
        } catch (Throwable throwable) {
            throw new RuntimeException("Failed to delete ...", throwable);
        }
        historyService.notify(toBeDeleted, HistoryEvent.DELETED);
    }
}
