package com.sequenceiq.environment.telemetry.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.common.api.telemetry.model.AnonymizationRule;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.environment.telemetry.domain.AccountTelemetry;
import com.sequenceiq.environment.telemetry.repository.AccountTelemetryRepository;

@Service
public class AccountTelemetryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountTelemetryService.class);

    private static final String EMAIL_PATTERN = "\\b([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-\\._]" +
            "*[A-Za-z0-9])@(([A-Za-z0-9]|[A-Za-z][A-Za-z0-9\\-]*[A-Za-z0-9])\\.)+([A-Za-z0-9]" +
            "|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])\\b";

    private static final String EMAIL_REPLACEMENT = "email@redacted.host";

    private static final String CREDIT_CARD_PATTERN = "\\d{4}[^\\w]\\d{4}[^\\w]\\d{4}[^\\w]\\d{4}";

    private static final String CREDIT_CARD_REPLACEMENT = "XXXX-XXXX-XXXX-XXXX";

    private static final String SSN_PATTERN = "\\d{3}[^\\w]\\d{2}[^\\w]\\d{4}";

    private static final String SSN_REPLACEMENT = "XXX-XX-XXXX";

    private final AccountTelemetryRepository accountTelemetryRepository;

    public AccountTelemetryService(AccountTelemetryRepository accountTelemetryRepository) {
        this.accountTelemetryRepository = accountTelemetryRepository;
    }

    public AccountTelemetry create(AccountTelemetry telemetry, String accountId) {
        try {
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
            }
            telemetry.setFeatures(finalFeatures);
            return create(telemetry, accountId).getFeatures();
        } catch (DataIntegrityViolationException e) {
            throw new AccessDeniedException("Access denied", e);
        }
    }

    public AccountTelemetry createDefaultAccuontTelemetry() {
        AccountTelemetry defaultTelemetry = new AccountTelemetry();
        List<AnonymizationRule> defaultRules = new ArrayList<>();

        AnonymizationRule creditCardWithSepRule = new AnonymizationRule();
        creditCardWithSepRule.setValue(
                Base64.getEncoder().encodeToString(CREDIT_CARD_PATTERN.getBytes()));
        creditCardWithSepRule.setReplacement(CREDIT_CARD_REPLACEMENT);

        AnonymizationRule ssnWithSepRule = new AnonymizationRule();
        ssnWithSepRule.setValue(
                Base64.getEncoder().encodeToString(SSN_PATTERN.getBytes()));
        ssnWithSepRule.setReplacement(SSN_REPLACEMENT);

        AnonymizationRule emailRule = new AnonymizationRule();
        emailRule.setValue(
                Base64.getEncoder().encodeToString(EMAIL_PATTERN.getBytes()));
        emailRule.setReplacement(EMAIL_REPLACEMENT);

        defaultRules.add(creditCardWithSepRule);
        defaultRules.add(ssnWithSepRule);
        defaultRules.add(emailRule);

        Features defaultFeatures = new Features();
        defaultFeatures.addClusterLogsCollection(false);
        defaultTelemetry.setRules(defaultRules);
        defaultTelemetry.setFeatures(defaultFeatures);
        return defaultTelemetry;
    }

    private String createCRN(String accountId) {
        return Crn.builder()
                .setService(Crn.Service.ACCOUNTTELEMETRY)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.ACCOUNT_TELEMETRY)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }
}
