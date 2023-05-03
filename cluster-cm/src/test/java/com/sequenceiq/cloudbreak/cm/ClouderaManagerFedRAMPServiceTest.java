package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cm.ClouderaManagerFedRAMPService.INTERMEDIATE2018;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerFedRAMPService.JAVA_EXCLUDE_INTERMEDIATE2018;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerFedRAMPService.JAVA_INTERMEDIATE2018;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerFedRAMPService.LOGIN_BANNER;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerFedRAMPService.OPENSSL_INTERMEDIATE2018;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerFedRAMPService.POLICY_DESCRIPTION;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerFedRAMPService.POLICY_NAME;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerFedRAMPService.POLICY_VERSION;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerFedRAMPService.TLS_CIPHERS_LIST_OPENSSL;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerFedRAMPService.TLS_CIPHER_SUITE;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerFedRAMPService.TLS_CIPHER_SUITE_JAVA;
import static com.sequenceiq.cloudbreak.cm.ClouderaManagerFedRAMPService.TLS_CIPHER_SUITE_JAVA_EXCLUDE;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.model.ApiConfigEnforcement;
import com.cloudera.api.swagger.model.ApiConfigPolicy;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerFedRAMPServiceTest {

    @InjectMocks
    private ClouderaManagerFedRAMPService underTest;

    @Test
    public void testGetApiConfigPolicyWhenEverythingWorksShouldReturnWithFivePolicy() {
        ApiConfigPolicy apiConfigPolicy = underTest.getApiConfigPolicy();

        Assertions.assertEquals(apiConfigPolicy.getVersion(), POLICY_VERSION);
        Assertions.assertEquals(apiConfigPolicy.getName(), POLICY_NAME);
        Assertions.assertEquals(apiConfigPolicy.getDescription(), POLICY_DESCRIPTION);
        Assertions.assertEquals(apiConfigPolicy.getConfigEnforcements().size(), 5);
        Assertions.assertEquals(apiConfigPolicy.getConfigEnforcements()
                        .stream()
                        .map(ApiConfigEnforcement::getLabel)
                        .collect(Collectors.toSet()),
                Set.of(TLS_CIPHER_SUITE_JAVA_EXCLUDE, LOGIN_BANNER, TLS_CIPHER_SUITE, TLS_CIPHER_SUITE_JAVA, TLS_CIPHERS_LIST_OPENSSL));
        Assertions.assertEquals(apiConfigPolicy.getConfigEnforcements()
                        .stream()
                        .filter(s -> s.getLabel().equals(TLS_CIPHER_SUITE_JAVA_EXCLUDE))
                        .map(ApiConfigEnforcement::getDefaultValue)
                        .findFirst()
                        .get(),
                JAVA_EXCLUDE_INTERMEDIATE2018);
        Assertions.assertEquals(apiConfigPolicy.getConfigEnforcements()
                        .stream()
                        .filter(s -> s.getLabel().equals(TLS_CIPHER_SUITE))
                        .map(ApiConfigEnforcement::getDefaultValue)
                        .findFirst()
                        .get(),
                INTERMEDIATE2018);
        Assertions.assertEquals(apiConfigPolicy.getConfigEnforcements()
                        .stream()
                        .filter(s -> s.getLabel().equals(TLS_CIPHER_SUITE_JAVA))
                        .map(ApiConfigEnforcement::getDefaultValue)
                        .findFirst()
                        .get(),
                JAVA_INTERMEDIATE2018);
        Assertions.assertEquals(apiConfigPolicy.getConfigEnforcements()
                        .stream()
                        .filter(s -> s.getLabel().equals(TLS_CIPHERS_LIST_OPENSSL))
                        .map(ApiConfigEnforcement::getDefaultValue)
                        .findFirst()
                        .get(),
                OPENSSL_INTERMEDIATE2018);
    }

}