package com.sequenceiq.cloudbreak.service.stack.handler.node;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.Provisioner;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.event.AddNodeRequest;

import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class AddNodeRequestHandler implements Consumer<Event<AddNodeRequest>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddNodeRequestHandler.class);

    @Autowired
    private StackRepository stackRepository;

    @Resource
    private Map<CloudPlatform, Provisioner> provisioners;

    @Autowired
    private UserDataBuilder userDataBuilder;

    @Override
    public void accept(Event<AddNodeRequest> event) {
        AddNodeRequest request = event.getData();
        CloudPlatform cloudPlatform = request.getCloudPlatform();
        Long stackId = request.getStackId();
        Stack one = stackRepository.findOneWithLists(stackId);
        LOGGER.info("Accepted {} event.", ReactorConfig.ADD_NODE_REQUEST_EVENT, stackId);
        provisioners.get(cloudPlatform)
                .addNode(one, userDataBuilder.build(cloudPlatform, one.getHash(), new HashMap<String, String>()));
    }
}
