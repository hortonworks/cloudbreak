package com.sequenceiq.cloudbreak.service.altus;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

@Component
public class AltusAnonymizationRulesService {

    private final AltusIAMService altusIAMService;

    public AltusAnonymizationRulesService(AltusIAMService altusIAMService) {
        this.altusIAMService = altusIAMService;
    }

    public List<AnonymizationRule> getAnonymizationRules(Stack stack) {
        return altusIAMService.getAnonymizationRules(Crn.safeFromString(stack.getResourceCrn()).getAccountId(), stack.getCreator().getUserCrn());
    }
}
