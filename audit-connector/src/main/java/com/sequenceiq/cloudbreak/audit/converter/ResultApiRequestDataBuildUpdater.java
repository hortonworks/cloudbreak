package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.converter.builder.ResultApiRequestDataBuilderProvider;
import com.sequenceiq.cloudbreak.audit.model.ResultApiRequestData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.doIfTrue;

@Component
public class ResultApiRequestDataBuildUpdater implements AttemptAuditEventResultBuilderUpdater<ResultApiRequestData> {

    private final ResultApiRequestDataBuilderProvider builderProvider;

    public ResultApiRequestDataBuildUpdater(ResultApiRequestDataBuilderProvider builderProvider) {
        this.builderProvider = builderProvider;
    }

    @Override
    public void update(AuditProto.AttemptAuditEventResult.Builder builder, ResultApiRequestData resultEventData) {
        AuditProto.ResultApiRequestData.Builder resultApiRequestDataBuilder = builderProvider.getNewResultApiRequestDataBuilder();
        doIfTrue(resultEventData.getResponseParameters(), StringUtils::isNotEmpty, resultApiRequestDataBuilder::setResponseParameters);

        builder.setResultApiRequestData(resultApiRequestDataBuilder.build());
    }

    @Override
    public Class<ResultApiRequestData> getType() {
        return ResultApiRequestData.class;
    }

}
