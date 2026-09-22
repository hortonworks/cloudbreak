package com.sequenceiq.cloudbreak.maintenancewindow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowTaskExecuteControllerTest {

    @Mock
    private MaintenanceWindowTaskExecuteService executeService;

    @InjectMocks
    private MaintenanceWindowTaskExecuteController underTest;

    @Test
    void executeReturns202AcceptedWithFlowIdentifierFromService() {
        MaintenanceTaskDispatchRequest request = MaintenanceTaskDispatchRequestTestBuilder.aDispatchRequest().build();
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "maintenance-rotation-flow");
        when(executeService.execute(request)).thenReturn(flowIdentifier);

        Response response = underTest.execute(request);

        assertThat(response.getStatus()).isEqualTo(Response.Status.ACCEPTED.getStatusCode());
        assertThat(response.getEntity()).isSameAs(flowIdentifier);
        verify(executeService).execute(request);
    }
}
