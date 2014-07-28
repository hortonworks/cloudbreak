package com.sequenceiq.cloudbreak.service.history;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.HistoryEvent;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    void aroundSave(ProceedingJoinPoint pjp) throws Exception {
        LOGGER.info("Advice around save:  {}", pjp.getSignature());
        HistoryEvent event = HistoryEvent.IGNORE;
        // save always has a single argument
        ProvisionEntity toBeSaved = (ProvisionEntity) pjp.getArgs()[0];
        if (historyService.isEntitySupported(toBeSaved)) {
            event = getEventType(toBeSaved);
        }
        try {
            pjp.proceed();
        } catch (Throwable throwable) {
            throw new Exception(throwable);
        }
        historyService.notify(toBeSaved, event);
    }

    @Around("repositoryDelete() && !historyRepository()")
    void aroundDelete(ProceedingJoinPoint pjp) throws Exception {
        LOGGER.info("Advice around delete:  {}", pjp.getSignature());
        ProvisionEntity toBeDeleted = (ProvisionEntity) pjp.getArgs()[0];
        try {
            pjp.proceed();
        } catch (Throwable throwable) {
            throw new Exception(throwable);
        }
        historyService.notify(toBeDeleted, HistoryEvent.DELETED);
    }

    private HistoryEvent getEventType(ProvisionEntity entity) {
        // new entities don't have ids before save
        Long id = null;
        if (entity instanceof Cluster) {
            id = ((Cluster) entity).getId();
        } else if (entity instanceof Blueprint) {
            id = ((Blueprint) entity).getId();
        } else if (entity instanceof Stack) {
            id = ((Stack) entity).getId();
        } else if (entity instanceof Template) {
            id = ((Blueprint) entity).getId();
        } else if (entity instanceof Credential) {
            id = ((Credential) entity).getId();
        }
        return null == id ? HistoryEvent.CREATED : HistoryEvent.UPDATED;
    }
}
