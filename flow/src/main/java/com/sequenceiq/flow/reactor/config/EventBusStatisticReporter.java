package com.sequenceiq.flow.reactor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.EventBus;

@Component
public class EventBusStatisticReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBusStatisticReporter.class);

    public void logInfoReport(EventBus eventbus) {
        LOGGER.info("Reactor event bus statistics: {}", create(eventbus));
    }

    public void logErrorReport(EventBus eventbus) {
        LOGGER.error("Reactor state is critical, statistics: {}", create(eventbus));
    }

    private EventBusStatistics create(EventBus eventBus) {
        EventBusStatistics stats = new EventBusStatistics();
        stats.setBackLogSize(eventBus.getDispatcher().backlogSize());
        stats.setRemainingSlots(eventBus.getDispatcher().remainingSlots());
        stats.setInContext(eventBus.getDispatcher().inContext());
        return stats;
    }


    public class EventBusStatistics {

        private long backLogSize;

        private long remainingSlots;

        private boolean inContext;

        public void setBackLogSize(long backLogSize) {
            this.backLogSize = backLogSize;
        }

        public void setRemainingSlots(long remainingSlots) {
            this.remainingSlots = remainingSlots;
        }

        public void setInContext(boolean inContext) {
            this.inContext = inContext;
        }

        @Override
        public String toString() {
            return "EventBusStatistics{" +
                    "backLogSize=" + backLogSize +
                    ", remainingSlots=" + remainingSlots +
                    ", inContext=" + inContext +
                    '}';
        }
    }

}
