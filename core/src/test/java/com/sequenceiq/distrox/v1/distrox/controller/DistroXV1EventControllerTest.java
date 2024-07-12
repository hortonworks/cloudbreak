package com.sequenceiq.distrox.v1.distrox.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventV4Response;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.facade.CloudbreakEventsFacade;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
public class DistroXV1EventControllerTest {
    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private StackService stackService;

    @Mock
    private CloudbreakEventsFacade cloudbreakEventsFacade;

    @InjectMocks
    private DistroXV1EventController underTest;

    @Test
    public void testGetAuditEvents() {
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        when(workspaceService.getForCurrentUser()).thenReturn(workspace);
        StackView stackView = new StackView();
        stackView.setId(2L);
        stackView.setType(StackType.WORKLOAD);
        when(stackService.getViewByCrnInWorkspace(anyString(), anyLong())).thenReturn(stackView);
        when(cloudbreakEventsFacade.retrieveLastEventsByStack(anyLong(), any(), anyInt()))
                .thenReturn(List.of(new CloudbreakEventV4Response()));

        underTest.getAuditEvents("crn", 0, 100);

        verify(stackService).getViewByCrnInWorkspace("crn", 1L);
        verify(cloudbreakEventsFacade).retrieveLastEventsByStack(2L, StackType.WORKLOAD, 100);
    }
}