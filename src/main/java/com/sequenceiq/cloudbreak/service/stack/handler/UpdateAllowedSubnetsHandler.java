package com.sequenceiq.cloudbreak.service.stack.handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Subnet;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.UserDataBuilder;
import com.sequenceiq.cloudbreak.service.stack.event.StackOperationFailure;
import com.sequenceiq.cloudbreak.service.stack.event.StackUpdateSuccess;
import com.sequenceiq.cloudbreak.service.stack.event.UpdateAllowedSubnetsRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.UpdateContextObject;

import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class UpdateAllowedSubnetsHandler implements Consumer<Event<UpdateAllowedSubnetsRequest>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAllowedSubnetsHandler.class);

    @Autowired
    private StackRepository stackRepository;

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> networkResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @Autowired
    private UserDataBuilder userDataBuilder;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Autowired
    private Reactor reactor;

    @Override
    public void accept(Event<UpdateAllowedSubnetsRequest> event) {
        UpdateAllowedSubnetsRequest request = event.getData();
        Stack stack = stackRepository.findOneWithLists(request.getStackId());
        MDCBuilder.buildMdcContext(stack);
        String userData = userDataBuilder.build(stack.cloudPlatform(), stack.getHash(), stack.getConsulServers(), new HashMap<String, String>());
        try {
            LOGGER.info("Accepted {} event on stack.", ReactorConfig.UPDATE_SUBNET_REQUEST_EVENT);
            stack.setAllowedSubnets(getNewSubnetList(stack, request.getAllowedSubnets()));
            if (stack.isCloudPlatformUsedWithTemplate()) {
                cloudPlatformConnectors.get(stack.cloudPlatform()).updateAllowedSubnets(stack, userData);
            } else {
                CloudPlatform cloudPlatform = stack.cloudPlatform();
                UpdateContextObject updateContext = resourceBuilderInits.get(cloudPlatform).updateInit(stack);
                for (ResourceBuilder resourceBuilder : networkResourceBuilders.get(cloudPlatform)) {
                    resourceBuilder.update(updateContext);
                }
            }
            stackUpdater.updateStack(stack);
            LOGGER.info("Publishing {} event.", ReactorConfig.UPDATE_SUBNET_COMPLETE_EVENT);
            reactor.notify(ReactorConfig.UPDATE_SUBNET_COMPLETE_EVENT,
                    Event.wrap(new StackUpdateSuccess(stack.getId(), false, null, null))
            );
        } catch (Exception e) {
            Stack tempStack = stackRepository.findById(stack.getId());
            if (!tempStack.isStackInDeletionPhase()) {
                String msg = String.format("Failed to update security constraints with allowed subnets: %s", stack.getAllowedSubnets());
                LOGGER.error(msg, e);
                notifyUpdateFailed(stack, e.getCause().getMessage());
            }
        }
    }

    private Set<Subnet> getNewSubnetList(Stack stack, List<Subnet> subnetList) {
        Set<Subnet> copy = new HashSet<>();
        for (Subnet subnet : stack.getAllowedSubnets()) {
            if (!subnet.isModifiable()) {
                copy.add(subnet);
                removeFromNewSubnetList(subnet, subnetList);
            }
        }
        for (Subnet subnet : subnetList) {
            copy.add(subnet);
        }
        return copy;
    }

    private void removeFromNewSubnetList(Subnet subnet, List<Subnet> subnetList) {
        Iterator<Subnet> iterator = subnetList.iterator();
        String cidr = subnet.getCidr();
        while (iterator.hasNext()) {
            Subnet next = iterator.next();
            if (next.getCidr().equals(cidr)) {
                iterator.remove();
            }
        }
    }

    private void notifyUpdateFailed(Stack stack, String detailedMessage) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Publishing {} event.", ReactorConfig.STACK_UPDATE_FAILED_EVENT);
        reactor.notify(ReactorConfig.STACK_UPDATE_FAILED_EVENT, Event.wrap(new StackOperationFailure(stack.getId(), detailedMessage)));
    }

}
