package com.sequenceiq.freeipa.flow.chain;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.FlowState;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.stack.AbstractStackAction;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

public abstract class AbstractCommonChainAction<S extends FlowState, E extends FlowEvent, C extends CommonContext, P extends Payload>
        extends AbstractStackAction<S, E, C, P> {
    protected static final String INSTANCE_IDS = "INSTANCE_IDS";

    protected static final String OPERATION_ID = "OPERATION_ID";

    protected static final String DOWNSCALE_HOSTS = "DOWNSCALE_HOSTS";

    protected static final String UPSCALE_HOSTS = "UPSCALE_HOSTS";

    protected static final String REPAIR = "REPAIR";

    protected static final String INSTANCE_COUNT_BY_GROUP = "INSTANCE_COUNT_BY_GROUP";

    protected static final String FINAL_CHAIN = "FINAL_CHAIN";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCommonChainAction.class);

    @Inject
    private FreeipaJobService jobService;

    protected AbstractCommonChainAction(Class<P> payloadClass) {
        super(payloadClass);
    }

    protected void setInstanceIds(Map<Object, Object> variables, List<String> instanceIds) {
        variables.put(INSTANCE_IDS, instanceIds);
    }

    protected List<String> getInstanceIds(Map<Object, Object> variables) {
        return (List<String>) variables.get(INSTANCE_IDS);
    }

    protected void setOperationId(Map<Object, Object> variables, String operationId) {
        variables.put(OPERATION_ID, operationId);
    }

    protected String getOperationId(Map<Object, Object> variables) {
        return (String) variables.get(OPERATION_ID);
    }

    protected void setDownscaleHosts(Map<Object, Object> variables, List<String> hosts) {
        variables.put(DOWNSCALE_HOSTS, hosts);
    }

    protected List<String> getDownscaleHosts(Map<Object, Object> variables) {
        return (List<String>) variables.get(DOWNSCALE_HOSTS);
    }

    protected void setUpscaleHosts(Map<Object, Object> variables, List<String> hosts) {
        variables.put(UPSCALE_HOSTS, hosts);
    }

    protected List<String> getUpscaleHosts(Map<Object, Object> variables) {
        return (List<String>) variables.get(UPSCALE_HOSTS);
    }

    protected void setRepair(Map<Object, Object> variables, Boolean repair) {
        variables.put(REPAIR, repair);
    }

    protected Boolean isRepair(Map<Object, Object> variable) {
        return (Boolean) variable.get(REPAIR);
    }

    protected void setInstanceCountByGroup(Map<Object, Object> variables, Integer instanceCountByGroup) {
        variables.put(INSTANCE_COUNT_BY_GROUP, instanceCountByGroup);
    }

    protected Integer getInstanceCountByGroup(Map<Object, Object> variables) {
        return (Integer) variables.get(INSTANCE_COUNT_BY_GROUP);
    }

    protected void setFinalChain(Map<Object, Object> variables, Boolean finalChain) {
        variables.put(FINAL_CHAIN, finalChain);
    }

    protected Boolean isFinalChain(Map<Object, Object> variable) {
        return (Boolean) variable.get(FINAL_CHAIN);
    }

    protected void enableStatusChecker(Stack stack, String reason) {
        LOGGER.info("Enabling the status checker for stack ID {}. {}", stack.getId(), reason);
        jobService.schedule(stack);
    }

    protected void disableStatusChecker(Stack stack, String reason) {
        LOGGER.info("Disabling the status checker for stack ID {}. {}", stack.getId(), reason);
        jobService.unschedule(stack);
    }
}
