package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.converter.builder.ResultServiceEventDataBuilderProvider;
import com.sequenceiq.cloudbreak.audit.model.ResultServiceEventData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.doIfTrue;

@Component
public class ResultServiceEventDataBuildUpdater implements AttemptAuditEventResultBuilderUpdater<ResultServiceEventData> {

    private final ResultServiceEventDataBuilderProvider builderProvider;

    public ResultServiceEventDataBuildUpdater(ResultServiceEventDataBuilderProvider builderProvider) {
        this.builderProvider = builderProvider;
    }

    @Override
    public void update(AuditProto.AttemptAuditEventResult.Builder builder, ResultServiceEventData resultEventData) {
        AuditProto.ResultServiceEventData.Builder resultServiceEventDataBuilder = builderProvider.getNewResultServiceEventDataBuilder()
                .addAllResourceCrn(resultEventData.getResourceCrns());
        doIfTrue(resultEventData.getResultDetails(), StringUtils::isNotEmpty, resultServiceEventDataBuilder::setResultDetails);

        builder.setResultServiceEventData(resultServiceEventDataBuilder.build());
    }

    @Override
    public Class<ResultServiceEventData> getType() {
        return ResultServiceEventData.class;
    }

}
