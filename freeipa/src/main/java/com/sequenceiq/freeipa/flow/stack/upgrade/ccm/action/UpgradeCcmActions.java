package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.action;

import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmHandlerSelector.UPGRADE_CCM_CHECK_PREREQUISITES_EVENT;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmCheckPrerequisitesRequest;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmTriggerEvent;

@Configuration
public class UpgradeCcmActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmActions.class);

    @Bean(name = "UPGRADE_CCM_CHECK_PREREQUISITES_STATE")
    public Action<?, ?> checkPrerequisites() {
        return new AbstractUpgradeCcmAction<>(UpgradeCcmTriggerEvent.class) {
            @Override
            protected void doExecute(UpgradeCcmContext context, UpgradeCcmTriggerEvent payload, Map<Object, Object> variables) {
                setOperationId(variables, payload.getOperationId());
                // TODO update stack status & send notification
                LOGGER.info("Starting checking prerequisites for CCM upgrade {}", payload);
                sendEvent(context, UPGRADE_CCM_CHECK_PREREQUISITES_EVENT.event(), new UpgradeCcmCheckPrerequisitesRequest(context.getStack()));
            }
        };
    }

    // TODO state actions

    // TODO finalize; complete operation

    // TODO handle failure; fail operation

}
