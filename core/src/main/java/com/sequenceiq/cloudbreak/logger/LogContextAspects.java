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

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainHandler.accept(..))")
    public void interceptFlowChainHandlerAcceptMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.service.events.CloudbreakEventHandler.accept(..))")
    public void interceptCloudbreakEventHandlerAcceptMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.cloud.handler.ResourcePersistenceHandler.accept(..))")
    public void interceptResourcePersistenceHandlerAcceptMethod() {
    }

    @Before("com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptCloudPlatformEventHandlersAcceptMethod()")
    public void buildLogContextForCloudPlatformEventHandler(JoinPoint joinPoint) {
        Event<CloudPlatformRequest> event = (Event<CloudPlatformRequest>) joinPoint.getArgs()[0];
        CloudPlatformRequest cloudPlatformRequest = event.getData();
        CloudContext cloudContext = cloudPlatformRequest.getCloudContext();
        buildMdcContext(cloudContext, event);
        LOGGER.info("A CloudPlatformEventHandler's 'accept' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString());
    }

    @Before("com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptReactorHandlersAcceptMethod() ||"
            + "com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptFlow2HandlerAcceptMethod() ||"
            + "com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptCloudbreakEventHandlerAcceptMethod() ||"
            + "com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptFlowChainHandlerAcceptMethod()")
    public void buildLogContextForReactorHandler(JoinPoint joinPoint) {
        Event<?> event = (Event<?>) joinPoint.getArgs()[0];
        Map<String, String> mdcContextMap = event.getHeaders().get(MDCBuilder.MDC_CONTEXT_ID);
        if (mdcContextMap != null) {
            MDCBuilder.buildMdcContextFromMap(mdcContextMap);
            String trackingId = mdcContextMap.get(LoggerContextKey.TRACKING_ID.toString());
            MDCBuilder.addTrackingIdToMdcContext(trackingId);
        }
        LOGGER.info("A Reactor event handler's 'accept' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString());
    }

    @Before("com.sequenceiq.cloudbreak.logger.LogContextAspects.interceptResourcePersistenceHandlerAcceptMethod()")
    public void buildLogContextForPersistenceHandler(JoinPoint joinPoint) {
        Event<ResourceNotification> event = (Event<ResourceNotification>) joinPoint.getArgs()[0];
        CloudContext cloudContext = event.getData().getCloudContext();
        buildMdcContext(cloudContext, event);
        LOGGER.debug("A Resource persistence handler's 'accept' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString());
    }

    private void buildMdcContext(CloudContext cloudContext, Event<?> event) {
        Map<String, String> eventMdcContext = event.getHeaders().get(MDCBuilder.MDC_CONTEXT_ID);
        if (cloudContext != null) {
            String flowId = eventMdcContext != null ? eventMdcContext.get(LoggerContextKey.FLOW_ID.toString()) : null;
            MDCBuilder.addFlowIdToMdcContext(flowId);
            String trackingId = eventMdcContext != null ? eventMdcContext.get(LoggerContextKey.TRACKING_ID.toString()) : null;
            MDCBuilder.addTrackingIdToMdcContext(trackingId);
            MDCBuilder.buildMdcContext(stringValue(cloudContext.getId()), stringValue(cloudContext.getName()), "STACK");
        } else {
            MDCBuilder.buildMdcContextFromMap(eventMdcContext);
        }
    }

    private String stringValue(Object object) {
        return object == null ? "" : object.toString();
    }
}
