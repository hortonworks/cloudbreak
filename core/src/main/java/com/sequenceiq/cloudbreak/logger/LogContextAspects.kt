package com.sequenceiq.cloudbreak.logger

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification
import com.sequenceiq.cloudbreak.cloud.task.FetchTask
import com.sequenceiq.cloudbreak.cloud.task.PollTask

import reactor.bus.Event

@Component
@Aspect
class LogContextAspects {

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler+.accept(..))")
    fun interceptReactorHandlersAcceptMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.cloud.handler.ResourcePersistenceHandler.accept(..))")
    fun interceptResourcePersistenceHandlerAcceptMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler.schedule(..))")
    fun interceptSchedulerScheduleMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.cloud.task.PollTask+.call(..))")
    fun interceptPollTasksCallMethod() {
    }

    @Before("com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptReactorHandlersAcceptMethod()")
    fun buildLogContextForReactorHandler(joinPoint: JoinPoint) {
        val event = joinPoint.args[0] as Event<CloudPlatformRequest<Any>>
        val cloudPlatformRequest = event.data
        val cloudContext = cloudPlatformRequest.cloudContext
        if (cloudContext != null) {
            MDCBuilder.buildMdcContext(cloudContext.id.toString(), cloudContext.name, cloudContext.owner)
        }
        LOGGER.info("A Reactor event handler's 'accept' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString())
    }

    @Before("com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptResourcePersistenceHandlerAcceptMethod()")
    fun buildLogContextForPersistenceHandler(joinPoint: JoinPoint) {
        val event = joinPoint.args[0] as Event<ResourceNotification>
        val cloudContext = event.data.cloudContext
        if (cloudContext != null) {
            MDCBuilder.buildMdcContext(cloudContext.id.toString(), cloudContext.name, cloudContext.owner)
        }
        LOGGER.debug("A Resource persistence handler's 'accept' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString())
    }

    @Before("com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptSchedulerScheduleMethod()")
    fun buildLogContextForScheduler(joinPoint: JoinPoint) {
        val task = joinPoint.args[0] as FetchTask<Any>
        val cloudContext = task.authenticatedContext.cloudContext
        MDCBuilder.buildMdcContext(cloudContext.id.toString(), cloudContext.name, cloudContext.owner)
        LOGGER.info("A SyncPollingScheduler's 'schedule' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString())
    }

    @Before("com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptPollTasksCallMethod()")
    fun buildLogContextForPollTask(joinPoint: JoinPoint) {
        val pollTask = joinPoint.target as PollTask<Any>
        val cloudContext = pollTask.authenticatedContext.cloudContext
        MDCBuilder.buildMdcContext(cloudContext.id.toString(), cloudContext.name, cloudContext.owner)
        LOGGER.debug("A PollTask's 'call' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString())
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LogContextAspects::class.java)
    }
}
