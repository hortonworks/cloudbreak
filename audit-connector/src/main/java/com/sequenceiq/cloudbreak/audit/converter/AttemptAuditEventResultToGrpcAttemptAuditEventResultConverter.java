package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.converter.builder.AttemptAuditEventResultBuilderProvider;
import com.sequenceiq.cloudbreak.audit.model.AttemptAuditEventResult;
import com.sequenceiq.cloudbreak.audit.model.ResultEventData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.doIfTrue;

@Component
public class AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter.class);

    private final Map<Class, AttemptAuditEventResultBuilderUpdater> builderUpdaters;

    private final AttemptAuditEventResultBuilderProvider builderProvider;

    public AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter(Map<Class, AttemptAuditEventResultBuilderUpdater> builderUpdaters,
                    AttemptAuditEventResultBuilderProvider builderProvider) {
        this.builderUpdaters = builderUpdaters;
        this.builderProvider = builderProvider;
    }

    public AuditProto.AttemptAuditEventResult convert(AttemptAuditEventResult source) {
        AuditProto.AttemptAuditEventResult.Builder builder = builderProvider.getNewAttemptAuditEventResultBuilder()
                .setId(source.getId())
                .setResultCode(source.getResultCode());
        doIfTrue(source.getResultMessage(), StringUtils::isNotEmpty, builder::setResultMessage);
        updateResultEventData(builder, source.getResultEventData());
        return builder.build();
    }

    private void updateResultEventData(AuditProto.AttemptAuditEventResult.Builder auditEventBuilder, ResultEventData source) {
        if (source == null) {
            LOGGER.debug("No ResultEventData has provided to update AuditEventData hence no operation will be done.");
            return;
        }
        if (builderUpdaters.containsKey(source.getClass())) {
            builderUpdaters.get(source.getClass()).update(auditEventBuilder, source);
        } else {
            throw new IllegalArgumentException("ResultEventData has an invalid class: " + source.getClass().getName());
        }
    }

}
