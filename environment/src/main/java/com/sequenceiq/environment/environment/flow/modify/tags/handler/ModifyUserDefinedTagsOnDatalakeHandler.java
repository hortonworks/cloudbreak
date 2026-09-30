package com.sequenceiq.environment.environment.flow.modify.tags.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_ON_DATALAKE_FAILED;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_DATALAKE_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.START_MODIFY_USER_DEFINED_TAGS_DATAHUBS_EVENT;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.service.stack.StackPollerService;

@Component
public class ModifyUserDefinedTagsOnDatalakeHandler extends ModifyUserDefinedTagsOnStacksHandler {

    public ModifyUserDefinedTagsOnDatalakeHandler(StackPollerService stackPollerService) {
        super(stackPollerService);
    }

    @Override
    public String selector() {
        return MODIFY_USER_DEFINED_TAGS_ON_DATALAKE_EVENT.selector();
    }

    @Override
    protected StackType stackType() {
        return StackType.DATALAKE;
    }

    @Override
    protected EnvironmentStatus failureStatus() {
        return USER_DEFINED_TAGS_MODIFICATION_ON_DATALAKE_FAILED;
    }

    @Override
    protected String nextSelector() {
        return START_MODIFY_USER_DEFINED_TAGS_DATAHUBS_EVENT.name();
    }

    @Override
    protected String failureLogMessage() {
        return "Modify user defined tags on Data Lake failed.";
    }
}
