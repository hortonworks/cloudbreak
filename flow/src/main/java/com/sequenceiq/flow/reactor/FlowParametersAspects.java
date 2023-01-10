package com.sequenceiq.flow.reactor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowConstants;

@Component
@Aspect
public class FlowParametersAspects {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowParametersAspects.class);

    @Pointcut("execution(public * java.util.function.Consumer+.accept(..)) && args(com.sequenceiq.cloudbreak.eventbus.Event) && within(com.sequenceiq..*)")
    public void interceptReactorConsumersAcceptMethod() {
    }

    @Around("com.sequenceiq.flow.reactor.FlowParametersAspects.interceptReactorConsumersAcceptMethod()")
    public Object setFlowTriggerUserCrnForReactorHandler(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Event<?> event = (Event<?>) proceedingJoinPoint.getArgs()[0];
        String flowTriggerUserCrn = event.getHeaders().get(FlowConstants.FLOW_TRIGGER_USERCRN);
        return ThreadBasedUserCrnProvider.doAsAndThrow(flowTriggerUserCrn, () -> doProceed(proceedingJoinPoint, flowTriggerUserCrn, event));
    }

    private Object doProceed(ProceedingJoinPoint proceedingJoinPoint, String flowTriggerUserCrn, Event<?> event) throws Throwable {
        if (flowTriggerUserCrn != null) {
            try {
                MDCBuilder.buildMdcContextFromCrn(Crn.fromString(flowTriggerUserCrn));
            } catch (Exception e) {
                LOGGER.debug("Couldn't set MDCContext from crn: [{}]", flowTriggerUserCrn, e);
            }
        }
        LOGGER.debug("A Reactor '{}' handler's '{}' has been intercepted: {}, user crn on thread local is: {}",
                event.getKey(), proceedingJoinPoint.toShortString(), event.getData(), flowTriggerUserCrn);
        return proceedingJoinPoint.proceed();
    }
}
