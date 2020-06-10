package com.sequenceiq.cloudbreak.audit.converter;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.converter.builder.ApiRequestDataBuilderProvider;
import com.sequenceiq.cloudbreak.audit.model.ApiRequestData;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.sequenceiq.cloudbreak.util.ConditionBasedEvaluatorUtil.doIfTrue;

@Component
public class ApiRequestDataBuildUpdater implements AuditEventBuilderUpdater<ApiRequestData> {

    private final ApiRequestDataBuilderProvider builderProvider;

    public ApiRequestDataBuildUpdater(ApiRequestDataBuilderProvider builderProvider) {
        this.builderProvider = builderProvider;
    }

    @Override
    public void update(AuditProto.AuditEvent.Builder auditEventBuilder, ApiRequestData eventData) {
        AuditProto.ApiRequestData.Builder apiRequestDataBuilder = builderProvider.getNewApiRequestDataBuilder()
                .setMutating(eventData.isMutating());
        doIfTrue(eventData.getApiVersion(), StringUtils::isNotEmpty, apiRequestDataBuilder::setApiVersion);
        doIfTrue(eventData.getRequestParameters(), StringUtils::isNotEmpty, apiRequestDataBuilder::setRequestParameters);
        doIfTrue(eventData.getUserAgent(), StringUtils::isNotEmpty, apiRequestDataBuilder::setUserAgent);
        auditEventBuilder.setApiRequestData(apiRequestDataBuilder.build());
    }

    @Override
    public Class<ApiRequestData> getType() {
        return ApiRequestData.class;
    }

}
