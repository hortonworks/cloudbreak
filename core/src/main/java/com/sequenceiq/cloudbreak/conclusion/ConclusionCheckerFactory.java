package com.sequenceiq.cloudbreak.conclusion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conclusion.step.ConclusionStep;

@Component
public class ConclusionCheckerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConclusionCheckerFactory.class);

    @Inject
    private List<ConclusionStep> conclusionSteps;

    private Map<Class<? extends ConclusionStep>, ConclusionStep> conclusionStepMapByType;

    @PostConstruct
    public void init() {
        conclusionStepMapByType = conclusionSteps.stream().collect(Collectors.toMap(ConclusionStep::getClass, step -> step));
    }

    public ConclusionChecker getConclusionChecker(ConclusionCheckerType conclusionCheckerType) {
        List<Class<? extends ConclusionStep>> stepClasses = conclusionCheckerType.getSteps();
        List<ConclusionStep> actualConclusionSteps = getActualConclusionSteps(stepClasses);
        return new ConclusionChecker(actualConclusionSteps);
    }

    private List<ConclusionStep> getActualConclusionSteps(List<Class<? extends ConclusionStep>> stepClasses) {
        List<ConclusionStep> actualConclusionSteps = new ArrayList<>();
        for (Class<? extends ConclusionStep> stepClass : stepClasses) {
            if (!conclusionStepMapByType.containsKey(stepClass)) {
                String message = "Unknown conclusion step class: " + stepClass;
                LOGGER.error(message);
                throw new IllegalArgumentException(message);
            } else {
                actualConclusionSteps.add(conclusionStepMapByType.get(stepClass));
            }
        }
        return actualConclusionSteps;
    }

}
