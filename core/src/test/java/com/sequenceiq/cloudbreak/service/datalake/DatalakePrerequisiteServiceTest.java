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

import com.sequenceiq.cloudbreak.api.model.KerberosResponse;
import com.sequenceiq.cloudbreak.api.model.datalake.DatalakePrerequisiteRequest;
import com.sequenceiq.cloudbreak.api.model.kerberos.FreeIPAKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.model.kerberos.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigResponse;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.events.DefaultCloudbreakEventService;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosService;
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
    private KerberosService kerberosService;

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
        when(conversionService.convert(any(LdapConfigRequest.class), eq(LdapConfig.class))).thenReturn(ldapConfig());
        when(conversionService.convert(any(LdapConfig.class), eq(LdapDetails.class))).thenReturn(ldapDetails());
        when(conversionService.convert(any(LdapConfig.class), eq(LdapConfigResponse.class))).thenReturn(ldapConfigResponse());

        when(conversionService.convert(any(RDSConfigRequest.class), eq(RDSConfig.class))).thenReturn(rdsConfig());
        when(conversionService.convert(any(RDSConfig.class), eq(RDSConfigResponse.class))).thenReturn(rdsConfigResponse());
        when(conversionService.convert(any(RDSConfig.class), eq(RdsDetails.class))).thenReturn(rdsDetails());

        when(conversionService.convert(any(KerberosRequest.class), eq(KerberosConfig.class))).thenReturn(kerberosConfig());
        when(conversionService.convert(any(KerberosConfig.class), eq(KerberosResponse.class))).thenReturn(kerberosResponse());
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

        when(kerberosService.createInEnvironment(any(KerberosConfig.class), anySet(), anyLong())).thenReturn(kerberosConfig());
        when(ldapConfigService.createInEnvironment(any(LdapConfig.class), anySet(), anyLong())).thenReturn(ldapConfig());
        when(rdsConfigService.createInEnvironment(any(RDSConfig.class), anySet(), anyLong())).thenReturn(rdsConfig());

        underTest.prepare(workspaceId, environmentName, datalakePrerequisiteRequest());

    }

    private DatalakePrerequisiteRequest datalakePrerequisiteRequest() {
        DatalakePrerequisiteRequest datalakePrerequisiteRequest = new DatalakePrerequisiteRequest();
        datalakePrerequisiteRequest.setKerberosConfig(kerberosRequest());
        datalakePrerequisiteRequest.setLdapConfig(ldapConfigRequest());
        datalakePrerequisiteRequest.setRdsConfigs(rdsConfigRequests());
        return datalakePrerequisiteRequest;
    }

    private Set<RDSConfigRequest> rdsConfigRequests() {
        Set<RDSConfigRequest> rdsConfigRequests = new HashSet<>();
        rdsConfigRequests.add(rdsConfigRequest("HIVE"));
        rdsConfigRequests.add(rdsConfigRequest("HIVE_DAS"));
        rdsConfigRequests.add(rdsConfigRequest("AMBARI"));
        rdsConfigRequests.add(rdsConfigRequest("RANGER"));
        return rdsConfigRequests;
    }

    private RDSConfigRequest rdsConfigRequest(String type) {
        RDSConfigRequest rdsConfigRequest = new RDSConfigRequest();
        rdsConfigRequest.setDescription("descriptiion");
        rdsConfigRequest.setName("rds-" + type);
        rdsConfigRequest.setType(type);
        return rdsConfigRequest;
    }

    private LdapConfigRequest ldapConfigRequest() {
        LdapConfigRequest ldapConfigRequest = new LdapConfigRequest();
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

    private LdapConfigResponse ldapConfigResponse() {
        LdapConfigResponse ldapConfigResponse = new LdapConfigResponse();
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

    private RDSConfigResponse rdsConfigResponse() {
        RDSConfigResponse rdsConfigResponse = new RDSConfigResponse();
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

    private KerberosRequest kerberosRequest() {
        KerberosRequest kerberosRequest = new KerberosRequest();
        kerberosRequest.setName("demo-kerberos");
        kerberosRequest.setDescription("kerberos description");
        kerberosRequest.setFreeIpa(freeIPAKerberosDescriptor());
        return kerberosRequest;
    }

    private KerberosResponse kerberosResponse() {
        KerberosResponse kerberosResponse = new KerberosResponse();
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