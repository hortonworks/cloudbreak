package com.sequenceiq.flow.reactor;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.RestRequestThreadLocalService;
import com.sequenceiq.flow.core.FlowConstants;

import reactor.bus.Event;

@Component
@Aspect
public class FlowParametersAspects {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowParametersAspects.class);

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.cloud.handler.CloudPlatformEventHandler+.accept(..))")
    public void interceptCloudPlatformEventHandlersAcceptMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.flow.reactor.api.handler.EventHandler+.accept(..))")
    public void interceptReactorHandlersAcceptMethod() {
    }

    @Around("com.sequenceiq.flow.reactor.FlowParametersAspects.interceptReactorHandlersAcceptMethod() ||"
            + "com.sequenceiq.flow.reactor.FlowParametersAspects.interceptCloudPlatformEventHandlersAcceptMethod()")
    public Object setFlowTriggerUserCrnForReactorHandler(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String flowTriggerUserCrn = null;
        try {
            Event<?> event = (Event<?>) proceedingJoinPoint.getArgs()[0];
            flowTriggerUserCrn = event.getHeaders().get(FlowConstants.FLOW_TRIGGER_USERCRN);
            if (flowTriggerUserCrn != null) {
                restRequestThreadLocalService.setUserCrn(flowTriggerUserCrn);
                LOGGER.debug("A Reactor event handler's 'accept' method has been intercepted: {}, FlowTriggerUserCrn set to threadlocal.",
                        proceedingJoinPoint.toShortString());
            }
            return proceedingJoinPoint.proceed();
        } finally {
            if (flowTriggerUserCrn != null) {
                LOGGER.debug("FlowTriggerUserCrn remove from threadlocal on {}.",
                        proceedingJoinPoint.toShortString());
                restRequestThreadLocalService.removeUserCrn();
            }
        }
    }
}
