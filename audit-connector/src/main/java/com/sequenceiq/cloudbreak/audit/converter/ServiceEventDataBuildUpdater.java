package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.converter.builder.ServiceEventDataBuilderProvider;
import com.sequenceiq.cloudbreak.audit.model.ServiceEventData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.doIfTrue;

@Component
public class ServiceEventDataBuildUpdater implements AuditEventBuilderUpdater<ServiceEventData> {

    private final ServiceEventDataBuilderProvider builderProvider;

    public ServiceEventDataBuildUpdater(ServiceEventDataBuilderProvider builderProvider) {
        this.builderProvider = builderProvider;
    }

    @Override
    public void update(AuditProto.AuditEvent.Builder auditEventBuilder, ServiceEventData eventData) {
        AuditProto.ServiceEventData.Builder serviceEventDataBuilder = builderProvider.getNewServiceEventDataBuilder();
        doIfTrue(eventData.getEventDetails(), StringUtils::isNotEmpty, serviceEventDataBuilder::setEventDetails);
        doIfTrue(eventData.getVersion(), StringUtils::isNotEmpty, serviceEventDataBuilder::setDetailsVersion);
        auditEventBuilder.setServiceEventData(serviceEventDataBuilder.build());
    }

    @Override
    public Class<ServiceEventData> getType() {
        return ServiceEventData.class;
    }

}
