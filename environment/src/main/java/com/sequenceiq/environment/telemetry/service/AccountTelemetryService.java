package com.sequenceiq.environment.telemetry.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.environment.configuration.telemetry.AccountTelemetryConfig;
import com.sequenceiq.environment.telemetry.domain.AccountTelemetry;
import com.sequenceiq.environment.telemetry.repository.AccountTelemetryRepository;

import jregex.Pattern;
import jregex.PatternSyntaxException;
import jregex.Replacer;

@Service
public class AccountTelemetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountTelemetryService.class);

    private final AccountTelemetryRepository accountTelemetryRepository;

    private final RegionAwareCrnGenerator regionAwareCrnGenerator;

    private final List<AnonymizationRule> defaultRules;

    public AccountTelemetryService(AccountTelemetryRepository accountTelemetryRepository, AccountTelemetryConfig accountTelemetryConfig,
            RegionAwareCrnGenerator regionAwareCrnGenerator) {
        this.accountTelemetryRepository = accountTelemetryRepository;
        this.defaultRules = accountTelemetryConfig.getRules();
        this.regionAwareCrnGenerator = regionAwareCrnGenerator;
    }

    public AccountTelemetry create(AccountTelemetry telemetry, String accountId) {
        try {
            validateAnonymizationRules(telemetry);
            LOGGER.debug("Creating account telemetry for account: {}", accountId);
            accountTelemetryRepository.archiveAll(accountId);
            String newCrn = createCRN(accountId);
            telemetry.setResourceCrn(newCrn);
            LOGGER.debug("New account telemetry crn for account '{}': {}", accountId, newCrn);
            telemetry.setAccountId(accountId);
            telemetry.setArchived(false);
            accountTelemetryRepository.save(telemetry);
            return telemetry;
        } catch (DataIntegrityViolationException e) {
            throw new AccessDeniedException("Access denied", e);
        }
    }

    public AccountTelemetry getOrDefault(String accountId) {
        try {
            LOGGER.debug("Get account telemetry for account: {}", accountId);
            Optional<AccountTelemetry> telemetry = accountTelemetryRepository.findByAccountId(accountId);
            return telemetry.orElse(createDefaultAccuontTelemetry());
        } catch (DataIntegrityViolationException e) {
            throw new AccessDeniedException("Access denied", e);
        }
    }

    public Features updateFeatures(String accountId, Features newFeatures) {
        try {
            LOGGER.debug("Update account telemetry features for account: {}", accountId);
            Optional<AccountTelemetry> telemetryOpt = accountTelemetryRepository.findByAccountId(accountId);
            AccountTelemetry telemetry = telemetryOpt.orElse(createDefaultAccuontTelemetry());
            Features features = telemetry.getFeatures();
            Features finalFeatures = null;
            if (features != null && newFeatures != null) {
                finalFeatures = new Features();
                finalFeatures.setClusterLogsCollection(features.getClusterLogsCollection());
                finalFeatures.setWorkloadAnalytics(features.getWorkloadAnalytics());
                finalFeatures.setMonitoring(features.getMonitoring());
                finalFeatures.setCloudStorageLogging(features.getCloudStorageLogging());
                if (newFeatures.getClusterLogsCollection() != null) {
                    LOGGER.debug("Account telemetry feature request contains log collection feature " +
                            "for account {} (set: {})", accountId, newFeatures.getClusterLogsCollection().isEnabled());
                    finalFeatures.setClusterLogsCollection(newFeatures.getClusterLogsCollection());
                }
                if (newFeatures.getWorkloadAnalytics() != null) {
                    LOGGER.debug("Account telemetry feature request contains workload analytics feature " +
                            "for account {} (set: {})", accountId, newFeatures.getWorkloadAnalytics().isEnabled());
                    finalFeatures.setWorkloadAnalytics(newFeatures.getWorkloadAnalytics());
                }
                if (newFeatures.getMonitoring() != null) {
                    LOGGER.debug("Account telemetry feature request contains monitoring feature " +
                            "for account {} (set: {})", accountId, newFeatures.getMonitoring().isEnabled());
                    finalFeatures.setMonitoring(newFeatures.getMonitoring());
                }
                if (newFeatures.getCloudStorageLogging() != null) {
                    LOGGER.debug("Account telemetry feature request contains cloud storage logging feature " +
                            "for account {} (set: {})", accountId, newFeatures.getCloudStorageLogging().isEnabled());
                    finalFeatures.setMonitoring(newFeatures.getCloudStorageLogging());
                }
            }
            telemetry.setFeatures(finalFeatures);
            return create(telemetry, accountId).getFeatures();
        } catch (DataIntegrityViolationException e) {
            throw new AccessDeniedException("Access denied", e);
        }
    }

    public AccountTelemetry createDefaultAccuontTelemetry() {
        AccountTelemetry defaultTelemetry = new AccountTelemetry();
        List<AnonymizationRule> defaultEncodedRules = defaultRules
                .stream()
                .map(rule -> {
                    AnonymizationRule encodedRule = new AnonymizationRule();
                    encodedRule.setValue(Base64.getEncoder().encodeToString(rule.getValue().getBytes()));
                    encodedRule.setReplacement(rule.getReplacement());
                    return encodedRule;
                }).collect(Collectors.toList());
        Features defaultFeatures = new Features();
        defaultFeatures.addClusterLogsCollection(false);
        defaultFeatures.addCloudStorageLogging(true);
        defaultTelemetry.setRules(defaultEncodedRules);
        defaultTelemetry.setFeatures(defaultFeatures);
        return defaultTelemetry;
    }

    public String testRulePatterns(List<AnonymizationRule> rules, String input) {
        String output = input;
        for (AnonymizationRule rule : rules) {
            String decodedRule = new String(Base64.getDecoder().decode(rule.getValue().getBytes()));
            Pattern p = createAndCheckPattern(decodedRule);
            Replacer replacer = p.replacer(rule.getReplacement());
            output = replacer.replace(output);
        }
        return output;
    }

    void validateAnonymizationRules(AccountTelemetry telemetry) {
        Optional.ofNullable(telemetry.getRules()).orElse(new ArrayList<>())
                .forEach(rule -> {
                    String decodedRule = new String(Base64.getDecoder().decode(rule.getValue().getBytes()));
                    createAndCheckPattern(decodedRule);
                });
    }

    private Pattern createAndCheckPattern(String decodedRule) {
        Pattern p;
        try {
            p = new Pattern(decodedRule);
        } catch (PatternSyntaxException e) {
            String errorMessage = String.format("Anonymization regex pattern is invalid: %s", decodedRule);
            LOGGER.debug(errorMessage, e);
            throw new BadRequestException(errorMessage);
        }
        return p;
    }

    private String createCRN(String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.ACCOUNT_TELEMETRY, accountId);
    }

}
