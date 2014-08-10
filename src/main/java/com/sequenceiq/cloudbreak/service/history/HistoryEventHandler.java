package com.sequenceiq.cloudbreak.service.history;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.HistoryEvent;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class HistoryEventHandler implements Consumer<Event<ProvisionEntity>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryEventHandler.class);

    @Autowired
    private HistoryService historyService;

    @Override
    public void accept(Event<ProvisionEntity> reactorEvent) {
        LOGGER.info("History event received: {}", reactorEvent);
        String historyEventStr = reactorEvent.getHeaders().get("history.event");
        HistoryEvent historyEvent = HistoryEvent.valueOf(historyEventStr);
        historyService.recordHistory(reactorEvent.getData(), historyEvent);

    }
}
