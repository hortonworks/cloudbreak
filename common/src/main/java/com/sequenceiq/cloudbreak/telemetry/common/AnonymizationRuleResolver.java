package com.sequenceiq.cloudbreak.telemetry.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;

@Service
public class AnonymizationRuleResolver {

    public List<AnonymizationRule> decodeRules(List<AnonymizationRule> rules) {
        return Optional.ofNullable(rules)
                .orElse(new ArrayList<>())
                .stream()
                .filter(rule -> StringUtils.isNotBlank(rule.getValue()))
                .map(rule -> {
                    AnonymizationRule newRule = new AnonymizationRule();
                    newRule.setReplacement(rule.getReplacement());
                    newRule.setValue(Base64Util.decode(rule.getValue()));
                    return newRule;
                })
                .collect(Collectors.toList());
    }
}
