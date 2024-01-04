package com.sequenceiq.cloudbreak.service.paywall;

import static java.util.Collections.singletonMap;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.CMLicenseParser;
import com.sequenceiq.cloudbreak.auth.JsonCMLicense;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

@Service
public class PaywallConfigService {
    private static final String PAYWALL_KEY = "paywall";

    private static final String PAYWALL_SLS_PATH = "/paywall/init.sls";

    private static final String USERNAME_KEY = "username";

    private static final String PASSWORD_KEY = "password";

    private static final Logger LOGGER = LoggerFactory.getLogger(PaywallConfigService.class);

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private CMLicenseParser cmLicenseParser;

    public Map<String, SaltPillarProperties> createPaywallPillarConfig(StackDto stackDto) {
        String accountId = Crn.safeFromString(stackDto.getResourceCrn()).getAccountId();
        UserManagementProto.Account account = umsClient.getAccountDetails(accountId);
        Optional<JsonCMLicense> license = Optional.of(account.getClouderaManagerLicenseKey())
                .flatMap(cmLicenseParser::parseLicense);

        Map<String, String> properties = license.map(this::getProperties).orElse(Map.of());
        return Map.of(PAYWALL_KEY, new SaltPillarProperties(PAYWALL_SLS_PATH, singletonMap(PAYWALL_KEY, properties)));
    }

    private Map<String, String> getProperties(JsonCMLicense license) {
        LOGGER.debug("CM license is available");
        String username = license.getPaywallUsername();
        String password = license.getPaywallPassword();
        if (isNotEmpty(username) && isNotEmpty(password)) {
            LOGGER.debug("Populate CM licence paywall username and password");
            return Map.of(USERNAME_KEY, username, PASSWORD_KEY, password);
        } else {
            LOGGER.debug("CM license paywall username or password is empty");
            return Map.of();
        }
    }
}
