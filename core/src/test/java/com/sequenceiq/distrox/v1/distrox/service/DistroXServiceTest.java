package com.sequenceiq.distrox.v1.distrox.service;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.AVAILABLE;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.START_DATAHUB_STARTED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.cloudbreak.service.image.ImageOsService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.distrox.api.v1.distrox.model.AzureDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAzureRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.image.DistroXImageV1Request;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToStackV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.fedramp.FedRampModificationService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@ExtendWith(MockitoExtension.class)
class DistroXServiceTest {

    private static final Long USER_ID = 123456L;

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc1:datalake:cluster1";

    private static final boolean NOT_INTERNAL_REQUEST = false;

    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    private static final String ENV_NAME = "someAwesomeEnvironment";

    private static final String CRN = "crn";

    @Mock
    private DistroXV1RequestToStackV4RequestConverter stackRequestConverter;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private FedRampModificationService fedRampModificationService;

    @Mock
    private StackOperations stackOperations;

    @Mock
    private FreeipaClientService freeipaClientService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Mock
    private ImageOsService imageOsService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @InjectMocks
    private DistroXService underTest;

    @BeforeEach
    void setUp() {
        Workspace workspace = new Workspace();
        workspace.setId(USER_ID);
        Tenant tenant = new Tenant();
        tenant.setName("test");
        workspace.setTenant(tenant);
        lenient().when(imageOsService.isSupported(any())).thenReturn(true);
        lenient().when(workspaceService.getForCurrentUser()).thenReturn(workspace);
    }

    @Test
    @DisplayName("When request doesn't contains a valid environment name then BadRequestException should come")
    void testWithInvalidEnvironmentNameValue() {
        String invalidEnvNameValue = "somethingInvalidStuff";
        DistroXV1Request r = new DistroXV1Request();
        r.setEnvironmentName(invalidEnvNameValue);
        when(environmentClientService.getByName(invalidEnvNameValue)).thenReturn(null);

        BadRequestException err = assertThrows(BadRequestException.class, () -> underTest.post(r, NOT_INTERNAL_REQUEST));

        assertEquals("No environment name provided hence unable to obtain some important data", err.getMessage());

        verify(environmentClientService, calledOnce()).getByName(any());
        verify(environmentClientService, calledOnce()).getByName(invalidEnvNameValue);
        verify(stackOperations, never()).post(any(), any(), any(), anyBoolean());
        verify(stackRequestConverter, never()).convert(any(DistroXV1Request.class));
    }

    @Test
    void testWithNotAvailableEnvironmentButFreeipaAvailableAndRunWithoutException() {
        DistroXV1Request request = new DistroXV1Request();
        request.setEnvironmentName(ENV_NAME);
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(START_DATAHUB_STARTED);
        envResponse.setCrn(CRN);
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        freeipa.setStatus(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE);
        doNothing().when(fedRampModificationService).prepare(any(), any());
        when(freeipaClientService.getByEnvironmentCrn(CRN)).thenReturn(freeipa);
        when(environmentClientService.getByName(ENV_NAME)).thenReturn(envResponse);
        when(platformAwareSdxConnector.listSdxCrnsWithAvailability(any())).thenReturn(Set.of(Pair.of(DATALAKE_CRN, StatusCheckResult.AVAILABLE)));

        doAs(ACTOR, () -> underTest.post(request, NOT_INTERNAL_REQUEST));

        verify(platformAwareSdxConnector).listSdxCrnsWithAvailability(any());
    }

    @ParameterizedTest
    @EnumSource(value = com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus.class)
    @DisplayName("When request contains a valid environment name but that environment is not in the AVAILABLE state then BadRequestException should come")
    void testWithValidEnvironmentNameValueButTheActualEnvIsNotAvailableBadRequestExceptionShouldCome(
            com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus detailedStackStatus) {
        if (!detailedStackStatus.getAvailabilityStatus().isAvailable()) {
            String envName = "someAwesomeExistingButNotAvailableEnvironment";
            DistroXV1Request r = new DistroXV1Request();
            r.setEnvironmentName(envName);

            DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
            envResponse.setCrn(CRN);
            envResponse.setEnvironmentStatus(AVAILABLE);
            envResponse.setName(envName);

            DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
            freeipa.setAvailabilityStatus(detailedStackStatus.getAvailabilityStatus());
            freeipa.setStatus(detailedStackStatus.getStatus());
            when(freeipaClientService.getByEnvironmentCrn(CRN)).thenReturn(freeipa);

            when(environmentClientService.getByName(envName)).thenReturn(envResponse);

            BadRequestException err = assertThrows(BadRequestException.class, () -> underTest.post(r, NOT_INTERNAL_REQUEST));

            assertEquals(String.format("If you want to provision a Data Hub then the FreeIPA instance must be running in the '%s' Environment.", envName),
                    err.getMessage());

            verify(environmentClientService, calledOnce()).getByName(any());
            verify(environmentClientService, calledOnce()).getByName(eq(envName));
            verify(stackOperations, never()).post(any(), any(), any(), anyBoolean());
            verify(stackRequestConverter, never()).convert(any(DistroXV1Request.class));
        }
    }

    @Test
    @DisplayName("When the environment that has the given name is exist and also in the state DELETE_IN_PROGRESS then no exception should come")
    void testWhenEnvDeleteInProgressExistsThenShouldThrowBadRequest() {
        DistroXV1Request r = new DistroXV1Request();
        r.setEnvironmentName(ENV_NAME);

        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(EnvironmentStatus.DELETE_INITIATED);
        envResponse.setName(ENV_NAME);
        envResponse.setCrn(CRN);
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        freeipa.setStatus(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE);

        lenient().when(freeipaClientService.getByEnvironmentCrn(CRN)).thenReturn(freeipa);
        lenient().when(environmentClientService.getByName(ENV_NAME)).thenReturn(envResponse);

        StackV4Request converted = new StackV4Request();
        CloudbreakUser cloudbreakUser = mock(CloudbreakUser.class);
        lenient().when(stackRequestConverter.convert(r)).thenReturn(converted);
        lenient().when(platformAwareSdxConnector.listSdxCrns(any())).thenReturn(Set.of(DATALAKE_CRN));
        lenient().when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);

        BadRequestException err = assertThrows(BadRequestException.class, () -> doAs(ACTOR, () -> underTest.post(r, NOT_INTERNAL_REQUEST)));

        assertEquals("'someAwesomeEnvironment' Environment can not be delete in progress state.", err.getMessage());
    }

    @Test
    @DisplayName("When the environment that has the given name is exist and also in the state AVAILABLE then no exception should come")
    void testWhenEnvExistsAndItIsAvailable() {
        DistroXV1Request r = new DistroXV1Request();
        r.setEnvironmentName(ENV_NAME);

        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(AVAILABLE);
        envResponse.setCrn(CRN);
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        freeipa.setStatus(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE);
        doNothing().when(fedRampModificationService).prepare(any(), any());
        when(freeipaClientService.getByEnvironmentCrn(CRN)).thenReturn(freeipa);
        when(environmentClientService.getByName(ENV_NAME)).thenReturn(envResponse);

        StackV4Request converted = new StackV4Request();
        CloudbreakUser cloudbreakUser = mock(CloudbreakUser.class);
        when(stackRequestConverter.convert(r)).thenReturn(converted);
        when(platformAwareSdxConnector.listSdxCrnsWithAvailability(any())).thenReturn(Set.of(Pair.of(DATALAKE_CRN, StatusCheckResult.AVAILABLE)));
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);

        doAs(ACTOR, () -> underTest.post(r, NOT_INTERNAL_REQUEST));

        verify(environmentClientService, calledOnce()).getByName(any());
        verify(environmentClientService, calledOnce()).getByName(ENV_NAME);
        verify(stackOperations, calledOnce()).post(any(), any(), any(), anyBoolean());
        verify(stackOperations, calledOnce()).post(eq(USER_ID), eq(cloudbreakUser), eq(converted), eq(true));
        verify(workspaceService, calledOnce()).getForCurrentUser();
        verify(stackRequestConverter, calledOnce()).convert(any(DistroXV1Request.class));
        verify(stackRequestConverter, calledOnce()).convert(r);
    }

    @Test
    @DisplayName("When the env does not contain FreeIPA, but has LDAP and Kerberos configs registered it should succeed")
    void testWhenEnvExistsWithoutFreeIPAButLdapAndKerberosExists() throws IllegalAccessException {
        String envName = "someAwesomeEnvironment";
        String clusterName = "someClusterName";
        DistroXV1Request r = new DistroXV1Request();
        r.setEnvironmentName(envName);
        r.setName(clusterName);

        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(AVAILABLE);
        envResponse.setCrn("crn");
        doNothing().when(fedRampModificationService).prepare(any(), any());
        when(environmentClientService.getByName(envName)).thenReturn(envResponse);

        StackV4Request converted = new StackV4Request();
        CloudbreakUser cloudbreakUser = mock(CloudbreakUser.class);
        when(stackRequestConverter.convert(r)).thenReturn(converted);
        when(platformAwareSdxConnector.listSdxCrnsWithAvailability(any())).thenReturn(Set.of(Pair.of(DATALAKE_CRN, StatusCheckResult.AVAILABLE)));
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(freeipaClientService.getByEnvironmentCrn("crn")).thenThrow(CloudbreakServiceException.class);
        when(ldapConfigService.isLdapConfigExistsForEnvironment("crn", clusterName)).thenReturn(true);
        when(kerberosConfigService.isKerberosConfigExistsForEnvironment("crn", clusterName)).thenReturn(true);

        doAs(ACTOR, () -> underTest.post(r, NOT_INTERNAL_REQUEST));

        verify(environmentClientService, calledOnce()).getByName(any());
        verify(environmentClientService, calledOnce()).getByName(envName);
        verify(stackOperations, calledOnce()).post(any(), any(), any(), anyBoolean());
        verify(stackOperations, calledOnce()).post(eq(USER_ID), eq(cloudbreakUser), eq(converted), eq(true));
        verify(workspaceService, calledOnce()).getForCurrentUser();
        verify(stackRequestConverter, calledOnce()).convert(any(DistroXV1Request.class));
        verify(stackRequestConverter, calledOnce()).convert(r);
        verify(ldapConfigService, calledOnce()).isLdapConfigExistsForEnvironment("crn", clusterName);
        verify(kerberosConfigService).isKerberosConfigExistsForEnvironment("crn", clusterName);
    }

    @Test
    @DisplayName("When the env does not contain FreeIPA, and Ldap config is not registered, bad request should be thrown")
    void testWhenEnvExistsWithoutFreeIPAButLdapConfigDoesNotExist() throws IllegalAccessException {
        String envName = "someAwesomeEnvironment";
        String clusterName = "someClusterName";
        DistroXV1Request r = new DistroXV1Request();
        r.setEnvironmentName(envName);
        r.setName(clusterName);

        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(AVAILABLE);
        envResponse.setCrn("crn");
        when(environmentClientService.getByName(envName)).thenReturn(envResponse);

        StackV4Request converted = new StackV4Request();
        CloudbreakUser cloudbreakUser = mock(CloudbreakUser.class);
        when(freeipaClientService.getByEnvironmentCrn("crn")).thenThrow(CloudbreakServiceException.class);

        BadRequestException err = assertThrows(BadRequestException.class, () -> doAs(ACTOR, () -> underTest.post(r, NOT_INTERNAL_REQUEST)));

        assertEquals("If you want to provision a Data Hub without FreeIPA then please register an LDAP config", err.getMessage());
    }

    @Test
    @DisplayName("When the env does not contain FreeIPA, and Kerberos config is not registered, bad request should be thrown")
    void testWhenEnvExistsWithoutFreeIPAButKerberosConfigDoesNotExist() throws IllegalAccessException {
        String envName = "someAwesomeEnvironment";
        String clusterName = "someClusterName";
        DistroXV1Request r = new DistroXV1Request();
        r.setEnvironmentName(envName);
        r.setName(clusterName);

        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(AVAILABLE);
        envResponse.setCrn("crn");
        when(environmentClientService.getByName(envName)).thenReturn(envResponse);

        StackV4Request converted = new StackV4Request();
        CloudbreakUser cloudbreakUser = mock(CloudbreakUser.class);
        when(freeipaClientService.getByEnvironmentCrn("crn")).thenThrow(CloudbreakServiceException.class);
        when(ldapConfigService.isLdapConfigExistsForEnvironment("crn", clusterName)).thenReturn(true);

        BadRequestException err = assertThrows(BadRequestException.class, () -> doAs(ACTOR, () -> underTest.post(r, NOT_INTERNAL_REQUEST)));

        assertEquals("If you want to provision a Data Hub without FreeIPA then please register a Kerberos config", err.getMessage());
    }

    @Test
    public void testIfDlIsNotExists() {
        DistroXV1Request request = new DistroXV1Request();
        request.setEnvironmentName(ENV_NAME);
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(AVAILABLE);
        envResponse.setCrn(CRN);
        envResponse.setName(ENV_NAME);
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        freeipa.setStatus(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE);
        when(freeipaClientService.getByEnvironmentCrn(CRN)).thenReturn(freeipa);
        when(environmentClientService.getByName(ENV_NAME)).thenReturn(envResponse);
        when(platformAwareSdxConnector.listSdxCrnsWithAvailability(any())).thenReturn(Set.of());

        assertThatThrownBy(() -> underTest.post(request, NOT_INTERNAL_REQUEST))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Data Lake stack cannot be found for environment: %s (%s)", ENV_NAME, envResponse.getCrn());

        verifyNoMoreInteractions(platformAwareSdxConnector);
    }

    @Test
    public void testIfDlIsNotRunning() {
        DistroXV1Request request = new DistroXV1Request();
        request.setEnvironmentName(ENV_NAME);
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(AVAILABLE);
        envResponse.setCrn(CRN);
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        freeipa.setStatus(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE);
        when(freeipaClientService.getByEnvironmentCrn(CRN)).thenReturn(freeipa);
        when(environmentClientService.getByName(ENV_NAME)).thenReturn(envResponse);
        when(platformAwareSdxConnector.listSdxCrnsWithAvailability(any()))
                .thenReturn(Set.of(Pair.of(DATALAKE_CRN, StatusCheckResult.NOT_AVAILABLE)));

        assertThatThrownBy(() -> underTest.post(request, NOT_INTERNAL_REQUEST))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Data Lake stacks of environment should be available.");

        verify(platformAwareSdxConnector).listSdxCrnsWithAvailability(any());
    }

    @Test
    public void testIfDlIsRunning() {
        DistroXV1Request request = new DistroXV1Request();
        request.setEnvironmentName(ENV_NAME);
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(AVAILABLE);
        envResponse.setCrn(CRN);
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        freeipa.setStatus(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE);
        Workspace workspace = new Workspace();
        Tenant tenant = new Tenant();
        tenant.setName("test");
        workspace.setTenant(tenant);
        doNothing().when(fedRampModificationService).prepare(any(), any());
        when(workspaceService.getForCurrentUser()).thenReturn(workspace);
        when(freeipaClientService.getByEnvironmentCrn(CRN)).thenReturn(freeipa);
        when(environmentClientService.getByName(ENV_NAME)).thenReturn(envResponse);
        when(platformAwareSdxConnector.listSdxCrnsWithAvailability(any()))
                .thenReturn(Set.of(Pair.of(DATALAKE_CRN, StatusCheckResult.AVAILABLE)));

        doAs(ACTOR, () -> underTest.post(request, NOT_INTERNAL_REQUEST));

        verify(platformAwareSdxConnector).listSdxCrnsWithAvailability(any());
    }

    @Test
    public void testIfDlRollingUpgradeInProgress() {
        DistroXV1Request request = new DistroXV1Request();
        request.setEnvironmentName(ENV_NAME);
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(AVAILABLE);
        envResponse.setCrn(CRN);
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        freeipa.setStatus(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE);
        Workspace workspace = new Workspace();
        Tenant tenant = new Tenant();
        tenant.setName("test");
        workspace.setTenant(tenant);
        doNothing().when(fedRampModificationService).prepare(any(), any());
        when(workspaceService.getForCurrentUser()).thenReturn(workspace);
        when(freeipaClientService.getByEnvironmentCrn(CRN)).thenReturn(freeipa);
        when(environmentClientService.getByName(ENV_NAME)).thenReturn(envResponse);
        when(platformAwareSdxConnector.listSdxCrnsWithAvailability(any()))
                .thenReturn(Set.of(Pair.of(DATALAKE_CRN, StatusCheckResult.ROLLING_UPGRADE_IN_PROGRESS)));

        doAs(ACTOR, () -> underTest.post(request, NOT_INTERNAL_REQUEST));

        verify(platformAwareSdxConnector).listSdxCrnsWithAvailability(any());
    }

    @Test
    void testIfImageIdAndOsBothSet() {
        DistroXV1Request request = new DistroXV1Request();
        request.setEnvironmentName(ENV_NAME);
        DistroXImageV1Request imageRequest = new DistroXImageV1Request();
        imageRequest.setId("id");
        imageRequest.setOs("os");
        request.setImage(imageRequest);
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(AVAILABLE);
        envResponse.setCrn(CRN);
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        freeipa.setStatus(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE);
        Workspace workspace = new Workspace();
        Tenant tenant = new Tenant();
        tenant.setName("test");
        workspace.setTenant(tenant);
        when(workspaceService.getForCurrentUser()).thenReturn(workspace);
        when(freeipaClientService.getByEnvironmentCrn(CRN)).thenReturn(freeipa);
        when(environmentClientService.getByName(ENV_NAME)).thenReturn(envResponse);
        when(platformAwareSdxConnector.listSdxCrnsWithAvailability(any()))
                .thenReturn(Set.of(Pair.of(DATALAKE_CRN, StatusCheckResult.AVAILABLE)));

        assertThatThrownBy(() -> underTest.post(request, NOT_INTERNAL_REQUEST))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Image request can not have both image id and os parameters set.");

        verify(platformAwareSdxConnector).listSdxCrnsWithAvailability(any());
    }

    @Test
    void testIfImageOsIsNotSupported() {
        DistroXV1Request request = new DistroXV1Request();
        request.setEnvironmentName(ENV_NAME);
        DistroXImageV1Request imageRequest = new DistroXImageV1Request();
        imageRequest.setOs("os");
        request.setImage(imageRequest);
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(AVAILABLE);
        envResponse.setCrn(CRN);
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        freeipa.setStatus(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE);
        Workspace workspace = new Workspace();
        Tenant tenant = new Tenant();
        tenant.setName("test");
        workspace.setTenant(tenant);
        when(workspaceService.getForCurrentUser()).thenReturn(workspace);
        when(freeipaClientService.getByEnvironmentCrn(CRN)).thenReturn(freeipa);
        when(environmentClientService.getByName(ENV_NAME)).thenReturn(envResponse);
        when(platformAwareSdxConnector.listSdxCrnsWithAvailability(any()))
                .thenReturn(Set.of(Pair.of(DATALAKE_CRN, StatusCheckResult.AVAILABLE)));
        when(imageOsService.isSupported(any())).thenReturn(false);

        assertThatThrownBy(() -> underTest.post(request, NOT_INTERNAL_REQUEST))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Image os 'os' is not supported in your account.");

        verify(platformAwareSdxConnector).listSdxCrnsWithAvailability(any());
    }

    @Test
    void testWithSingleServerWhenSingleServerRejectEnabled() {
        DistroXV1Request request = createDistroXV1RequestForAzureSingleServerRejectionTest(AzureDatabaseType.SINGLE_SERVER, true);

        assertThatThrownBy(() -> doAs(ACTOR, () -> underTest.post(request, NOT_INTERNAL_REQUEST)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Azure Database for PostgreSQL - Single Server is retired. New deployments cannot be created anymore. " +
                        "Check documentation for more information: " +
                        "https://learn.microsoft.com/en-us/azure/postgresql/migrate/whats-happening-to-postgresql-single-server");
    }

    @Test
    void testWithSingleServerWhenSingleServerRejectDisabled() {
        DistroXV1Request request = createDistroXV1RequestForAzureSingleServerRejectionTest(AzureDatabaseType.SINGLE_SERVER, false);
        doAs(ACTOR, () -> underTest.post(request, NOT_INTERNAL_REQUEST));
        verify(platformAwareSdxConnector).listSdxCrnsWithAvailability(any());

    }

    @Test
    void testWithFlexibleServerWhenSingleServerRejectEnabled() {
        DistroXV1Request request = createDistroXV1RequestForAzureSingleServerRejectionTest(AzureDatabaseType.FLEXIBLE_SERVER, true);
        doAs(ACTOR, () -> underTest.post(request, NOT_INTERNAL_REQUEST));
        verify(platformAwareSdxConnector).listSdxCrnsWithAvailability(any());
    }

    @Test
    void testWithFlexibleServerWhenSingleServerRejectDisabled() {
        DistroXV1Request request = createDistroXV1RequestForAzureSingleServerRejectionTest(AzureDatabaseType.FLEXIBLE_SERVER, false);
        doAs(ACTOR, () -> underTest.post(request, NOT_INTERNAL_REQUEST));
        verify(platformAwareSdxConnector).listSdxCrnsWithAvailability(any());

    }

    @Test
    void testShouldThrowExceptionWhenAzureRequestContainsBasicLoadBalancerSku() {
        DistroXV1Request request = createDistroXV1RequestForAzureSingleServerRejectionTest(AzureDatabaseType.FLEXIBLE_SERVER, true);
        AzureDistroXV1Parameters azureDistroXV1Parameters = new AzureDistroXV1Parameters();
        azureDistroXV1Parameters.setLoadBalancerSku(LoadBalancerSku.BASIC);
        request.setAzure(azureDistroXV1Parameters);

        assertThatThrownBy(() -> doAs(ACTOR, () -> underTest.post(request, NOT_INTERNAL_REQUEST)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("The Basic SKU type is no longer supported for Load Balancers. Please use the Standard SKU to provision a Load Balancer. "
                        + "Check documentation for more information: https://azure.microsoft.com/en-gb/updates?id="
                        + "azure-basic-load-balancer-will-be-retired-on-30-september-2025-upgrade-to-standard-load-balancer");
    }

    @Test
    void testShouldNotThrowExceptionWhenTheAzureLoadBalancerSkuIsNull() {
        DistroXV1Request request = createDistroXV1RequestForAzureSingleServerRejectionTest(AzureDatabaseType.FLEXIBLE_SERVER, true);
        request.setAzure(new AzureDistroXV1Parameters());

        doAs(ACTOR, () -> underTest.post(request, NOT_INTERNAL_REQUEST));
    }

    private DistroXV1Request createDistroXV1RequestForAzureSingleServerRejectionTest(
            AzureDatabaseType azureDatabaseType, boolean singleServerRejectEnabled) {
        DistroXV1Request request = new DistroXV1Request();
        request.setEnvironmentName(ENV_NAME);
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(AVAILABLE);
        envResponse.setCrn(CRN);
        envResponse.setCloudPlatform(CloudPlatform.AZURE.name());
        DescribeFreeIpaResponse freeipa = new DescribeFreeIpaResponse();
        freeipa.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        freeipa.setStatus(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE);

        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        DistroXDatabaseAzureRequest databaseAzureRequest = new DistroXDatabaseAzureRequest();
        databaseAzureRequest.setAzureDatabaseType(azureDatabaseType);
        databaseRequest.setDatabaseAzureRequest(databaseAzureRequest);
        request.setExternalDatabase(databaseRequest);

        when(freeipaClientService.getByEnvironmentCrn(CRN)).thenReturn(freeipa);
        when(environmentClientService.getByName(ENV_NAME)).thenReturn(envResponse);
        when(platformAwareSdxConnector.listSdxCrnsWithAvailability(any()))
                .thenReturn(Set.of(Pair.of(DATALAKE_CRN, StatusCheckResult.AVAILABLE)));
        when(entitlementService.isSingleServerRejectEnabled(any())).thenReturn(singleServerRejectEnabled);
        return request;
    }

    private static VerificationMode calledOnce() {
        return times(1);
    }

}