package com.sequenceiq.freeipa.flow.freeipa.diagnostics.handler;

import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionHandlerSelectors.PREFLIGHT_CHECK_DIAGNOSTICS_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_INIT_EVENT;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.freeipa.service.stack.FreeIpaNodeStatusService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

import reactor.bus.Event;

@Component
public class DiagnosticsPreFlightCheckHandler extends EventSenderAwareHandler<DiagnosticsCollectionEvent>  {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsPreFlightCheckHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private FreeIpaNodeStatusService freeIpaNodeStatusService;

    public DiagnosticsPreFlightCheckHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public String selector() {
        return PREFLIGHT_CHECK_DIAGNOSTICS_EVENT.selector();
    }

    @Override
    public void accept(Event<DiagnosticsCollectionEvent> event) {
        DiagnosticsCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        DiagnosticParameters parameters = data.getParameters();
        Stack stack = stackService.getByIdWithListsInTransaction(resourceId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(resourceId);
        instanceMetaDataSet.stream()
                .filter(im -> im.getInstanceMetadataType() == InstanceMetadataType.GATEWAY_PRIMARY)
                .forEach(instance -> executeNetworkReport(stack, instance));
        DiagnosticsCollectionEvent diagnosticsCollectionEvent = DiagnosticsCollectionEvent.builder()
                .withResourceCrn(resourceCrn)
                .withResourceId(resourceId)
                .withSelector(START_DIAGNOSTICS_INIT_EVENT.selector())
                .withParameters(parameters)
                .build();
        eventSender().sendEvent(diagnosticsCollectionEvent, event.getHeaders());
    }

    private void executeNetworkReport(Stack stack, InstanceMetaData instance) {
        try {
            RPCResponse<NodeStatusProto.NodeStatusReport> rpcResponse = freeIpaNodeStatusService.nodeNetworkReport(stack, instance);
            if (rpcResponse != null) {
                LOGGER.debug("Diagnostics PreFlight check result (network): \n{}", rpcResponse.getFirstTextMessage());
            }
        } catch (FreeIpaClientException e) {
            LOGGER.debug("Error occurred during fetching data from nodestatus monitor: {}", e.getMessage());
        }
    }
}
