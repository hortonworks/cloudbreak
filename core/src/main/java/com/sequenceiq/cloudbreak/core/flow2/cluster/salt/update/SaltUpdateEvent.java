package com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update;

import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.kerberos.KeytabConfigurationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.BootstrapMachinesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartClusterManagerServicesSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.UploadRecipesSuccess;
import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public enum SaltUpdateEvent implements FlowEvent {

    SALT_UPDATE_EVENT("SALT_UPDATE_TRIGGER_EVENT"),
    BOOTSTRAP_MACHINES_FINISHED_EVENT(EventSelectorUtil.selector(BootstrapMachinesSuccess.class)),
    BOOTSTRAP_MACHINES_FAILED_EVENT(EventSelectorUtil.selector(BootstrapMachinesFailed.class)),
    UPLOAD_RECIPES_FINISHED_EVENT(EventSelectorUtil.selector(UploadRecipesSuccess.class)),
    UPLOAD_RECIPES_FAILED_EVENT(EventSelectorUtil.selector(UploadRecipesFailed.class)),
    CONFIGURE_KEYTABS_FINISHED_EVENT(EventSelectorUtil.selector(KeytabConfigurationSuccess.class)),
    CONFIGURE_KEYTABS_FAILED_EVENT(EventSelectorUtil.selector(KeytabConfigurationFailed.class)),
    START_AMBARI_SERVICES_FINISHED_EVENT(EventSelectorUtil.selector(StartClusterManagerServicesSuccess.class)),
    START_AMBARI_SERVICES_FAILED_EVENT(EventSelectorUtil.selector(StartAmbariServicesFailed.class)),
    SALT_UPDATE_FAILED_EVENT("SALT_UPDATE_FAILED"),
    SALT_UPDATE_FINISHED_EVENT("SALT_UPDATE_FINISHED"),
    SALT_UPDATE_FAILURE_HANDLED_EVENT("SALT_UPDATE_FAIL_HANDLED");

    private final String event;

    SaltUpdateEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
