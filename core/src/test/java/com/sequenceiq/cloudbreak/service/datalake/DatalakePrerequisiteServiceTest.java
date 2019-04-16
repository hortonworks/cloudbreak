package com.sequenceiq.cloudbreak.service.datalake;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.DatalakePrerequisiteV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.FreeIPAKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.events.DefaultCloudbreakEventService;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.structuredevent.event.LdapDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.RdsDetails;

@RunWith(MockitoJUnitRunner.class)
public class DatalakePrerequisiteServiceTest {

    private static long workspaceId = 4;

    private static String environmentName = "test-environment";

    private static String errorMessage = "Something wrong...";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private RdsConnectionValidator rdsConnectionValidator;

    @Mock
    private LdapConfigValidator ldapConfigValidator;

    @Mock
    private DefaultCloudbreakEventService defaultCloudbreakEventService;

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private final DatalakePrerequisiteService underTest = new DatalakePrerequisiteService();

    @Before
    public void before() {
        when(conversionService.convert(any(LdapV4Request.class), eq(LdapConfig.class))).thenReturn(ldapConfig());
        when(conversionService.convert(any(LdapConfig.class), eq(LdapDetails.class))).thenReturn(ldapDetails());
        when(conversionService.convert(any(LdapConfig.class), eq(LdapV4Response.class))).thenReturn(ldapConfigResponse());

        when(conversionService.convert(any(DatabaseV4Request.class), eq(RDSConfig.class))).thenReturn(rdsConfig());
        when(conversionService.convert(any(RDSConfig.class), eq(DatabaseV4Response.class))).thenReturn(rdsConfigResponse());
        when(conversionService.convert(any(RDSConfig.class), eq(RdsDetails.class))).thenReturn(rdsDetails());

        when(conversionService.convert(any(KerberosV4Request.class), eq(KerberosConfig.class))).thenReturn(kerberosConfig());
        when(conversionService.convert(any(KerberosConfig.class), eq(KerberosV4Response.class))).thenReturn(kerberosResponse());
    }

    @Test
    public void testTestConnectionWhenLdapIsNotAvailableShouldThrowBadRequestException() {
        doThrow(new BadRequestException(errorMessage)).when(ldapConfigValidator).validateLdapConnection(any(LdapConfig.class));
        doNothing().when(defaultCloudbreakEventService).fireLdapEvent(any(LdapDetails.class), anyString(), anyString(), anyBoolean());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Some of the resource for datalake prerequistes was not available please make sure that everything is up and running.");

        underTest.prepare(workspaceId, environmentName, datalakePrerequisiteRequest());
    }

    @Test
    public void testTestConnectionWhenRdsIsNotAvailableShouldThrowBadRequestException() {
        doThrow(new BadRequestException(errorMessage)).when(rdsConnectionValidator).validateRdsConnection(any(RDSConfig.class));
        doNothing().when(defaultCloudbreakEventService).fireLdapEvent(any(LdapDetails.class), anyString(), anyString(), anyBoolean());
        doNothing().when(ldapConfigValidator).validateLdapConnection(any(LdapConfig.class));
        doNothing().when(defaultCloudbreakEventService).fireRdsEvent(any(RdsDetails.class), anyString(), anyString(), anyBoolean());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Some of the resource for datalake prerequistes was not available please make sure that everything is up and running.");

        underTest.prepare(workspaceId, environmentName, datalakePrerequisiteRequest());
    }

    @Test
    public void testPrepareResourceWhenTestIsOkAndShouldPersistResources() {
        doNothing().when(defaultCloudbreakEventService).fireLdapEvent(any(LdapDetails.class), anyString(), anyString(), anyBoolean());
        doNothing().when(ldapConfigValidator).validateLdapConnection(any(LdapConfig.class));
        doNothing().when(rdsConnectionValidator).validateRdsConnection(any(RDSConfig.class));
        doNothing().when(defaultCloudbreakEventService).fireRdsEvent(any(RdsDetails.class), anyString(), anyString(), anyBoolean());

        when(kerberosConfigService.createInEnvironment(any(KerberosConfig.class), anySet(), anyLong())).thenReturn(kerberosConfig());
        when(ldapConfigService.createInEnvironment(any(LdapConfig.class), anySet(), anyLong())).thenReturn(ldapConfig());
        when(rdsConfigService.createInEnvironment(any(RDSConfig.class), anySet(), anyLong())).thenReturn(rdsConfig());

        underTest.prepare(workspaceId, environmentName, datalakePrerequisiteRequest());

    }

    private DatalakePrerequisiteV4Request datalakePrerequisiteRequest() {
        DatalakePrerequisiteV4Request datalakePrerequisiteV4Request = new DatalakePrerequisiteV4Request();
        datalakePrerequisiteV4Request.setKerberos(kerberosRequest());
        datalakePrerequisiteV4Request.setLdap(ldapConfigRequest());
        datalakePrerequisiteV4Request.setDatabases(rdsConfigRequests());
        return datalakePrerequisiteV4Request;
    }

    private Set<DatabaseV4Request> rdsConfigRequests() {
        Set<DatabaseV4Request> rdsConfigRequests = new HashSet<>();
        rdsConfigRequests.add(rdsConfigRequest("HIVE"));
        rdsConfigRequests.add(rdsConfigRequest("HIVE_DAS"));
        rdsConfigRequests.add(rdsConfigRequest("AMBARI"));
        rdsConfigRequests.add(rdsConfigRequest("RANGER"));
        return rdsConfigRequests;
    }

    private DatabaseV4Request rdsConfigRequest(String type) {
        DatabaseV4Request rdsConfigRequest = new DatabaseV4Request();
        rdsConfigRequest.setDescription("descriptiion");
        rdsConfigRequest.setName("rds-" + type);
        rdsConfigRequest.setType(type);
        return rdsConfigRequest;
    }

    private LdapV4Request ldapConfigRequest() {
        LdapV4Request ldapConfigRequest = new LdapV4Request();
        ldapConfigRequest.setName("demo-ldap");
        ldapConfigRequest.setDescription("ldap description");
        return ldapConfigRequest;
    }

    private LdapConfig ldapConfig() {
        LdapConfig ldapConfigRequest = new LdapConfig();
        ldapConfigRequest.setName("demo-ldap");
        ldapConfigRequest.setDescription("ldap description");
        return ldapConfigRequest;
    }

    private LdapV4Response ldapConfigResponse() {
        LdapV4Response ldapConfigResponse = new LdapV4Response();
        ldapConfigResponse.setName("demo-ldap");
        ldapConfigResponse.setDescription("ldap description");
        return ldapConfigResponse;
    }

    private LdapDetails ldapDetails() {
        LdapDetails ldapDetails = new LdapDetails();
        ldapDetails.setName("demo-ldap");
        ldapDetails.setDescription("ldap description");
        return ldapDetails;
    }

    private RDSConfig rdsConfig() {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setName("demo-rds");
        rdsConfig.setDescription("rds description");
        return rdsConfig;
    }

    private RdsDetails rdsDetails() {
        RdsDetails rdsDetails = new RdsDetails();
        rdsDetails.setName("demo-rds");
        rdsDetails.setDescription("rds description");
        return rdsDetails;
    }

    private DatabaseV4Response rdsConfigResponse() {
        DatabaseV4Response rdsConfigResponse = new DatabaseV4Response();
        rdsConfigResponse.setName("demo-rds");
        rdsConfigResponse.setDescription("rds description");
        return rdsConfigResponse;
    }

    private KerberosConfig kerberosConfig() {
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setName("demo-kerberos");
        kerberosConfig.setDescription("kerberos description");
        return kerberosConfig;
    }

    private KerberosV4Request kerberosRequest() {
        KerberosV4Request kerberosRequest = new KerberosV4Request();
        kerberosRequest.setName("demo-kerberos");
        kerberosRequest.setDescription("kerberos description");
        kerberosRequest.setFreeIpa(freeIPAKerberosDescriptor());
        return kerberosRequest;
    }

    private KerberosV4Response kerberosResponse() {
        KerberosV4Response kerberosResponse = new KerberosV4Response();
        kerberosResponse.setName("demo-kerberos");
        kerberosResponse.setDescription("kerberos description");
        return kerberosResponse;
    }

    private FreeIPAKerberosDescriptor freeIPAKerberosDescriptor() {
        FreeIPAKerberosDescriptor freeIPAKerberosDescriptor = new FreeIPAKerberosDescriptor();
        freeIPAKerberosDescriptor.setAdminUrl("123.123.123.123");
        freeIPAKerberosDescriptor.setAdmin("kerberos-admin");
        freeIPAKerberosDescriptor.setRealm("realm");
        freeIPAKerberosDescriptor.setUrl("345.345.345.345");
        freeIPAKerberosDescriptor.setDomain("domain");
        freeIPAKerberosDescriptor.setNameServers("nameserver");
        freeIPAKerberosDescriptor.setPassword("password");
        freeIPAKerberosDescriptor.setTcpAllowed(true);
        freeIPAKerberosDescriptor.setVerifyKdcTrust(true);
        freeIPAKerberosDescriptor.setPrincipal("principal");
        return freeIPAKerberosDescriptor;
    }

}