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

    private final Map<Key, Element> submittedEvaluators = new ConcurrentHashMap<>();

    boolean putIfAbsent(EvaluatorExecutor evaluator, long clusterOrStackId) {
        Element elementToSubmit = new Element(evaluator, clusterOrStackId);
        Element elementAlreadyPresent = submittedEvaluators.putIfAbsent(elementToSubmit.getKey(), elementToSubmit);
        if (elementAlreadyPresent == null) {
            return true;
        }

        if (clock.getCurrentTime() - elementAlreadyPresent.getTimestampMillis() > timeout) {
            LOGGER.info("timeout after {}: {} for cluster {} still present, resubmitting",
                    clock.getCurrentTime() - elementAlreadyPresent.getTimestampMillis(),
                    elementAlreadyPresent.getKey().getEvaluatorExecutor().getName(),
                    elementAlreadyPresent.getKey().getClusterId());
            return true;
        }

        LOGGER.info("submitting {} for cluster {} failed, it has been present in the system for {} ms",
                elementAlreadyPresent.getKey().getEvaluatorExecutor().getName(),
                elementAlreadyPresent.getKey().getClusterId(),
                clock.getCurrentTime() - elementAlreadyPresent.getTimestampMillis());
        return false;
    }

    public void remove(EvaluatorExecutor evaluator, long clusterId) {
        Key key = new Key(evaluator, clusterId);
        submittedEvaluators.remove(key);
    }

    private static class Key {
        private final EvaluatorExecutor evaluatorExecutor;

        private final long elementId;

        private Key(EvaluatorExecutor evaluatorExecutor, long elementId) {
            this.evaluatorExecutor = evaluatorExecutor;
            this.elementId = elementId;
        }

        private EvaluatorExecutor getEvaluatorExecutor() {
            return evaluatorExecutor;
        }

        private long getClusterId() {
            return elementId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(evaluatorExecutor.getName(), elementId);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || !getClass().equals(o.getClass())) {
                return false;
            }
            Key key = (Key) o;
            return elementId == key.elementId && Objects.equals(evaluatorExecutor.getName(), key.evaluatorExecutor.getName());
        }
    }

    private class Element {

        private final Key key;

        private final long timestampMillis;

        Element(EvaluatorExecutor evaluatorExecutor, long clusterId) {
            key = new Key(evaluatorExecutor, clusterId);
            timestampMillis = clock.getCurrentTime();
        }

        private Key getKey() {
            return key;
        }

        private long getTimestampMillis() {
            return timestampMillis;
        }
    }
}
