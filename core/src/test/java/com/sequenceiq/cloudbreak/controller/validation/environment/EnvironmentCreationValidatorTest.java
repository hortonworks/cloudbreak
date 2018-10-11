package com.sequenceiq.cloudbreak.controller.validation.environment;

import static com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State.ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.service.platform.PlatformParameterService;

import reactor.fn.tuple.Tuple;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentCreationValidatorTest {

    @Mock
    private PlatformParameterService platformParameterService;

    @InjectMocks
    private EnvironmentCreationValidator environmentCreationValidator;

    @Test
    public void testValidation() {
        Credential credential = new Credential();

        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setName("ldap1");

        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setName("proxy1");

        RDSConfig rdsConfig1 = new RDSConfig();
        rdsConfig1.setName("rds1");
        RDSConfig rdsConfig2 = new RDSConfig();
        rdsConfig2.setName("rds2");

        Environment environment = new Environment();
        environment.setCredential(credential);
        environment.setLdapConfigs(Set.of(ldapConfig));
        environment.setProxyConfigs(Set.of(proxyConfig));
        environment.setRdsConfigs(Set.of(rdsConfig1, rdsConfig2));

        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setLdapConfigs(Set.of("ldap1", "ldap2"));
        environmentRequest.setProxyConfigs(Set.of("proxy1", "proxy2"));
        environmentRequest.setRdsConfigs(Set.of("rds1", "rds2", "rds3"));
        environmentRequest.setRegions(Set.of("region1", "region2", "region3"));

        CloudRegions cloudRegions = new CloudRegions();
        cloudRegions.setCloudRegions(Map.of(Region.region("region1"), List.of(), Region.region("region2"), List.of()));
        when(platformParameterService.getRegionsByCredential(any())).thenReturn(cloudRegions);

        ValidationResult result = environmentCreationValidator.validate(Tuple.of(environment, environmentRequest));

        assertEquals(ERROR, result.getState());
        assertEquals(4L, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("[ldap2]"));
        assertTrue(result.getErrors().get(1).contains("[proxy2]"));
        assertTrue(result.getErrors().get(2).contains("[rds3]"));
        assertTrue(result.getErrors().get(3).contains("[region3]"));
    }
}