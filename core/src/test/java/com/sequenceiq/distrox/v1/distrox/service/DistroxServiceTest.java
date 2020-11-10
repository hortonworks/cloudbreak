package com.sequenceiq.distrox.v1.distrox.service;

import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.AVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.verification.VerificationMode;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.domain.view.StackStatusView;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToStackV4RequestConverter;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;

class DistroxServiceTest {

    private static final Long USER_ID = 123456L;

    @Mock
    private DistroXV1RequestToStackV4RequestConverter stackRequestConverter;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private StackOperations stackOperations;

    @Mock
    private StackViewService stackViewService;

    @InjectMocks
    private DistroxService underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        Workspace workspace = new Workspace();
        workspace.setId(USER_ID);
        when(workspaceService.getForCurrentUser()).thenReturn(workspace);
    }

    @Test
    @DisplayName("When request doesn't contains a valid environment name then BadRequestException should come")
    void testWithInvalidEnvironmentNameValue() {
        String invalidEnvNameValue = "somethingInvalidStuff";
        DistroXV1Request r = new DistroXV1Request();
        r.setEnvironmentName(invalidEnvNameValue);

        when(environmentClientService.getByName(invalidEnvNameValue)).thenReturn(null);

        BadRequestException err = assertThrows(BadRequestException.class, () -> underTest.post(r));

        assertEquals("No environment name provided hence unable to obtain some important data", err.getMessage());

        verify(environmentClientService, calledOnce()).getByName(any());
        verify(environmentClientService, calledOnce()).getByName(invalidEnvNameValue);
        verify(stackOperations, never()).post(any(), any(), anyBoolean());
        verify(workspaceService, never()).getForCurrentUser();
        verify(stackRequestConverter, never()).convert(any(DistroXV1Request.class));
    }

    @ParameterizedTest
    @EnumSource(value = EnvironmentStatus.class, names = "AVAILABLE", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("When request contains a valid environment name but that environment is not in the AVAILABLE state then BadRequestException should come")
    void testWithValidEnvironmentNameValueButTheActualEnvIsNotAvailableBadRequestExceptionShouldCome(EnvironmentStatus status) {
        String envName = "someAwesomeExistingButNotAvailableEnvironment";
        DistroXV1Request r = new DistroXV1Request();
        r.setEnvironmentName(envName);

        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(status);

        when(environmentClientService.getByName(envName)).thenReturn(envResponse);

        BadRequestException err = assertThrows(BadRequestException.class, () -> underTest.post(r));


        assertEquals(String.format("Environment state is %s instead of AVAILABLE", status.name()), err.getMessage());

        verify(environmentClientService, calledOnce()).getByName(any());
        verify(environmentClientService, calledOnce()).getByName(eq(envName));
        verify(stackOperations, never()).post(any(), any(), anyBoolean());
        verify(workspaceService, never()).getForCurrentUser();
        verify(stackRequestConverter, never()).convert(any(DistroXV1Request.class));
    }

    @Test
    @DisplayName("When the environment that has the given name is exist and also in the state AVAILABLE then no exception should come")
    void testWhenEnvExistsAndItIsAvailable() throws IllegalAccessException {
        String envName = "someAwesomeEnvironment";
        DistroXV1Request r = new DistroXV1Request();
        r.setEnvironmentName(envName);

        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(AVAILABLE);
        envResponse.setCrn("crn");

        when(environmentClientService.getByName(envName)).thenReturn(envResponse);

        StackV4Request converted = new StackV4Request();
        when(stackRequestConverter.convert(r)).thenReturn(converted);
        when(stackViewService.findDatalakeViewByEnvironmentCrn(anyString())).thenReturn(Optional.of(createDlStackView(Status.AVAILABLE)));

        underTest.post(r);

        verify(environmentClientService, calledOnce()).getByName(any());
        verify(environmentClientService, calledOnce()).getByName(envName);
        verify(stackOperations, calledOnce()).post(any(), any(), anyBoolean());
        verify(stackOperations, calledOnce()).post(USER_ID, converted, true);
        verify(workspaceService, calledOnce()).getForCurrentUser();
        verify(stackRequestConverter, calledOnce()).convert(any(DistroXV1Request.class));
        verify(stackRequestConverter, calledOnce()).convert(r);
    }

    @Test
    public void testIfDlIsNotExists() {
        String envName = "someAwesomeEnvironment";
        DistroXV1Request request = new DistroXV1Request();
        request.setEnvironmentName(envName);
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(AVAILABLE);
        envResponse.setCrn("crn");
        when(environmentClientService.getByName(envName)).thenReturn(envResponse);
        when(stackViewService.findDatalakeViewByEnvironmentCrn(anyString())).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> underTest.post(request));

        verify(stackViewService).findDatalakeViewByEnvironmentCrn(anyString());
    }

    @Test
    public void testIfDlIsNotRunning() throws IllegalAccessException {
        String envName = "someAwesomeEnvironment";
        DistroXV1Request request = new DistroXV1Request();
        request.setEnvironmentName(envName);
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(AVAILABLE);
        envResponse.setCrn("crn");
        when(environmentClientService.getByName(envName)).thenReturn(envResponse);
        when(stackViewService.findDatalakeViewByEnvironmentCrn(anyString())).thenReturn(Optional.of(createDlStackView(Status.CREATE_IN_PROGRESS)));

        assertThrows(BadRequestException.class, () -> underTest.post(request));

        verify(stackViewService).findDatalakeViewByEnvironmentCrn(anyString());
    }

    @Test
    public void testIfDlIsRunning() throws IllegalAccessException {
        String envName = "someAwesomeEnvironment";
        DistroXV1Request request = new DistroXV1Request();
        request.setEnvironmentName(envName);
        DetailedEnvironmentResponse envResponse = new DetailedEnvironmentResponse();
        envResponse.setEnvironmentStatus(AVAILABLE);
        envResponse.setCrn("crn");
        when(environmentClientService.getByName(envName)).thenReturn(envResponse);
        when(stackViewService.findDatalakeViewByEnvironmentCrn(anyString())).thenReturn(Optional.of(createDlStackView(Status.AVAILABLE)));

        underTest.post(request);

        verify(stackViewService).findDatalakeViewByEnvironmentCrn(anyString());
    }

    private StackView createDlStackView(Status status) throws IllegalAccessException {
        StackView stack = new StackView();
        stack.setType(StackType.DATALAKE);
        StackStatusView stackStatusView = new StackStatusView();
        stackStatusView.setStatus(status);
        FieldUtils.writeField(stack, "stackStatus", stackStatusView, true);
        return stack;
    }

    private static VerificationMode calledOnce() {
        return times(1);
    }

}