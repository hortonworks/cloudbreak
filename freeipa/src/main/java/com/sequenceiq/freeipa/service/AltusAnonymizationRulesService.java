package com.sequenceiq.freeipa.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class AltusAnonymizationRulesService {

    private final CrnService crnService;

    private final AltusIAMService altusIAMService;

    public AltusAnonymizationRulesService(CrnService crnService, AltusIAMService altusIAMService) {
        this.crnService = crnService;
        this.altusIAMService = altusIAMService;
    }

    public List<AnonymizationRule> getAnonymizationRules() {
        return altusIAMService.getAnonymizationRules(crnService.getCurrentAccountId(), crnService.getUserCrn());
    }
}
