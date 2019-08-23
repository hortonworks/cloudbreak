package com.sequenceiq.flow.logger;

import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import reactor.bus.Event;

@Component("FlowLogContextAspects")
@Aspect
public class LogContextAspects {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogContextAspects.class);

    @Pointcut("execution(public * com.sequenceiq.flow.reactor.api.handler.EventHandler+.accept(..))")
    public void interceptReactorHandlersAcceptMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.flow.core.Flow2Handler.accept(..))")
    public void interceptFlow2HandlerAcceptMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.flow.core.chain.FlowChainHandler.accept(..))")
    public void interceptFlowChainHandlerAcceptMethod() {
    }

    @Before("com.sequenceiq.flow.logger.LogContextAspects.interceptReactorHandlersAcceptMethod() ||"
            + "com.sequenceiq.flow.logger.LogContextAspects.interceptFlow2HandlerAcceptMethod() ||"
            + "com.sequenceiq.flow.logger.LogContextAspects.interceptFlowChainHandlerAcceptMethod()")
    public void buildLogContextForReactorHandler(JoinPoint joinPoint) {
        buildLogContextForEventHandler(joinPoint);
        LOGGER.debug("A Reactor event handler's 'accept' method has been intercepted: {}, MDC logger context is built.", joinPoint.toShortString());
    }

    public static void buildLogContextForEventHandler(JoinPoint joinPoint) {
        Event<?> event = (Event<?>) joinPoint.getArgs()[0];
        Map<String, String> mdcContextMap = event.getHeaders().get(MDCBuilder.MDC_CONTEXT_ID);
        if (mdcContextMap != null) {
            MDCBuilder.buildMdcContextFromMap(mdcContextMap);
            String requestId = mdcContextMap.get(LoggerContextKey.REQUEST_ID.toString());
            MDCBuilder.addRequestId(requestId);
        }
    }
}
