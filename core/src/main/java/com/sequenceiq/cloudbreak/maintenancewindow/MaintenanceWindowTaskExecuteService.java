package com.sequenceiq.cloudbreak.maintenancewindow;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.maintenancewindow.handler.MaintenanceWindowTaskExecuteHandler;
import com.sequenceiq.flow.api.model.FlowIdentifier;

/**
 * Routes maintenance window dispatch requests to the {@link MaintenanceWindowTaskExecuteHandler} registered for {@code task_type}.
 */
@Service
public class MaintenanceWindowTaskExecuteService {

    private final Map<String, MaintenanceWindowTaskExecuteHandler> handlersByTaskType;

    @Inject
    public MaintenanceWindowTaskExecuteService(List<MaintenanceWindowTaskExecuteHandler> handlers) {
        this.handlersByTaskType = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(MaintenanceWindowTaskExecuteHandler::taskType, Function.identity()));
    }

    public FlowIdentifier execute(MaintenanceTaskDispatchRequest request) {
        MaintenanceWindowTaskExecuteHandler handler = handlersByTaskType.get(request.getTaskType());
        if (handler == null) {
            throw new BadRequestException("Unsupported task_type: " + request.getTaskType());
        }
        return handler.execute(request);
    }
}
