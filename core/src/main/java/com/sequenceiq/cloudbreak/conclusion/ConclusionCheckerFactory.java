package com.sequenceiq.cloudbreak.conclusion;

import java.util.HashMap;
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

    private Map<Class<? extends ConclusionStep>, ConclusionStep> conclusionStepInstancesMapByType;

    @PostConstruct
    public void init() {
        conclusionStepInstancesMapByType = conclusionSteps.stream().collect(Collectors.toMap(ConclusionStep::getClass, step -> step));
    }

    public ConclusionChecker getConclusionChecker(ConclusionCheckerType conclusionCheckerType) {
        ConclusionStepNode rootNode = ConclusionStepNodeFactory.getConclusionStepNode(conclusionCheckerType);
        Map<Class<? extends ConclusionStep>, ConclusionStep> conclusionStepInstances = getConclusionStepInstances(rootNode);
        return new ConclusionChecker(rootNode, conclusionStepInstances);
    }

    private Map<Class<? extends ConclusionStep>, ConclusionStep> getConclusionStepInstances(ConclusionStepNode rootNode) {
        Map<Class<? extends ConclusionStep>, ConclusionStep> conclusionStepInstances = new HashMap<>();
        for (Class<? extends ConclusionStep> stepClass : rootNode.getAllSteps()) {
            if (!conclusionStepInstancesMapByType.containsKey(stepClass)) {
                String message = "Unknown conclusion step class: " + stepClass;
                LOGGER.error(message);
                throw new IllegalArgumentException(message);
            } else {
                conclusionStepInstances.put(stepClass, conclusionStepInstancesMapByType.get(stepClass));
            }
        }
        return conclusionStepInstances;
    }

}
