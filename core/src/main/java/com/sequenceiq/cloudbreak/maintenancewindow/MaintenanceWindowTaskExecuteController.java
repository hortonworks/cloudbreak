package com.sequenceiq.cloudbreak.maintenancewindow;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.api.v1.maintenance.endpoint.MaintenanceWindowTaskExecuteEndpoint;
import com.sequenceiq.cloudbreak.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;

@Controller
public class MaintenanceWindowTaskExecuteController implements MaintenanceWindowTaskExecuteEndpoint {

    private final MaintenanceWindowTaskExecuteService executeService;

    @Inject
    public MaintenanceWindowTaskExecuteController(MaintenanceWindowTaskExecuteService executeService) {
        this.executeService = executeService;
    }

    @Override
    @InternalOnly
    public Response execute(@RequestObject MaintenanceTaskDispatchRequest request) {
        return Response.status(Response.Status.ACCEPTED).entity(executeService.execute(request)).build();
    }
}
