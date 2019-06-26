package com.sequenceiq.redbeams.flow.redbeams.provision.action;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.provision.AbstractRedbeamsProvisionAction;
import com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerSuccess;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.RegisterDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.RegisterDatabaseServerSuccess;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

@Configuration
public class RedbeamsProvisionActions {

    @Bean(name = "ALLOCATE_DATABASE_SERVER_STATE")
    public Action<?, ?> allocateDatabaseServer() {
        return new AbstractRedbeamsProvisionAction<>(RedbeamsEvent.class) {

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new AllocateDatabaseServerRequest(context.getCloudContext(), context.getCloudCredential(),
                        context.getDatabaseStack());
            }
        };
    }

    @Bean(name = "REGISTER_DATABASE_SERVER_STATE")
    public Action<?, ?> registerDatabaseServer() {
        return new AbstractRedbeamsProvisionAction<>(AllocateDatabaseServerSuccess.class) {

            private List<CloudResource> dbResources;

            @Override
            protected void prepareExecution(AllocateDatabaseServerSuccess payload, Map<Object, Object> variables) {
                dbResources = ResourceLists.transform(payload.getResults());
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new RegisterDatabaseServerRequest(context.getCloudContext(), context.getDBStack(), dbResources);
            }
        };
    }

    @Bean(name = "REDBEAMS_PROVISION_FINISHED_STATE")
    public Action<?, ?> provisionFinished() {
        return new AbstractRedbeamsProvisionAction<>(RegisterDatabaseServerSuccess.class) {
            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new RedbeamsEvent(RedbeamsProvisionEvent.REDBEAMS_PROVISION_FINISHED_EVENT.name(), 0L);
            }
        };
    }
}
