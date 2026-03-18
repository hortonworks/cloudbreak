package com.sequenceiq.cloudbreak.cloud.azure.conf;

import java.util.Map;
import java.util.function.Function;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class MdcReactorSchedulerDecorator implements Function<Runnable, Runnable> {

    @Override
    public Runnable apply(Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> originalContext = MDC.getCopyOfContextMap();
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            } else {
                MDC.clear();
            }
            try {
                runnable.run();
            } finally {
                if (originalContext != null) {
                    MDC.setContextMap(originalContext);
                } else {
                    MDC.clear();
                }
            }
        };
    }
}
