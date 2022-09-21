package com.sequenceiq.environment.environment.flow.modify.proxy.action;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationContext;
import com.sequenceiq.environment.environment.flow.modify.proxy.EnvProxyModificationState;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;

@Component("ProxyConfigModificationFinishedStateAction")
public class ProxyConfigModificationFinishedStateAction extends AbstractEnvProxyModificationAction<EnvProxyModificationDefaultEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyConfigModificationFinishedStateAction.class);

    private final EnvironmentStatusUpdateService environmentStatusUpdateService;

    private final UsageReporter usageReporter;

    public ProxyConfigModificationFinishedStateAction(EnvironmentStatusUpdateService environmentStatusUpdateService, UsageReporter usageReporter) {
        super(EnvProxyModificationDefaultEvent.class);
        this.environmentStatusUpdateService = environmentStatusUpdateService;
        this.usageReporter = usageReporter;
    }

    @Override
    protected void doExecute(EnvProxyModificationContext context, EnvProxyModificationDefaultEvent payload, Map<Object, Object> variables) {
        LOGGER.info("Env proxy modification flow finished successfully");

        environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                ResourceEvent.ENVIRONMENT_PROXY_CONFIG_MODIFICATION_FINISHED, EnvProxyModificationState.PROXY_CONFIG_MODIFICATION_FINISHED_STATE);
        reportSuccess(context, payload);

        sendEvent(context, EnvProxyModificationStateSelectors.FINALIZE_MODIFY_PROXY_EVENT.selector(), payload);
    }

    private void reportSuccess(EnvProxyModificationContext context, EnvProxyModificationDefaultEvent payload) {
        UsageProto.CDPEnvironmentProxyConfigEditEvent event = UsageProto.CDPEnvironmentProxyConfigEditEvent.newBuilder()
                .setEnvironmentCrn(payload.getResourceCrn())
                .setProxyConfigCrn(getProxyConfigCrn(payload.getProxyConfig()))
                .setPreviousProxyConfigCrn(getProxyConfigCrn(context.getPreviousProxyConfig()))
                .setResult(UsageProto.CDPEnvironmentProxyConfigEditResult.Value.SUCCESS)
                .build();
        usageReporter.cdpEnvironmentProxyConfigEditEvent(event);
    }
}
