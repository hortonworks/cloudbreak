package com.sequenceiq.cloudbreak.conclusion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;

@Service
public class ConclusionCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConclusionCheckerService.class);

    @Inject
    private ConclusionCheckerFactory conclusionCheckerFactory;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private EntitlementService entitlementService;

    public void runConclusionChecker(Long resourceId, String eventType, ResourceEvent resourceEvent,
            ConclusionCheckerType conclusionCheckerType, String... eventMessageArgs) {
        try {
            ConclusionChecker conclusionChecker = conclusionCheckerFactory.getConclusionChecker(conclusionCheckerType);
            ConclusionResult conclusionResult = conclusionChecker.doCheck(resourceId);
            if (entitlementService.conclusionCheckerSendUserEventEnabled(ThreadBasedUserCrnProvider.getAccountId()) && conclusionResult.isFailureFound()) {
                flowMessageService.fireEventAndLog(resourceId, eventType, resourceEvent, getFullEventMessageArgs(eventMessageArgs, conclusionResult));
            }
        } catch (Exception e) {
            LOGGER.error("Error happened during conclusion check", e);
        }
    }

    private static String[] getFullEventMessageArgs(String[] eventMessageArgs, ConclusionResult conclusionResult) {
        List<String> fullEventMessageList = new ArrayList<>();
        if (eventMessageArgs != null) {
            fullEventMessageList.addAll(Arrays.asList(eventMessageArgs));
        }
        fullEventMessageList.add(conclusionResult.getFailedConclusionTexts().toString());
        return fullEventMessageList.toArray(new String[0]);
    }
}
