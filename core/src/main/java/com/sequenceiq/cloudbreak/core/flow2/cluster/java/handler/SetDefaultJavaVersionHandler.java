package com.sequenceiq.cloudbreak.core.flow2.cluster.java.handler;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.java.SetDefaultJavaVersionFlowEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class SetDefaultJavaVersionHandler extends ExceptionCatcherEventHandler<SetDefaultJavaVersionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetDefaultJavaVersionHandler.class);

    private static final String DEFAULT_JAVA_VERSION_EXECUTED_FILE = "/var/log/set-default-java-version-executed";

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private StackUpdater stackUpdater;

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<SetDefaultJavaVersionRequest> event) {
        return new SetDefaultJavaVersionFailedEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_FAILED_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<SetDefaultJavaVersionRequest> event) {
        SetDefaultJavaVersionRequest request = event.getData();
        Long stackId = request.getResourceId();
        StackDto stackDto = stackDtoService.getById(stackId);
        List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stackDto);
        Set<String> allNodes = stackUtil.collectNodes(stackDto).stream().map(Node::getHostname).collect(Collectors.toSet());
        try {
            LOGGER.info("Setting default java version for stack to {}", request.getDefaultJavaVersion());
            stackUpdater.updateJavaVersion(stackId, request.getDefaultJavaVersion());
            LOGGER.info("Removing " + DEFAULT_JAVA_VERSION_EXECUTED_FILE + " file from all hosts");
            hostOrchestrator.runCommandOnHosts(allGatewayConfigs, allNodes, "rm " + DEFAULT_JAVA_VERSION_EXECUTED_FILE);
        } catch (CloudbreakOrchestratorException e) {
            return new SetDefaultJavaVersionFailedEvent(
                    SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_FAILED_EVENT.event(), stackId, e);
        }
        return new SetDefaultJavaVersionResult(request);
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SetDefaultJavaVersionRequest.class);
    }
}
