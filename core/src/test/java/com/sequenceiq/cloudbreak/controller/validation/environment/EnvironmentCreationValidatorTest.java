package com.sequenceiq.cloudbreak.controller.validation.environment;

import static com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State.ERROR;
import static com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State.VALID;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentNetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.LocationV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.environment.network.EnvironmentNetworkValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.environment.Region;
import com.sequenceiq.cloudbreak.util.EnvironmentUtils;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentCreationValidatorTest {

    @Mock
    private Map<CloudPlatform, EnvironmentNetworkValidator> environmentNetworkValidatorsByCloudPlatform;

    @Spy
    private EnvironmentRegionValidator environmentRegionValidator = new EnvironmentRegionValidator();

    @InjectMocks
    private EnvironmentCreationValidator environmentCreationValidator;

    @Test
    public void testValidationWithMultipleErrors() {
        assertNotNull(environmentRegionValidator);
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

        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setId(1L);
        kerberosConfig.setName("kdc1");

        Environment environment = new Environment();
        environment.setCredential(credential);
        environment.setLdapConfigs(Set.of(ldapConfig));
        environment.setProxyConfigs(Set.of(proxyConfig));
        environment.setRdsConfigs(Set.of(rdsConfig1, rdsConfig2));
        environment.setKerberosConfigs(Set.of(kerberosConfig));

        Region region1 = new Region();
        region1.setName("region1");
        Region region2 = new Region();
        region2.setName("region2");
        environment.setRegions(Set.of(region1, region2));
        EnvironmentV4Request environmentRequest = new EnvironmentV4Request();
        environmentRequest.setLdaps(Set.of("ldap1", "ldap2"));
        environmentRequest.setProxies(Set.of("proxy1", "proxy2"));
        environmentRequest.setDatabases(Set.of("rds1", "rds2", "rds3"));
        environmentRequest.setKerberoses(Set.of("kdc1", "kdc2"));
        environmentRequest.setRegions(Set.of("region1", "region2", "region3"));
        LocationV4Request locationRequest = new LocationV4Request();
        locationRequest.setName("region1");
        environmentRequest.setLocation(locationRequest);
        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions();

        ValidationResult result = environmentCreationValidator.validate(environment, environmentRequest, cloudRegions);

        assertEquals(ERROR, result.getState());
        assertEquals(5L, result.getErrors().size());
        assertTrue(result.getFormattedErrors().contains("[ldap2]"));
        assertTrue(result.getFormattedErrors().contains("[proxy2]"));
        assertTrue(result.getFormattedErrors().contains("[rds3]"));
        assertTrue(result.getFormattedErrors().contains("[region3]"));
        assertTrue(result.getFormattedErrors().contains("[kdc2]"));
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
        EnvironmentV4Request environmentRequest = new EnvironmentV4Request();
        environmentRequest.setLdaps(Collections.emptySet());
        environmentRequest.setProxies(Collections.emptySet());
        environmentRequest.setDatabases(Collections.emptySet());
        environmentRequest.setRegions(Set.of("region1", "region2", "region3"));
        LocationV4Request locationRequest = new LocationV4Request();
        locationRequest.setName("region1");
        environmentRequest.setLocation(locationRequest);
        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions(false);
        // WHEN
        ValidationResult result = environmentCreationValidator.validate(environment, environmentRequest, cloudRegions);
        // THEN
        assertEquals(ERROR, result.getState());
        assertEquals(1L, result.getErrors().size());

        assertThat(result.getErrors().get(0), containsString("Regions are not supporeted on cloudprovider"));
    }

    @Test
    public void testValidationWhenRegionsAreSupportedOnCloudProviderButNotProvided() {
        // GIVEN
        Credential credential = new Credential();
        Environment environment = new Environment();
        environment.setCredential(credential);
        environment.setLdapConfigs(Collections.emptySet());
        environment.setProxyConfigs(Collections.emptySet());
        environment.setRdsConfigs(Collections.emptySet());
        environment.setRegions(Set.of());
        EnvironmentV4Request environmentRequest = new EnvironmentV4Request();
        environmentRequest.setLdaps(Collections.emptySet());
        environmentRequest.setProxies(Collections.emptySet());
        environmentRequest.setDatabases(Collections.emptySet());
        environmentRequest.setRegions(Collections.emptySet());
        LocationV4Request locationRequest = new LocationV4Request();
        locationRequest.setName("region1");
        locationRequest.setLatitude(1.1);
        locationRequest.setLongitude(-1.1);
        environmentRequest.setLocation(locationRequest);
        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions();
        // WHEN
        ValidationResult result = environmentCreationValidator.validate(environment, environmentRequest, cloudRegions);
        // THEN
        assertEquals(ERROR, result.getState());
        assertEquals(1L, result.getErrors().size());
        assertThat(result.getErrors().get(0), containsString("Regions are mandatory on cloudprovider"));
    }

    @Test
    public void testSuccessfulValidationWithRegions() {
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
        environment.setRegions(Set.of(region1, region2));
        environment.setLocation("region1");
        environment.setLatitude(1.1);
        environment.setLongitude(-1.1);

        EnvironmentV4Request environmentRequest = new EnvironmentV4Request();
        environmentRequest.setLdaps(Set.of("ldap1"));
        environmentRequest.setProxies(Set.of("proxy1"));
        environmentRequest.setDatabases(Set.of("rds1", "rds2"));
        environmentRequest.setRegions(Set.of("region1", "region2"));
        LocationV4Request locationRequest = new LocationV4Request();
        locationRequest.setName("region1");
        environmentRequest.setLocation(locationRequest);
        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions();
        // WHEN
        ValidationResult result = environmentCreationValidator.validate(environment, environmentRequest, cloudRegions);
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

        EnvironmentV4Request environmentRequest = new EnvironmentV4Request();
        environmentRequest.setLdaps(Set.of("ldap1"));
        environmentRequest.setProxies(Set.of("proxy1"));
        environmentRequest.setDatabases(Set.of("rds1", "rds2"));
        environmentRequest.setRegions(Collections.emptySet());
        LocationV4Request locationRequest = new LocationV4Request();
        locationRequest.setName("region1");
        locationRequest.setLatitude(1.1);
        locationRequest.setLongitude(-1.1);
        environmentRequest.setLocation(locationRequest);
        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions(false);

        // WHEN
        ValidationResult result = environmentCreationValidator.validate(environment, environmentRequest, cloudRegions);
        // THEN
        assertEquals(VALID, result.getState());
    }

    @Test
    public void testValidationWhenNetworkIsNotSupportedForPlatform() {
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

        EnvironmentV4Request environmentRequest = new EnvironmentV4Request();
        environmentRequest.setLdaps(Set.of("ldap1"));
        environmentRequest.setProxies(Set.of("proxy1"));
        environmentRequest.setDatabases(Set.of("rds1", "rds2"));
        environmentRequest.setRegions(Collections.emptySet());
        LocationV4Request locationRequest = new LocationV4Request();
        locationRequest.setName("region1");
        locationRequest.setLatitude(1.1);
        locationRequest.setLongitude(-1.1);
        environmentRequest.setLocation(locationRequest);
        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions(false);

        CloudPlatform mock = CloudPlatform.MOCK;
        environment.setCloudPlatform(mock.name());
        environmentRequest.setNetwork(new EnvironmentNetworkV4Request());
        when(environmentNetworkValidatorsByCloudPlatform.get(mock)).thenReturn(null);

        // WHEN
        ValidationResult result = environmentCreationValidator.validate(environment, environmentRequest, cloudRegions);
        // THEN
        assertEquals(ERROR, result.getState());
        assertEquals(1L, result.getErrors().size());
        assertThat(result.getErrors().get(0), containsString("Environment specific network is not supported for cloud platform:"));
    }
}