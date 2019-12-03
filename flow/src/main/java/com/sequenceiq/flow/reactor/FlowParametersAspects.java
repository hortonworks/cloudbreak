package com.sequenceiq.flow.reactor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.core.FlowConstants;

import reactor.bus.Event;

@Component
@Aspect
public class FlowParametersAspects {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowParametersAspects.class);

    @Pointcut("execution(public * reactor.fn.Consumer+.accept(..)) && within(com.sequenceiq..*)")
    public void interceptReactorConsumersAcceptMethod() {
    }

    @Around("com.sequenceiq.flow.reactor.FlowParametersAspects.interceptReactorConsumersAcceptMethod()")
    public Object setFlowTriggerUserCrnForReactorHandler(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String flowTriggerUserCrn = null;
        try {
            Event<?> event = (Event<?>) proceedingJoinPoint.getArgs()[0];
            flowTriggerUserCrn = event.getHeaders().get(FlowConstants.FLOW_TRIGGER_USERCRN);
            if (flowTriggerUserCrn != null) {
                ThreadBasedUserCrnProvider.setUserCrn(flowTriggerUserCrn);
                try {
                    MDCBuilder.buildMdcContextFromCrn(Crn.fromString(flowTriggerUserCrn));
                } catch (Exception e) {
                    LOGGER.debug("Couldn't set MDCContext from crn: [{}]", flowTriggerUserCrn, e);
                }
                LOGGER.debug("A Reactor event handler's 'accept' method has been intercepted: {}, FlowTriggerUserCrn set to threadlocal.",
                        proceedingJoinPoint.toShortString());
            }
            return proceedingJoinPoint.proceed();
        } finally {
            if (flowTriggerUserCrn != null) {
                LOGGER.debug("FlowTriggerUserCrn remove from threadlocal on {}.",
                        proceedingJoinPoint.toShortString());
                ThreadBasedUserCrnProvider.removeUserCrn();
            }
        }
    }
}
