package com.sequenceiq.environment.environment.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class EnvironmentStackConfigUpdateService {
    private final EnvironmentViewService environmentViewService;

    private final EnvironmentReactorFlowManager reactorFlowManager;

    public EnvironmentStackConfigUpdateService(
            EnvironmentViewService environmentViewService,
        EnvironmentReactorFlowManager reactorFlowManager) {
        this.environmentViewService = environmentViewService;
        this.reactorFlowManager = reactorFlowManager;
    }

    public FlowIdentifier updateAllStackConfigsByCrn(String envCrn) {
        String accountId = Crn.safeFromString(envCrn).getAccountId();
        EnvironmentView environmentView = environmentViewService.getByCrnAndAccountId(envCrn, accountId);

        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return reactorFlowManager.triggerStackConfigUpdatesFlow(environmentView, userCrn);
    }
}
