package com.sequenceiq.flow.reactor;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBaseUserCrnProvider;
import com.sequenceiq.flow.core.FlowConstants;

import reactor.bus.Event;

@Component
@Aspect
public class FlowParametersAspects {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowParametersAspects.class);

    @Inject
    private ThreadBaseUserCrnProvider threadBaseUserCrnProvider;

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
                threadBaseUserCrnProvider.setUserCrn(flowTriggerUserCrn);
                LOGGER.debug("A Reactor event handler's 'accept' method has been intercepted: {}, FlowTriggerUserCrn set to threadlocal.",
                        proceedingJoinPoint.toShortString());
            }
            return proceedingJoinPoint.proceed();
        } finally {
            if (flowTriggerUserCrn != null) {
                LOGGER.debug("FlowTriggerUserCrn remove from threadlocal on {}.",
                        proceedingJoinPoint.toShortString());
                threadBaseUserCrnProvider.removeUserCrn();
            }
        }
    }
}
