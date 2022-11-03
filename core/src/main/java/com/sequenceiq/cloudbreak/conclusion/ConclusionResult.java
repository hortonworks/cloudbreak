package com.sequenceiq.cloudbreak.conclusion;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.conclusion.step.Conclusion;

public class ConclusionResult {

    private List<Conclusion> conclusions;

    public ConclusionResult(List<Conclusion> conclusions) {
        if (conclusions != null) {
            this.conclusions = conclusions;
        } else {
            this.conclusions = Collections.emptyList();
        }
    }

    public List<Conclusion> getConclusions() {
        return conclusions;
    }

    public List<String> getConclusionTexts() {
        return conclusions.stream()
                .map(Conclusion::getConclusion)
                .collect(Collectors.toList());
    }

    public List<String> getFailedConclusionTexts() {
        return conclusions.stream()
                .filter(Conclusion::isFailureFound)
                .map(Conclusion::getConclusion)
                .collect(Collectors.toList());
    }

    public boolean isFailureFound() {
        return conclusions.stream().anyMatch(Conclusion::isFailureFound);
    }

}
