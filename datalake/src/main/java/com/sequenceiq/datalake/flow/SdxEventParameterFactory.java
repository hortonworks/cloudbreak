package com.sequenceiq.datalake.flow;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;

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
        String userCrn = Optional.ofNullable(ThreadBasedUserCrnProvider.getUserCrn()).orElseThrow(() ->
                new InternalServerErrorException("There is no user present to perform the given operation."));
        return Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, userCrn);
    }
}