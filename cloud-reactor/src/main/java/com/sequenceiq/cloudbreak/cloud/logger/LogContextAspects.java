package com.sequenceiq.cloudbreak.cloud.logger;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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
import com.sequenceiq.cloudbreak.logger.MdcContext;

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
            String crn = getIfNotNull(eventMdcContext, c -> c.get(LoggerContextKey.CRN.toString()));
            if (StringUtils.isEmpty(crn)) {
                crn = getIfNotNull(eventMdcContext, c -> c.get(LoggerContextKey.RESOURCE_CRN.toString()));
            }
            MdcContext.builder()
                    .flowId(getIfNotNull(eventMdcContext, c -> c.get(LoggerContextKey.FLOW_ID.toString())))
                    .requestId(getIfNotNull(eventMdcContext, c -> c.get(LoggerContextKey.REQUEST_ID.toString())))
                    .resourceCrn(crn)
                    .resourceName(getIfNotNull(cloudContext.getName(), this::stringValue))
                    .resourceType(getIfNotNull(eventMdcContext, c -> c.get(LoggerContextKey.RESOURCE_TYPE.toString())))
                    .userCrn(getIfNotNull(eventMdcContext, c -> c.get(LoggerContextKey.USER_CRN.toString())))
                    .tenant(getIfNotNull(eventMdcContext, c -> c.get(LoggerContextKey.TENANT.toString())))
                    .environmentCrn(getIfNotNull(eventMdcContext, c -> c.get(LoggerContextKey.ENVIRONMENT_CRN.toString())))
                    .buildMdc();
        } else {
            MDCBuilder.buildMdcContextFromMap(eventMdcContext);
        }
    }

    private String stringValue(Object object) {
        return object == null ? "" : object.toString();
    }
}
