package com.sequenceiq.cloudbreak.service.cluster.flow;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.UPDATE_IN_PROGRESS;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.SecurityRuleRepository;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderConnectorAdapter;

@Service
public class UpdateSubnetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateSubnetService.class);
    private static final String UPDATED_SUBNETS = "updated";
    private static final String REMOVED_SUBNETS = "removed";

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private ServiceProviderConnectorAdapter connector;

    @Inject
    private SecurityGroupService securityGroupService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private SecurityRuleRepository securityRuleRepository;

    public Map<String, Set<SecurityRule>> updateSubnet(Stack stack, List<SecurityRule> allowedSecurityRules) {
        LOGGER.info("Start updating allowed subnets");
        stackUpdater.updateStackStatus(stack.getId(), UPDATE_IN_PROGRESS, "Updating allowed subnets.");
        String message = messagesService.getMessage(Msg.STACK_INFRASTRUCTURE_SUBNETS_UPDATING.code());
        cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), message);
        Map<String, Set<SecurityRule>> modifiedSubnets = getModifiedSubnetList(stack, allowedSecurityRules);
        Set<SecurityRule> newSecurityRules = modifiedSubnets.get(UPDATED_SUBNETS);
        LOGGER.info("New security rules are: " + newSecurityRules);
        stack.getSecurityGroup().setSecurityRules(newSecurityRules);
        connector.updateAllowedSubnets(stack);
        return modifiedSubnets;
    }

    private Map<String, Set<SecurityRule>> getModifiedSubnetList(Stack stack, List<SecurityRule> securityRuleList) {
        Map<String, Set<SecurityRule>> result = new HashMap<>();
        Set<SecurityRule> removed = new HashSet<>();
        Set<SecurityRule> updated = new HashSet<>();
        Long securityGroupId = stack.getSecurityGroup().getId();
        Set<SecurityRule> securityRules = securityGroupService.get(securityGroupId).getSecurityRules();
        for (SecurityRule securityRule : securityRules) {
            if (!securityRule.isModifiable()) {
                updated.add(securityRule);
                removeFromNewSubnetList(securityRule, securityRuleList);
            } else {
                removed.add(securityRule);
            }
        }
        for (SecurityRule securityRule : securityRuleList) {
            updated.add(securityRule);
        }
        result.put(UPDATED_SUBNETS, updated);
        result.put(REMOVED_SUBNETS, removed);
        return result;
    }

    private void removeFromNewSubnetList(SecurityRule securityRule, List<SecurityRule> securityRuleList) {
        Iterator<SecurityRule> iterator = securityRuleList.iterator();
        String cidr = securityRule.getCidr();
        while (iterator.hasNext()) {
            SecurityRule next = iterator.next();
            if (next.getCidr().equals(cidr)) {
                iterator.remove();
            }
        }
    }

    public void finalizeUpdateSubnet(Stack stack, Map<String, Set<SecurityRule>> modifiedSubnets) {
        LOGGER.info("Finalizing updating allowed subnets");
        securityRuleRepository.delete(modifiedSubnets.get(REMOVED_SUBNETS));
        Set<SecurityRule> newSecurityRules = modifiedSubnets.get(UPDATED_SUBNETS);
        securityRuleRepository.save(newSecurityRules);
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Allowed subnets successfully updated.");
        String message = messagesService.getMessage(Msg.STACK_INFRASTRUCTURE_SUBNETS_UPDATED.code());
        cloudbreakEventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), message);
    }

    public void handlerUpdateSubnetFailure(Stack stack, Exception e) {
        SecurityGroup securityGroup = stack.getSecurityGroup();
        String msg = stack.isStackInDeletionPhase() ? "Failed to update security group with allowed subnets: %s; stack is already in deletion phase."
                : "Failed to update security group with allowed subnets: %s";
        LOGGER.error(String.format(msg, securityGroup));
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, String.format("Stack update failed. %s", e.getMessage()));
        String message = messagesService.getMessage(Msg.STACK_INFRASTRUCTURE_UPDATE_FAILED.code(), Collections.singletonList(e.getMessage()));
        cloudbreakEventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), message);
    }

}