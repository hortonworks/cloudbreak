package com.sequenceiq.cloudbreak.usage.strategy;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequest;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequestContext;
import com.sequenceiq.cloudbreak.usage.messagebroker.MessageBrokerDatabusRecordProcessor;
import com.sequenceiq.cloudbreak.usage.model.UsageContext;

@Service
public class MessageBrokerUsageStrategy implements UsageProcessingStrategy {

    private final MessageBrokerDatabusRecordProcessor messageBrokerDatabusRecordProcessor;

    public MessageBrokerUsageStrategy(MessageBrokerDatabusRecordProcessor messageBrokerDatabusRecordProcessor) {
        this.messageBrokerDatabusRecordProcessor = messageBrokerDatabusRecordProcessor;
    }

    @Override
    public void processUsage(UsageProto.Event event, UsageContext context) {
        DatabusRequestContext dbusContext = DatabusRequestContext.Builder.newBuilder()
                .withAccountId(getAccountId(context))
                .withAdditionalDatabusHeaders(messageBrokerDatabusRecordProcessor.getOptionalUsageHeaders())
                .build();
        DatabusRequest databusRequest = DatabusRequest.Builder.newBuilder()
                .withMessageBody(event)
                .withContext(dbusContext)
                .build();
        messageBrokerDatabusRecordProcessor.processRecord(databusRequest);
    }

    private String getAccountId(UsageContext context) {
        if (context != null && StringUtils.isNotBlank(context.getAccountId())) {
            return context.getAccountId();
        } else {
            return Crn.safeFromString(ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN).getAccountId();
        }
    }
}
