package com.sequenceiq.datalake.maintenancewindow;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.maintenance.api.execution.MaintenanceWindowTaskExecuteHandler;
import com.sequenceiq.sdx.api.v1.maintenance.model.MaintenanceTaskDispatchRequest;

/**
 * Routes maintenance window dispatch requests to the {@link MaintenanceWindowTaskExecuteHandler} registered for {@code task_type}.
 */
@Service
public class MaintenanceWindowTaskExecuteService {

    @Inject
    private List<MaintenanceWindowTaskExecuteHandler<MaintenanceTaskDispatchRequest>> handlers;

    private Map<String, MaintenanceWindowTaskExecuteHandler<MaintenanceTaskDispatchRequest>> handlersByTaskType;

    @PostConstruct
    void buildHandlerMap() {
        handlersByTaskType = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(MaintenanceWindowTaskExecuteHandler::taskType, Function.identity()));
    }

    public FlowIdentifier execute(MaintenanceTaskDispatchRequest request) {
        MaintenanceWindowTaskExecuteHandler<MaintenanceTaskDispatchRequest> handler = handlersByTaskType.get(request.getTaskType());
        if (handler == null) {
            throw new BadRequestException("Unsupported task_type: " + request.getTaskType());
        }
        return handler.execute(request);
    }
}
