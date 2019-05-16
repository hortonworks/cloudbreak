package com.sequenceiq.flow.conf;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Configuration
public class EventHandlerConfiguration {

    @Bean
    @ConditionalOnBean(EventHandler.class)
    public EventHandlers eventHandlers(List<EventHandler<?>> eventHandlers) {
        return new EventHandlers(eventHandlers);
    }

    @Bean
    @ConditionalOnMissingBean(EventHandlers.class)
    public EventHandlers emptyEventHandlers() {
        return new EventHandlers(new ArrayList<>());
    }

    static class EventHandlers {
        private final List<EventHandler<?>> eventHandlers;

        EventHandlers(List<EventHandler<?>> eventHandlers) {
            this.eventHandlers = eventHandlers;
        }

        List<EventHandler<?>> getEventHandlers() {
            return eventHandlers;
        }
    }
}
