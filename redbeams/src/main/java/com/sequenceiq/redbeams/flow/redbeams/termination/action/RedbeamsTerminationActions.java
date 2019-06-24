package com.sequenceiq.redbeams.flow.redbeams.termination.action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.AbstractRedbeamsTerminationAction;
import com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerSuccess;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate.TerminateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate.TerminateDatabaseServerSuccess;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

@Configuration
public class RedbeamsTerminationActions {

    // FIXME should the database be deregistered before it is terminated?

    @Bean(name = "TERMINATE_DATABASE_SERVER_STATE")
    public Action<?, ?> terminateDatabaseServer() {
        return new AbstractRedbeamsTerminationAction<>(RedbeamsEvent.class) {

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new TerminateDatabaseServerRequest(context.getCloudContext(), context.getCloudCredential(),
                        context.getDatabaseStack());
            }
        };
    }

    @Bean(name = "DEREGISTER_DATABASE_SERVER_STATE")
    public Action<?, ?> deregisterDatabaseServer() {
        return new AbstractRedbeamsTerminationAction<>(TerminateDatabaseServerSuccess.class) {

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new DeregisterDatabaseServerRequest(0L);
            }
        };
    }

    @Bean(name = "REDBEAMS_TERMINATION_FINISHED_STATE")
    public Action<?, ?> terminationFinished() {
        return new AbstractRedbeamsTerminationAction<>(DeregisterDatabaseServerSuccess.class) {
            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new RedbeamsEvent(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_FINISHED_EVENT.name(), 0L);
            }
        };
    }
}
