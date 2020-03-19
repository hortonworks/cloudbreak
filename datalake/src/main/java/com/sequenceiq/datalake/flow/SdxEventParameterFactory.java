package com.sequenceiq.datalake.flow;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.core.EventParameterFactory;
import com.sequenceiq.flow.core.FlowConstants;

@Component
public class SdxEventParameterFactory implements EventParameterFactory {

    @Inject
    private SdxService sdxService;

    public Map<String, Object> createEventParameters(Long stackId) {
        String userCrn;
        try {
            userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        } catch (RuntimeException ex) {
            userCrn = sdxService.getById(stackId).getInitiatorUserCrn();
        }
        return Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
    }
}