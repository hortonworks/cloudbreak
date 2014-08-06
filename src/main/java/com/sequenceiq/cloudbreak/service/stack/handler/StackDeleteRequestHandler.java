package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.event.StackDeleteRequest;

import reactor.event.Event;
import reactor.function.Consumer;

@Service
public class StackDeleteRequestHandler implements Consumer<Event<StackDeleteRequest>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackDeleteRequestHandler.class);

    @Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RetryingStackUpdater retryingStackUpdater;

    @Override
    public void accept(Event<StackDeleteRequest> stackDeleteRequest) {
        StackDeleteRequest data = stackDeleteRequest.getData();
        LOGGER.info("Accepted {} event.", ReactorConfig.DELETE_REQUEST_EVENT, data.getStackId());
        retryingStackUpdater.updateStackStatus(data.getStackId(), Status.DELETE_IN_PROGRESS);
        Stack oneWithLists = stackRepository.findOneWithLists(data.getStackId());
        try {
            cloudPlatformConnectors.get(data.getCloudPlatform()).deleteStack(oneWithLists.getUser(), oneWithLists, oneWithLists.getCredential());
        } catch (Exception ex) {
            retryingStackUpdater.updateStackStatus(data.getStackId(), Status.DELETE_FAILED);
        }

    }
}
