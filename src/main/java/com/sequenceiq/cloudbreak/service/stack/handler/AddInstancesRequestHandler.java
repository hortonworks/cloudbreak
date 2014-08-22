package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.event.Event;
import reactor.function.Consumer;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.Provisioner;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.event.AddInstancesRequest;

@Component
public class AddInstancesRequestHandler implements Consumer<Event<AddInstancesRequest>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddInstancesRequestHandler.class);

    @Autowired
    private StackRepository stackRepository;

    @Resource
    private Map<CloudPlatform, Provisioner> provisioners;

    @Autowired
    private UserDataBuilder userDataBuilder;

    @Override
    public void accept(Event<AddInstancesRequest> event) {
        AddInstancesRequest request = event.getData();
        CloudPlatform cloudPlatform = request.getCloudPlatform();
        Long stackId = request.getStackId();
        Integer scalingAdjustment = request.getScalingAdjustment();
        try {
            Stack one = stackRepository.findOneWithLists(stackId);
            LOGGER.info("Accepted {} event on stack: '{}'", ReactorConfig.ADD_INSTANCES_REQUEST_EVENT, stackId);
            provisioners.get(cloudPlatform)
                    .addNode(one, userDataBuilder.build(cloudPlatform, one.getHash(), new HashMap<String, String>()), scalingAdjustment);
        } catch (Exception e) {
            LOGGER.error("Unhandled exception occured while creating stack.", e);
            // TODO: should we do something else? websocket nofitication, status
            // to available/update failed? update statusReason?
        }
    }
}
