package com.sequenceiq.cloudbreak.core.flow2.chain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyproxy.ModifyProxyConfigEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.update.userdata.UpdateUserDataEvents;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.modifyproxy.ModifyProxyConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.userdata.UserDataUpdateRequest;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@ExtendWith(MockitoExtension.class)
class ModifyProxyConfigFlowEventChainFactoryTest {

    private static final long STACK_ID = 1L;

    private static final String PREVIOUS_PROXY_CONFIG_CRN = "prev-proxy-crn";

    private static final ModifyProxyConfigFlowChainTriggerEvent EVENT = new ModifyProxyConfigFlowChainTriggerEvent(STACK_ID, PREVIOUS_PROXY_CONFIG_CRN);

    @InjectMocks
    private ModifyProxyConfigFlowEventChainFactory underTest;

    @Test
    void createFlowTriggerEventQueue() {
        FlowTriggerEventQueue result = underTest.createFlowTriggerEventQueue(EVENT);

        assertThat(result)
                .returns("ModifyProxyConfigFlowEventChainFactory", FlowTriggerEventQueue::getFlowChainName)
                .returns(EVENT, FlowTriggerEventQueue::getTriggerEvent);

        assertThat(result.getQueue().poll())
                .isInstanceOf(StackEvent.class)
                .extracting(StackEvent.class::cast)
                .returns(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), StackEvent::selector)
                .returns(STACK_ID, StackEvent::getResourceId);

        assertThat(result.getQueue().poll())
                .isInstanceOf(ModifyProxyConfigRequest.class)
                .extracting(ModifyProxyConfigRequest.class::cast)
                .returns(ModifyProxyConfigEvent.MODIFY_PROXY_CONFIG_EVENT.event(), ModifyProxyConfigRequest::selector)
                .returns(STACK_ID, ModifyProxyConfigRequest::getResourceId)
                .returns(PREVIOUS_PROXY_CONFIG_CRN, ModifyProxyConfigRequest::getPreviousProxyConfigCrn);

        assertThat(result.getQueue().poll())
                .isInstanceOf(UserDataUpdateRequest.class)
                .extracting(UserDataUpdateRequest.class::cast)
                .returns(UpdateUserDataEvents.UPDATE_USERDATA_TRIGGER_EVENT.event(), UserDataUpdateRequest::selector)
                .returns(STACK_ID, UserDataUpdateRequest::getResourceId)
                .returns(true, UserDataUpdateRequest::isModifyProxyConfig)
                .returns(null, UserDataUpdateRequest::getOldTunnel);

        assertThat(result.getQueue()).isEmpty();
    }

}
