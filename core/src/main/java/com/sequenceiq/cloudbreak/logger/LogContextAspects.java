package com.sequenceiq.cloudbreak.logger;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.task.FetchTask;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

import reactor.bus.Event;

@Component
@Aspect
public class LogContextAspects {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogContextAspects.class);

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler+.accept(..))")
    public void interceptReactorHandlersAcceptMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler.schedule(..))")
    public void interceptSchedulerScheduleMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.cloud.task.PollTask+.call(..))")
    public void interceptPollTasksCallMethod() {
    }

    @Before("com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptReactorHandlersAcceptMethod()")
    public void buildLogContextForReactorHandler(JoinPoint joinPoint) {
        Event<CloudPlatformRequest> event = (Event<CloudPlatformRequest>) joinPoint.getArgs()[0];
        CloudPlatformRequest cloudPlatformRequest = event.getData();
        CloudContext cloudContext = cloudPlatformRequest.getCloudContext();
        if (cloudContext != null) {
            MDCBuilder.buildMdcContext(String.valueOf(cloudContext.getId()), cloudContext.getName(), cloudContext.getOwner());
        }
        LOGGER.info("A Reactor event handler's 'accept' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString());
    }

    @Before("com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptSchedulerScheduleMethod()")
    public void buildLogContextForScheduler(JoinPoint joinPoint) {
        FetchTask task = (FetchTask) joinPoint.getArgs()[0];
        CloudContext cloudContext = task.getAuthenticatedContext().getCloudContext();
        MDCBuilder.buildMdcContext(String.valueOf(cloudContext.getId()), cloudContext.getName(), cloudContext.getOwner());
        LOGGER.info("A SyncPollingScheduler's 'schedule' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString());
    }

    @Before("com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptPollTasksCallMethod()")
    public void buildLogContextForPollTask(JoinPoint joinPoint) {
        PollTask pollTask = (PollTask) joinPoint.getTarget();
        CloudContext cloudContext = pollTask.getAuthenticatedContext().getCloudContext();
        MDCBuilder.buildMdcContext(String.valueOf(cloudContext.getId()), cloudContext.getName(), cloudContext.getOwner());
        LOGGER.debug("A PollTask's 'call' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString());
    }
}
