package com.sequenceiq.redbeams.flow.redbeams.provision.action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.flow.redbeams.provision.AbstractRedbeamsProvisionAction;
import com.sequenceiq.redbeams.flow.redbeams.provision.RedbeamsProvisionEvent;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.allocate.AllocateDatabaseServerSuccess;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.RegisterDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.provision.event.register.RegisterDatabaseServerSuccess;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

@Configuration
public class RedbeamsProvisionActions {

    @Bean(name = "ALLOCATE_DATABASE_STATE")
    public Action<?, ?> allocateDatabase() {
        return new AbstractRedbeamsProvisionAction<>(RedbeamsEvent.class) {

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new AllocateDatabaseServerRequest(context.getCloudContext(), context.getCloudCredential(),
                        context.getDatabaseStack());
            }
        };
    }

    @Bean(name = "REGISTER_DATABASE_STATE")
    public Action<?, ?> registerDatabase() {
        return new AbstractRedbeamsProvisionAction<>(AllocateDatabaseServerSuccess.class) {

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new RegisterDatabaseServerRequest(0L);
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
