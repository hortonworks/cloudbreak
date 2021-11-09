package com.sequenceiq.cloudbreak.controller.v4;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ChangeImageCatalogV4Request;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.distrox.v1.distrox.StackOperations;

@ExtendWith(MockitoExtension.class)
class StackV4ControllerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:test@test.com";

    private static final long WORKSPACE_ID = 1236L;

    private static final String STACK_NAME = "stack name";

    @Mock
    private StackOperations stackOperations;

    @Mock
    private StackOperationService stackOperationService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @InjectMocks
    private StackV4Controller underTest;

    @Test
    void changeImageCatalogInternalTest() {
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);

        String imageCatalog = "image-catalog";
        ChangeImageCatalogV4Request request = new ChangeImageCatalogV4Request();
        request.setImageCatalog(imageCatalog);

        underTest.changeImageCatalogInternal(WORKSPACE_ID, STACK_NAME, USER_CRN, request);

        verify(stackOperations).changeImageCatalog(NameOrCrn.ofName(STACK_NAME), WORKSPACE_ID, imageCatalog);
    }

    @Test
    void rangerRazEnabledTest() {
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);
        String stackCrn = "test-crn";

        underTest.rangerRazEnabledInternal(WORKSPACE_ID, stackCrn, USER_CRN);

        verify(stackOperationService).rangerRazEnabled(WORKSPACE_ID, stackCrn);
    }

    @Test
    void generateImageCatalogInternalTest() {
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(WORKSPACE_ID);

        underTest.generateImageCatalogInternal(WORKSPACE_ID, STACK_NAME, USER_CRN);

        verify(stackOperations).generateImageCatalog(NameOrCrn.ofName(STACK_NAME), WORKSPACE_ID);
    }
}
