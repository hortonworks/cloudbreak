package com.sequenceiq.cloudbreak.conclusion;

import java.util.Collections;
import java.util.List;

public class ConclusionResult {

    private List<String> conclusions;

    public ConclusionResult(List<String> conclusions) {
        if (conclusions != null) {
            this.conclusions = conclusions;
        } else {
            this.conclusions = Collections.emptyList();
        }
    }

    public List<String> getConclusions() {
        return conclusions;
    }
}
