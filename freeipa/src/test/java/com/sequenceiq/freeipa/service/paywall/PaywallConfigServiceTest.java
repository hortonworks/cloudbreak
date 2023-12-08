package com.sequenceiq.freeipa.service.paywall;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.CMLicenseParser;
import com.sequenceiq.cloudbreak.auth.JsonCMLicense;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
public class PaywallConfigServiceTest {

    private static final String RESOURCE_CRN = "crn:cdp:iam:us-west-1:someworkspace:user:someuser";

    private static final String PAYWALL_KEY = "paywall";

    private static final String PAYWALL_SLS_PATH = "/paywall/init.sls";

    private static final String LICENCE_STRING = "licence string";

    private static final String PAYWALL_USERNAME = "paywall username";

    private static final String PAYWALL_PASSWORD = "paywall password";

    private static final String USERNAME_KEY = "username";

    private static final String PASSWORD_KEY = "password";

    @Mock
    private GrpcUmsClient umsClient;

    @Mock
    private CMLicenseParser cmLicenseParser;

    @Mock
    private Stack stack;

    @Mock
    private UserManagementProto.Account account;

    @Mock
    private JsonCMLicense license;

    @InjectMocks
    private PaywallConfigService victim;

    @BeforeEach
    public void initTest() {
        when(stack.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(umsClient.getAccountDetails(any())).thenReturn(account);
    }

    @Test
    public void shouldReturnEmptyConfigInCaseOfEmptyUser() {
        when(account.getClouderaManagerLicenseKey()).thenReturn(LICENCE_STRING);
        when(cmLicenseParser.parseLicense(LICENCE_STRING)).thenReturn(Optional.of(license));
        when(license.getPaywallUsername()).thenReturn(null);

        Map<String, SaltPillarProperties> actual = victim.createPaywallPillarConfig(stack);
        SaltPillarProperties actualSaltPillarProperties = actual.get(PAYWALL_KEY);
        Map<String, Object> actualProperties = actualSaltPillarProperties.getProperties();
        Map<String, String> actualPaywallProperties = (Map<String, String>) actualProperties.get(PAYWALL_KEY);

        assertThat(actualSaltPillarProperties.getPath(), equalTo(PAYWALL_SLS_PATH));
        assertThat(actualPaywallProperties, anEmptyMap());
    }

    @Test
    public void shouldReturnEmptyConfigInCaseOfEmptyPassword() {
        when(account.getClouderaManagerLicenseKey()).thenReturn(LICENCE_STRING);
        when(cmLicenseParser.parseLicense(LICENCE_STRING)).thenReturn(Optional.of(license));
        when(license.getPaywallUsername()).thenReturn(PAYWALL_USERNAME);
        when(license.getPaywallPassword()).thenReturn(null);

        Map<String, SaltPillarProperties> actual = victim.createPaywallPillarConfig(stack);
        SaltPillarProperties actualSaltPillarProperties = actual.get(PAYWALL_KEY);
        Map<String, Object> actualProperties = actualSaltPillarProperties.getProperties();
        Map<String, String> actualPaywallProperties = (Map<String, String>) actualProperties.get(PAYWALL_KEY);

        assertThat(actualSaltPillarProperties.getPath(), equalTo(PAYWALL_SLS_PATH));
        assertThat(actualPaywallProperties, anEmptyMap());
    }

    @Test
    public void shouldReturnConfigWithUsernameAndPassword() {
        when(account.getClouderaManagerLicenseKey()).thenReturn(LICENCE_STRING);
        when(cmLicenseParser.parseLicense(LICENCE_STRING)).thenReturn(Optional.of(license));
        when(license.getPaywallUsername()).thenReturn(PAYWALL_USERNAME);
        when(license.getPaywallPassword()).thenReturn(PAYWALL_PASSWORD);

        Map<String, SaltPillarProperties> actual = victim.createPaywallPillarConfig(stack);
        SaltPillarProperties actualSaltPillarProperties = actual.get(PAYWALL_KEY);
        Map<String, Object> actualProperties = actualSaltPillarProperties.getProperties();
        Map<String, String> actualPaywallProperties = (Map<String, String>) actualProperties.get(PAYWALL_KEY);

        assertThat(actualSaltPillarProperties.getPath(), equalTo(PAYWALL_SLS_PATH));
        assertThat(actualPaywallProperties.get(USERNAME_KEY), is(PAYWALL_USERNAME));
        assertThat(actualPaywallProperties.get(PASSWORD_KEY), is(PAYWALL_PASSWORD));
    }
}