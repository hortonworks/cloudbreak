package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.distrox.v1.distrox.StackOperations;

@ExtendWith(MockitoExtension.class)
class DistroXV1ControllerTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String CRN = "crn";

    private static final String NAME = "name";

    @Mock
    private StackOperations stackOperations;

    @Mock
    private WorkspaceService workspaceService;

    @InjectMocks
    private DistroXV1Controller distroXV1Controller;

    @BeforeEach
    void setup() {
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        when(workspaceService.getForCurrentUser()).thenReturn(workspace);
    }

    @Test
    void testListWithBothParamsNull() {
        distroXV1Controller.list(null, null);

        verify(stackOperations, times(1)).listByEnvironmentCrn(WORKSPACE_ID, null, WORKLOAD);
    }

    @Test
    void testListWithBothParamsSet() {
        distroXV1Controller.list(NAME, CRN);

        verify(stackOperations, times(1)).listByEnvironmentCrn(WORKSPACE_ID, CRN, WORKLOAD);
    }

    @Test
    void testListWithNameSetAndCrnIsNull() {
        distroXV1Controller.list(NAME, null);

        verify(stackOperations, times(1)).listByEnvironmentName(WORKSPACE_ID, NAME, WORKLOAD);
    }

    @Test
    void testListWithCrnSetAndNameIsNull() {
        distroXV1Controller.list(null, CRN);

        verify(stackOperations, times(1)).listByEnvironmentCrn(WORKSPACE_ID, CRN, WORKLOAD);
    }
}