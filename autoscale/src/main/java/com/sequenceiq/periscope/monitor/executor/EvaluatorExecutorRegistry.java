package com.sequenceiq.periscope.monitor.executor;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;

@Service
public class EvaluatorExecutorRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluatorExecutorRegistry.class);

    @Value("${periscope.monitor.exeutor.registry.timeout:60000}")
    private long timeout;

    @Inject
    private Clock clock;

    private final Map<Element, Element> submittedEvaluators = new ConcurrentHashMap<>();

    boolean putIfAbsent(EvaluatorExecutor evaluator, long resourceId) {
        long now = clock.getCurrentTimeMillis();
        Element elementToSubmit = new Element(evaluator, resourceId, now);
        Element elementAlreadyPresent = submittedEvaluators.putIfAbsent(elementToSubmit, elementToSubmit);

        if (elementAlreadyPresent == null) {
            return true;
        } else if (now - elementAlreadyPresent.getTimestampMillis() > timeout) {
            LOGGER.info("Timeout after {}: {} for cluster {} still present, resubmitting",
                    now - elementAlreadyPresent.getTimestampMillis(),
                    elementAlreadyPresent.getEvaluatorExecutor().getName(),
                    elementAlreadyPresent.getResourceId());
            return true;
        }

        LOGGER.debug("Submitting {} for cluster {} failed, it has been present in the system for {} ms",
                elementAlreadyPresent.getEvaluatorExecutor().getName(),
                elementAlreadyPresent.getResourceId(),
                now - elementAlreadyPresent.getTimestampMillis());
        return false;
    }

    public void remove(EvaluatorExecutor evaluator, long clusterId) {
        Element element = new Element(evaluator, clusterId);
        submittedEvaluators.remove(element);
    }

    public int activeCount() {
        return submittedEvaluators.size();
    }

    private static class Element {

        private final EvaluatorExecutor evaluatorExecutor;

        private final long resourceId;

        private final long timestampMillis;

        Element(EvaluatorExecutor evaluatorExecutor, long resourceId) {
            this(evaluatorExecutor, resourceId, -1);
        }

        Element(EvaluatorExecutor evaluatorExecutor, long resourceId, long timestampMillis) {
            this.evaluatorExecutor = evaluatorExecutor;
            this.resourceId = resourceId;
            this.timestampMillis = timestampMillis;
        }

        EvaluatorExecutor getEvaluatorExecutor() {
            return evaluatorExecutor;
        }

        long getResourceId() {
            return resourceId;
        }

        long getTimestampMillis() {
            return timestampMillis;
        }

        @Override
        public int hashCode() {
            return Objects.hash(resourceId, evaluatorExecutor.getName());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || !getClass().equals(o.getClass())) {
                return false;
            }
            Element element = (Element) o;
            return resourceId == element.resourceId && Objects.equals(evaluatorExecutor.getName(), element.evaluatorExecutor.getName());
        }
    }
}
