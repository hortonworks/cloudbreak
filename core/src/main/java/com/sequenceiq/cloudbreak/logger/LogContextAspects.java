package com.sequenceiq.cloudbreak.logger;

import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.task.FetchTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.core.flow2.Flow2Handler;

import reactor.bus.Event;

@Component
@Aspect
public class LogContextAspects {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogContextAspects.class);

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler+.accept(..))")
    public void interceptCloudPlatformEventHandlersAcceptMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler+.accept(..))")
    public void interceptReactorHandlersAcceptMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.core.flow2.Flow2Handler.accept(..))")
    public void interceptFlow2HandlerAcceptMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.service.events.CloudbreakEventHandler.accept(..))")
    public void interceptCloudbreakEventHandlerAcceptMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.cloud.handler.ResourcePersistenceHandler.accept(..))")
    public void interceptResourcePersistenceHandlerAcceptMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler.schedule(..))")
    public void interceptSchedulerScheduleMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.cloud.task.PollTask+.call(..))")
    public void interceptPollTasksCallMethod() {
    }

    @Before("com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptCloudPlatformEventHandlersAcceptMethod()")
    public void buildLogContextForCloudPlatformEventHandler(JoinPoint joinPoint) {
        Event<CloudPlatformRequest> event = (Event<CloudPlatformRequest>) joinPoint.getArgs()[0];
        CloudPlatformRequest cloudPlatformRequest = event.getData();
        CloudContext cloudContext = cloudPlatformRequest.getCloudContext();
        if (cloudContext != null) {
            MDCBuilder.buildMdcContext(String.valueOf(cloudContext.getId()), cloudContext.getName(), cloudContext.getOwner(), "STACK");
        }
        LOGGER.info("A CloudPlatformEventHandler's 'accept' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString());
    }

    @Before("com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptReactorHandlersAcceptMethod() ||"
            + "com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptFlow2HandlerAcceptMethod() ||"
            + "com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptCloudbreakEventHandlerAcceptMethod()")
    public void buildLogContextForReactorHandler(JoinPoint joinPoint) {
        Event<?> event = (Event<?>) joinPoint.getArgs()[0];
        Map<String, String> mdcContextMap = event.getHeaders().get(Flow2Handler.MDC_CONTEXT_ID);
        if (mdcContextMap != null) {
            MDCBuilder.buildMdcContextFromMap(mdcContextMap);
        }
        LOGGER.info("A Reactor event handler's 'accept' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString());
    }

    @Before("com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptResourcePersistenceHandlerAcceptMethod()")
    public void buildLogContextForPersistenceHandler(JoinPoint joinPoint) {
        Event<ResourceNotification> event = (Event<ResourceNotification>) joinPoint.getArgs()[0];
        CloudContext cloudContext = event.getData().getCloudContext();
        if (cloudContext != null) {
            MDCBuilder.buildMdcContext(String.valueOf(cloudContext.getId()), cloudContext.getName(), cloudContext.getOwner(), "STACK");
        }
        LOGGER.debug("A Resource persistence handler's 'accept' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString());
    }

    @Before("com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptSchedulerScheduleMethod()")
    public void buildLogContextForScheduler(JoinPoint joinPoint) {
        FetchTask task = (FetchTask) joinPoint.getArgs()[0];
        CloudContext cloudContext = task.getAuthenticatedContext().getCloudContext();
        MDCBuilder.buildMdcContext(String.valueOf(cloudContext.getId()), cloudContext.getName(), cloudContext.getOwner(), "STACK");
        LOGGER.info("A SyncPollingScheduler's 'schedule' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString());
    }

    @Before("com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptPollTasksCallMethod()")
    public void buildLogContextForPollTask(JoinPoint joinPoint) {
        PollTask pollTask = (PollTask) joinPoint.getTarget();
        CloudContext cloudContext = pollTask.getAuthenticatedContext().getCloudContext();
        MDCBuilder.buildMdcContext(String.valueOf(cloudContext.getId()), cloudContext.getName(), cloudContext.getOwner(), "STACK");
        LOGGER.debug("A PollTask's 'call' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString());
    }
}
