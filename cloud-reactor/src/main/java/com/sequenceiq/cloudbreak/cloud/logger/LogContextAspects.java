package com.sequenceiq.cloudbreak.cloud.logger;

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
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import reactor.bus.Event;

@Component("CloudLogContextAspects")
@Aspect
public class LogContextAspects {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogContextAspects.class);

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler+.accept(..))")
    public void interceptCloudPlatformEventHandlersAcceptMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.cloud.handler.ResourcePersistenceHandler.accept(..))")
    public void interceptResourcePersistenceHandlerAcceptMethod() {
    }

    @Before("com.sequenceiq.cloudbreak.cloud.logger.LogContextAspects.interceptCloudPlatformEventHandlersAcceptMethod()")
    public void buildLogContextForCloudPlatformEventHandler(JoinPoint joinPoint) {
        Event<CloudPlatformRequest<?>> event = (Event<CloudPlatformRequest<?>>) joinPoint.getArgs()[0];
        CloudPlatformRequest<?> cloudPlatformRequest = event.getData();
        CloudContext cloudContext = cloudPlatformRequest.getCloudContext();
        buildMdcContext(cloudContext, event);
        LOGGER.debug("A CloudPlatformEventHandler's 'accept' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString());
    }

    @Before("com.sequenceiq.cloudbreak.cloud.logger.LogContextAspects.interceptResourcePersistenceHandlerAcceptMethod()")
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
            String requestId = eventMdcContext != null ? eventMdcContext.get(LoggerContextKey.REQUEST_ID.toString()) : null;
            MDCBuilder.addRequestIdToMdcContext(requestId);
            MDCBuilder.buildMdcContext(stringValue(cloudContext.getId()), stringValue(cloudContext.getName()), "STACK");
            MDCBuilder.buildUserAndTenantMdcContext(cloudContext.getUserId(), cloudContext.getAccountId());
            MDCBuilder.buildEnvironmentMdcContext(eventMdcContext != null ? eventMdcContext.get(LoggerContextKey.ENVIRONMENT_CRN.toString()) : null);
        } else {
            MDCBuilder.buildMdcContextFromMap(eventMdcContext);
        }
    }

    private String stringValue(Object object) {
        return object == null ? "" : object.toString();
    }
}
