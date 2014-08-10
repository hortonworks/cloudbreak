package com.sequenceiq.cloudbreak.service.history;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.AbstractHistory;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintHistory;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ClusterHistory;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.CredentialHistory;
import com.sequenceiq.cloudbreak.domain.HistoryEvent;
import com.sequenceiq.cloudbreak.domain.ProvisionEntity;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackHistory;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.TemplateHistory;
import com.sequenceiq.cloudbreak.repository.BlueprintHistoryRepository;
import com.sequenceiq.cloudbreak.repository.ClusterHistoryRepository;
import com.sequenceiq.cloudbreak.repository.CredentialHistoryRepository;
import com.sequenceiq.cloudbreak.repository.StackHistoryRepository;
import com.sequenceiq.cloudbreak.repository.TemplateHistoryRepository;
import com.sequenceiq.cloudbreak.service.history.converter.HistoryConverter;

import reactor.core.Reactor;
import reactor.event.Event;

@Service
public class DefaultHistoryService implements HistoryService<ProvisionEntity> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHistoryService.class);
    private static final List<Class<? extends ProvisionEntity>> SUPPORTED_ENTITIES = Arrays.asList(
            Cluster.class, Stack.class, Blueprint.class, AzureTemplate.class, AwsTemplate.class, AzureCredential.class, AwsCredential.class);

    @Autowired
    private List<HistoryConverter> converters = new ArrayList<>();

    @Autowired
    private Reactor reactor;

    @Autowired
    private ClusterHistoryRepository clusterHistoryRepository;

    @Autowired
    private StackHistoryRepository stackHistoryRepository;

    @Autowired
    private BlueprintHistoryRepository blueprintHistoryRepository;

    @Autowired
    private CredentialHistoryRepository credentialHistoryRepository;

    @Autowired
    private TemplateHistoryRepository templateHistoryRepository;

    @Override
    public void notify(ProvisionEntity entity, HistoryEvent historyEvent) {
        Event reactorEvent = Event.wrap(entity);
        reactorEvent.getHeaders().set("history.event", historyEvent.name());
        if (isEntitySupported(entity)) {
            LOGGER.debug("Notifying history service. Event: {}, Entity type: {}", historyEvent, entity.getClass());
            reactor.notify(ReactorConfig.HISTORY_EVENT, reactorEvent);
        } else {
            LOGGER.debug("Ignoring history event. Event: {}, Entity type: {}", historyEvent, entity.getClass());
        }
    }

    @Override
    public void recordHistory(ProvisionEntity entity, HistoryEvent historyEvent) {
        LOGGER.debug("Recording history for entity: {}, event: {}", entity, historyEvent.name());

        ProvisionEntity history = convert(entity);
        ((AbstractHistory) history).setEventType(historyEvent);
        ((AbstractHistory) history).setEventTimestamp(Calendar.getInstance().getTime().getTime());

        if (entity instanceof Cluster) {
            clusterHistoryRepository.save((ClusterHistory) history);
        } else if (entity instanceof Blueprint) {
            blueprintHistoryRepository.save((BlueprintHistory) history);
        } else if (entity instanceof Template) {
            templateHistoryRepository.save((TemplateHistory) history);
        } else if (entity instanceof Stack) {
            stackHistoryRepository.save((StackHistory) history);
        } else if (entity instanceof Credential) {
            credentialHistoryRepository.save((CredentialHistory) history);
        }
    }

    @Override
    public boolean isEntitySupported(ProvisionEntity entity) {
        boolean ret = SUPPORTED_ENTITIES.contains(entity.getClass());
        LOGGER.debug("Entity {} supported. EntityClass: {}", ret ? "is" : "is not", entity.getClass());
        return ret;
    }

    @Override
    public HistoryEvent getEventType(ProvisionEntity entity) {
        HistoryEvent historyEvent = HistoryEvent.IGNORE;
        if (isEntitySupported(entity)) {
            Long id = null;
            if (entity instanceof Cluster) {
                id = ((Cluster) entity).getId();
            } else if (entity instanceof Blueprint) {
                id = ((Blueprint) entity).getId();
            } else if (entity instanceof Stack) {
                id = ((Stack) entity).getId();
            } else if (entity instanceof Template) {
                id = ((Template) entity).getId();
            } else if (entity instanceof Credential) {
                id = ((Credential) entity).getId();
            }
            historyEvent = (id != null) ? HistoryEvent.UPDATED : HistoryEvent.CREATED;
        }
        return historyEvent;
    }

    private ProvisionEntity convert(ProvisionEntity entity) {
        ProvisionEntity history = null;
        for (HistoryConverter converter : converters) {
            if (converter.supportsEntity(entity)) {
                history = converter.convert(entity);
                LOGGER.debug("Converter {} : entity type: {}", converter.getClass(), entity.getClass());
                break;
            }
        }
        if (null == history) {
            throw new UnsupportedOperationException("Entity conversion not supported");
        }
        return history;
    }
}
