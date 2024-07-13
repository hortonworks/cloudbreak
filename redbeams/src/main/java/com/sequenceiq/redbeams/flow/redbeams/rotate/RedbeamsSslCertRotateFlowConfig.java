package com.sequenceiq.redbeams.flow.redbeams.rotate;

import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent.REDBEAMS_SSL_CERT_ROTATE_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent.REDBEAMS_SSL_CERT_ROTATE_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent.REDBEAMS_SSL_CERT_ROTATE_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent.REDBEAMS_SSL_CERT_ROTATE_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent.SSL_CERT_ROTATE_DATABASE_SERVER_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent.SSL_CERT_ROTATE_DATABASE_SERVER_FINISHED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateEvent.values;
import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateState.FINAL_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateState.INIT_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateState.REDBEAMS_SSL_CERT_ROTATE_FAILED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateState.REDBEAMS_SSL_CERT_ROTATE_FINISHED_STATE;
import static com.sequenceiq.redbeams.flow.redbeams.rotate.RedbeamsSslCertRotateState.SSL_CERT_ROTATE_DATABASE_SERVER_STATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.RetryableFlowConfiguration;

@Component
public class RedbeamsSslCertRotateFlowConfig extends AbstractFlowConfiguration<RedbeamsSslCertRotateState, RedbeamsSslCertRotateEvent>
        implements RetryableFlowConfiguration<RedbeamsSslCertRotateEvent> {

    private static final RedbeamsSslCertRotateEvent[] REDBEAMS_INIT_EVENTS = {REDBEAMS_SSL_CERT_ROTATE_EVENT};

    private static final List<Transition<RedbeamsSslCertRotateState, RedbeamsSslCertRotateEvent>> TRANSITIONS =
            new Transition.Builder<RedbeamsSslCertRotateState, RedbeamsSslCertRotateEvent>()
                    .defaultFailureEvent(REDBEAMS_SSL_CERT_ROTATE_FAILED_EVENT)

                    .from(RedbeamsSslCertRotateState.INIT_STATE)
                    .to(SSL_CERT_ROTATE_DATABASE_SERVER_STATE)
                    .event(REDBEAMS_SSL_CERT_ROTATE_EVENT)
                    .defaultFailureEvent()

                    .from(SSL_CERT_ROTATE_DATABASE_SERVER_STATE)
                    .to(REDBEAMS_SSL_CERT_ROTATE_FINISHED_STATE)
                    .event(SSL_CERT_ROTATE_DATABASE_SERVER_FINISHED_EVENT)
                    .failureEvent(SSL_CERT_ROTATE_DATABASE_SERVER_FAILED_EVENT)

                    .from(REDBEAMS_SSL_CERT_ROTATE_FINISHED_STATE)
                    .to(RedbeamsSslCertRotateState.FINAL_STATE)
                    .event(REDBEAMS_SSL_CERT_ROTATE_FINISHED_EVENT)
                    .defaultFailureEvent()

                    .build();

    private static final FlowEdgeConfig<RedbeamsSslCertRotateState, RedbeamsSslCertRotateEvent> EDGE_CONFIG =
            new FlowEdgeConfig<>(INIT_STATE, FINAL_STATE, REDBEAMS_SSL_CERT_ROTATE_FAILED_STATE, REDBEAMS_SSL_CERT_ROTATE_FAILURE_HANDLED_EVENT);

    public RedbeamsSslCertRotateFlowConfig() {
        super(RedbeamsSslCertRotateState.class, RedbeamsSslCertRotateEvent.class);
    }

    @Override
    protected List<Transition<RedbeamsSslCertRotateState, RedbeamsSslCertRotateEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    public FlowEdgeConfig<RedbeamsSslCertRotateState, RedbeamsSslCertRotateEvent> getEdgeConfig() {
        return EDGE_CONFIG;
    }

    @Override
    public RedbeamsSslCertRotateEvent[] getEvents() {
        return values();
    }

    @Override
    public RedbeamsSslCertRotateEvent[] getInitEvents() {
        return REDBEAMS_INIT_EVENTS;
    }

    @Override
    public String getDisplayName() {
        return "Rotate Cert RDS";
    }

    @Override
    public RedbeamsSslCertRotateEvent getRetryableEvent() {
        return REDBEAMS_SSL_CERT_ROTATE_FAILURE_HANDLED_EVENT;
    }
}
