package com.sequenceiq.datalake.flow;

import java.util.Map;
import java.util.Optional;

import javax.ws.rs.InternalServerErrorException;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.flow.core.EventParameterFactory;
import com.sequenceiq.flow.core.FlowConstants;

@Component
public class SdxEventParameterFactory implements EventParameterFactory {

    public Map<String, Object> createEventParameters(Long stackId) {
        String userCrn  = Optional.ofNullable(ThreadBasedUserCrnProvider.getUserCrn())
                    .orElseThrow(() -> new InternalServerErrorException("User cannot be found to fulfill the request."));
        return Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
    }
}