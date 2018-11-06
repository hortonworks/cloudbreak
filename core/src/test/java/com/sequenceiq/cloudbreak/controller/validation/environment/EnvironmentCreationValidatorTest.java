package com.sequenceiq.cloudbreak.controller.validation.environment;

import static com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State.ERROR;
import static com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State.VALID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.LocationRequest;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.environment.Region;
import com.sequenceiq.cloudbreak.domain.json.Json;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentCreationValidatorTest {
    @InjectMocks
    private EnvironmentCreationValidator environmentCreationValidator;

    @Test
    public void testValidationWithMultipleErrors() throws IOException {
        Credential credential = new Credential();

        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setId(0L);
        ldapConfig.setName("ldap1");

        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setId(0L);
        proxyConfig.setName("proxy1");

        RDSConfig rdsConfig1 = new RDSConfig();
        rdsConfig1.setId(0L);
        rdsConfig1.setName("rds1");
        RDSConfig rdsConfig2 = new RDSConfig();
        rdsConfig1.setId(1L);
        rdsConfig2.setName("rds2");

        Environment environment = new Environment();
        environment.setCredential(credential);
        environment.setLdapConfigs(Set.of(ldapConfig));
        environment.setProxyConfigs(Set.of(proxyConfig));
        environment.setRdsConfigs(Set.of(rdsConfig1, rdsConfig2));

        Region region1 = new Region();
        region1.setName("region1");
        Region region2 = new Region();
        region2.setName("region2");
        environment.setRegions(new Json(Set.of(region1, region2)));
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setLdapConfigs(Set.of("ldap1", "ldap2"));
        environmentRequest.setProxyConfigs(Set.of("proxy1", "proxy2"));
        environmentRequest.setRdsConfigs(Set.of("rds1", "rds2", "rds3"));
        environmentRequest.setRegions(Set.of("region1", "region2", "region3"));
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setLocationName("region1");
        environmentRequest.setLocation(locationRequest);

        ValidationResult result = environmentCreationValidator.validate(environment, environmentRequest, true);

        assertEquals(ERROR, result.getState());
        assertEquals(4L, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("[ldap2]"));
        assertTrue(result.getErrors().get(1).contains("[proxy2]"));
        assertTrue(result.getErrors().get(2).contains("[rds3]"));
        assertTrue(result.getErrors().get(3).contains("[region3]"));
    }

    @Test
    public void testValidationWhenRegionsAreNotSupportedOnCloudProviderButProvided() {
        // GIVEN
        Credential credential = new Credential();
        Environment environment = new Environment();
        environment.setCredential(credential);
        environment.setLdapConfigs(Collections.emptySet());
        environment.setProxyConfigs(Collections.emptySet());
        environment.setRdsConfigs(Collections.emptySet());
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setLdapConfigs(Collections.emptySet());
        environmentRequest.setProxyConfigs(Collections.emptySet());
        environmentRequest.setRdsConfigs(Collections.emptySet());
        environmentRequest.setRegions(Set.of("region1", "region2", "region3"));
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setLocationName("region1");
        environmentRequest.setLocation(locationRequest);
        // WHEN
        ValidationResult result = environmentCreationValidator.validate(environment, environmentRequest, false);
        // THEN
        assertEquals(ERROR, result.getState());
        assertEquals(1L, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Region are not supporeted on cloudprovider"));
    }

    @Test
    public void testValidationWhenRegionsAreSupportedOnCloudProviderButNotProvided() throws IOException {
        // GIVEN
        Credential credential = new Credential();
        Environment environment = new Environment();
        environment.setCredential(credential);
        environment.setLdapConfigs(Collections.emptySet());
        environment.setProxyConfigs(Collections.emptySet());
        environment.setRdsConfigs(Collections.emptySet());
        environment.setRegions(new Json(Set.of()));
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setLdapConfigs(Collections.emptySet());
        environmentRequest.setProxyConfigs(Collections.emptySet());
        environmentRequest.setRdsConfigs(Collections.emptySet());
        environmentRequest.setRegions(Collections.emptySet());
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setLocationName("region1");
        locationRequest.setLatitude(1.1);
        locationRequest.setLongitude(-1.1);
        environmentRequest.setLocation(locationRequest);

        // WHEN
        ValidationResult result = environmentCreationValidator.validate(environment, environmentRequest, true);
        // THEN
        assertEquals(ERROR, result.getState());
        assertEquals(1L, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Region are mandatory on cloudprovider"));
    }

    @Test
    public void testSuccessfulValidationWithRegions() throws IOException {
        // GIVEN
        Credential credential = new Credential();

        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setId(0L);
        ldapConfig.setName("ldap1");

        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setId(0L);
        proxyConfig.setName("proxy1");

        RDSConfig rdsConfig1 = new RDSConfig();
        rdsConfig1.setId(0L);
        rdsConfig1.setName("rds1");
        RDSConfig rdsConfig2 = new RDSConfig();
        rdsConfig1.setId(1L);
        rdsConfig2.setName("rds2");

        Environment environment = new Environment();
        environment.setCredential(credential);
        environment.setLdapConfigs(Set.of(ldapConfig));
        environment.setProxyConfigs(Set.of(proxyConfig));
        environment.setRdsConfigs(Set.of(rdsConfig1, rdsConfig2));
        Region region1 = new Region();
        region1.setName("region1");
        Region region2 = new Region();
        region2.setName("region2");
        environment.setRegions(new Json(Set.of(region1, region2)));
        environment.setLocation("region1");
        environment.setLatitude(1.1);
        environment.setLongitude(-1.1);

        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setLdapConfigs(Set.of("ldap1"));
        environmentRequest.setProxyConfigs(Set.of("proxy1"));
        environmentRequest.setRdsConfigs(Set.of("rds1", "rds2"));
        environmentRequest.setRegions(Set.of("region1", "region2"));
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setLocationName("region1");
        environmentRequest.setLocation(locationRequest);
        // WHEN
        ValidationResult result = environmentCreationValidator.validate(environment, environmentRequest, true);
        // THEN
        assertEquals(VALID, result.getState());
    }

    @Test
    public void testSuccessfulValidationWithoutRegions() {
        // GIVEN
        Credential credential = new Credential();

        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setId(0L);
        ldapConfig.setName("ldap1");

        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setId(0L);
        proxyConfig.setName("proxy1");

        RDSConfig rdsConfig1 = new RDSConfig();
        rdsConfig1.setId(0L);
        rdsConfig1.setName("rds1");
        RDSConfig rdsConfig2 = new RDSConfig();
        rdsConfig1.setId(1L);
        rdsConfig2.setName("rds2");

        Environment environment = new Environment();
        environment.setCredential(credential);
        environment.setLdapConfigs(Set.of(ldapConfig));
        environment.setProxyConfigs(Set.of(proxyConfig));
        environment.setRdsConfigs(Set.of(rdsConfig1, rdsConfig2));
        environment.setLocation("region1");
        environment.setLatitude(1.1);
        environment.setLongitude(-1.1);

        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setLdapConfigs(Set.of("ldap1"));
        environmentRequest.setProxyConfigs(Set.of("proxy1"));
        environmentRequest.setRdsConfigs(Set.of("rds1", "rds2"));
        environmentRequest.setRegions(Collections.emptySet());
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setLocationName("region1");
        locationRequest.setLatitude(1.1);
        locationRequest.setLongitude(-1.1);
        environmentRequest.setLocation(locationRequest);

        // WHEN
        ValidationResult result = environmentCreationValidator.validate(environment, environmentRequest, false);
        // THEN
        assertEquals(VALID, result.getState());
    }
}
