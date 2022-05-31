package com.sequenceiq.distrox.v1.distrox.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ChangeImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXGenerateImageCatalogV1Response;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.distrox.v1.distrox.authorization.DataHubFiltering;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@ExtendWith(MockitoExtension.class)
class DistroXV1ControllerTest {

    private static final String CRN = "crn";

    private static final String NAME = "name";

    private static final String IMAGE_CATALOG = "image catalog";

    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private DataHubFiltering datahubFiltering;

    @Mock
    private StackOperations stackOperations;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private Workspace workspace;

    @Mock
    private StackOperationService stackOperationService;

    @Captor
    private ArgumentCaptor<NameOrCrn> nameOrCrnArgumentCaptor;

    @InjectMocks
    private DistroXV1Controller distroXV1Controller;

    @Test
    void testListUsesListAuthorizationService() {
        StackViewV4Responses expected = new StackViewV4Responses();
        when(datahubFiltering.filterDataHubs(any(), anyString(), anyString())).thenReturn(expected);

        StackViewV4Responses actual = distroXV1Controller.list(NAME, CRN);

        assertEquals(expected, actual);
        verify(datahubFiltering).filterDataHubs(AuthorizationResourceAction.DESCRIBE_DATAHUB, NAME, CRN);
    }

    @Test
    void testChangeImageCatalog() {
        ChangeImageCatalogV4Request request = new ChangeImageCatalogV4Request();
        request.setImageCatalog(IMAGE_CATALOG);
        when(workspaceService.getForCurrentUser()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);

        distroXV1Controller.changeImageCatalog(NAME, request);

        verify(stackOperations).changeImageCatalog(nameOrCrnArgumentCaptor.capture(), Mockito.eq(WORKSPACE_ID), Mockito.eq(IMAGE_CATALOG));
        assertEquals(NAME, nameOrCrnArgumentCaptor.getValue().getName());
    }

    @Test
    void testGenerateImageCatalog() {
        CloudbreakImageCatalogV3 imageCatalog = Mockito.mock(CloudbreakImageCatalogV3.class);

        when(workspaceService.getForCurrentUser()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(stackOperations.generateImageCatalog(nameOrCrnArgumentCaptor.capture(), Mockito.eq(WORKSPACE_ID))).thenReturn(imageCatalog);

        DistroXGenerateImageCatalogV1Response actual = distroXV1Controller.generateImageCatalog(NAME);

        assertEquals(NAME, nameOrCrnArgumentCaptor.getValue().getName());
        assertEquals(imageCatalog, actual.getImageCatalog());
    }

    @Test
    void testRenewInternalCertificate() {
        FlowIdentifier flowIdentifier = Mockito.mock(FlowIdentifier.class);
        Stack stack = Mockito.mock(Stack.class);

        when(stackOperations.getStackByCrn(anyString())).thenReturn(stack);
        when(stackOperationService.renewCertificate(anyLong())).thenReturn(flowIdentifier);

        FlowIdentifier actual = distroXV1Controller.renewCertificate(CRN);

        assertEquals(actual, flowIdentifier);
    }

    @Test
    void testRotateSaltPasswordByCrn() {
        when(workspaceService.getForCurrentUser()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);

        distroXV1Controller.rotateSaltPasswordByCrn(CRN);

        verify(stackOperations).rotateSaltPassword(NameOrCrn.ofCrn(CRN), WORKSPACE_ID);
    }
}