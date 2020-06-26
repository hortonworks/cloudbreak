package com.sequenceiq.cloudbreak.audit.converter;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.doIfTrue;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.AttemptAuditEventResult;
import com.sequenceiq.cloudbreak.audit.model.ResultApiRequestData;
import com.sequenceiq.cloudbreak.audit.model.ResultEventData;
import com.sequenceiq.cloudbreak.audit.model.ResultServiceEventData;

@Component
public class AttemptAuditEventResultToGrpcAttemptAuditEventResultConverter {

    public AuditProto.AttemptAuditEventResult convert(AttemptAuditEventResult source) {
        AuditProto.AttemptAuditEventResult.Builder attemptAuditEventResultBuilder = prepareBuilderForCreateAuditEvent(source);
        updatResultEventData(attemptAuditEventResultBuilder, source.getResultEventData());
        return attemptAuditEventResultBuilder.build();
    }

    private AuditProto.AttemptAuditEventResult.Builder prepareBuilderForCreateAuditEvent(AttemptAuditEventResult source) {
        AuditProto.AttemptAuditEventResult.Builder builder = AuditProto.AttemptAuditEventResult.newBuilder()
                .setId(source.getId())
                .setResultCode(source.getResultCode());
        doIfTrue(source.getResultMessage(), StringUtils::isNotEmpty, builder::setResultMessage);
        return builder;
    }

    private void updatResultEventData(AuditProto.AttemptAuditEventResult.Builder auditEventBuilder, ResultEventData source) {
        if (source == null) {
            return;
        }

        if (source instanceof ResultServiceEventData) {
            ResultServiceEventData serviceEventData = (ResultServiceEventData) source;
            AuditProto.ResultServiceEventData.Builder resultServiceEventDataBuilder = AuditProto.ResultServiceEventData.newBuilder()
                    .addAllResourceCrn(serviceEventData.getResourceCrns());
            doIfTrue(serviceEventData.getResultDetails(), StringUtils::isNotEmpty, resultServiceEventDataBuilder::setResultDetails);
            auditEventBuilder.setResultServiceEventData(resultServiceEventDataBuilder.build());
        } else if (source instanceof ResultApiRequestData) {
            ResultApiRequestData apiRequestData = (ResultApiRequestData) source;
            AuditProto.ResultApiRequestData.Builder resultApiRequestDataBuilder = AuditProto.ResultApiRequestData.newBuilder();
            doIfTrue(apiRequestData.getResponseParameters(), StringUtils::isNotEmpty, resultApiRequestDataBuilder::setResponseParameters);
            auditEventBuilder.setResultApiRequestData(resultApiRequestDataBuilder.build());
        } else {
            throw new IllegalArgumentException("ResultEventData has an invalid class: " + source.getClass().getName());
        }
    }
}
