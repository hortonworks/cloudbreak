package com.sequenceiq.environment.environment.flow.modify.proxy.action;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationContext;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationFailedEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationHandlerSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;

@Component("ProxyConfigModificationFailedStateAction")
public class ProxyConfigModificationFailedStateAction extends AbstractEnvProxyModificationAction<EnvProxyModificationFailedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigModificationFailedStateAction.class);

    @Inject
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @Inject
    private UsageReporter usageReporter;

    public ProxyConfigModificationFailedStateAction() {
        super(EnvProxyModificationFailedEvent.class);
    }

    @Override
    protected void doExecute(EnvProxyModificationContext context, EnvProxyModificationFailedEvent payload, Map<Object, Object> variables) {
        LOGGER.warn("Env proxy modification flow finished with an error", payload.getException());

        environmentStatusUpdateService.updateFailedEnvironmentStatusAndNotify(context, payload, payload.getEnvironmentStatus(),
                ResourceEvent.ENVIRONMENT_PROXY_CONFIG_MODIFICATION_FAILED, List.of(getProxyConfigName(context), payload.getException().getMessage()),
                EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_FAILED_STATE);
        reportFailure(payload);

        sendEvent(context, EnvProxyModificationHandlerSelectors.REVERT_PROXY_ASSOCIATION_HANDLER_EVENT.event(), payload);
    }

    private void reportFailure(EnvProxyModificationFailedEvent payload) {
        UsageProto.CDPEnvironmentProxyConfigEditEvent event = UsageProto.CDPEnvironmentProxyConfigEditEvent.newBuilder()
                .setEnvironmentCrn(payload.getResourceCrn())
                .setProxyConfigCrn(Objects.requireNonNullElse(payload.getProxyConfigCrn(), ""))
                .setPreviousProxyConfigCrn(Objects.requireNonNullElse(payload.getPreviousProxyConfigCrn(), ""))
                .setResult(UsageProto.CDPEnvironmentProxyConfigEditResult.Value.ENVIRONMENT_FAILURE)
                .setMessage(Objects.requireNonNullElse(payload.getException().getMessage(), ""))
                .build();
        usageReporter.cdpEnvironmentProxyConfigEditEvent(event);
    }
}
