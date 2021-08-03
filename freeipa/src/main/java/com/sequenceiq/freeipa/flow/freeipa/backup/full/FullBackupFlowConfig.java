package com.sequenceiq.freeipa.flow.freeipa.backup.full;

import static com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupEvent.FULL_BACKUP_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupEvent.FULL_BACKUP_FAILED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupEvent.FULL_BACKUP_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupEvent.FULL_BACKUP_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupEvent.FULL_BACKUP_SUCCESSFUL_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupState.BACKUP_FAILED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupState.BACKUP_FINISHED_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupState.BACKUP_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupState.FINAL_STATE;
import static com.sequenceiq.freeipa.flow.freeipa.backup.full.FullBackupState.INIT_SATE;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition.Builder;

@Component
public class FullBackupFlowConfig extends AbstractFlowConfiguration<FullBackupState, FullBackupEvent> {

    private static final List<Transition<FullBackupState, FullBackupEvent>> TRANSITIONS =
            new Builder<FullBackupState, FullBackupEvent>().defaultFailureEvent(FULL_BACKUP_FAILED_EVENT)
            .from(INIT_SATE)
            .to(BACKUP_STATE)
            .event(FULL_BACKUP_EVENT)
            .defaultFailureEvent()

            .from(BACKUP_STATE)
            .to(BACKUP_FINISHED_STATE)
            .event(FULL_BACKUP_SUCCESSFUL_EVENT)
            .defaultFailureEvent()

            .from(BACKUP_FINISHED_STATE)
            .to(FINAL_STATE)
            .event(FULL_BACKUP_FINISHED_EVENT)
            .defaultFailureEvent()

            .build();

    protected FullBackupFlowConfig() {
        super(FullBackupState.class, FullBackupEvent.class);
    }

    @Override
    protected List<Transition<FullBackupState, FullBackupEvent>> getTransitions() {
        return TRANSITIONS;
    }

    @Override
    protected FlowEdgeConfig<FullBackupState, FullBackupEvent> getEdgeConfig() {
        return new FlowEdgeConfig<>(INIT_SATE, FINAL_STATE, BACKUP_FAILED_STATE, FULL_BACKUP_FAILURE_HANDLED_EVENT);
    }

    @Override
    public FullBackupEvent[] getEvents() {
        return FullBackupEvent.values();
    }

    @Override
    public FullBackupEvent[] getInitEvents() {
        return new FullBackupEvent[]{FULL_BACKUP_EVENT};
    }

    @Override
    public String getDisplayName() {
        return "FreeIPA full backup";
    }
}
