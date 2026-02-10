package com.sequenceiq.cloudbreak.service.stackpatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.service.CloudbreakException;

/**
 * A class to store the current state of a volume conversion operation. Only intended for internal state management.
 */
public class ConversionStatus {
    private Map<String, CloudbreakException> cloudbreakExceptions;

    private List<String> successes;

    public ConversionStatus() {
        this.cloudbreakExceptions = new HashMap<>();
        this.successes = new ArrayList<>();
    }

    public ConversionStatus(Map<String, CloudbreakException> cloudbreakExceptions, List<String> successes) {
        this.cloudbreakExceptions = cloudbreakExceptions;
        this.successes = successes;
    }

    public Map<String, CloudbreakException> cloudbreakExceptions() {
        return cloudbreakExceptions;
    }

    public List<String> successes() {
        return successes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String, CloudbreakException> cloudbreakExceptions = new HashMap<>();

        private List<String> successes = new ArrayList<>();

        public Builder cloudbreakExceptions(Map<String, CloudbreakException> cloudbreakExceptions) {
            this.cloudbreakExceptions = cloudbreakExceptions;
            return this;
        }

        public Builder successes(List<String> successes) {
            this.successes = successes;
            return this;
        }

        public ConversionStatus build() {
            return new ConversionStatus(cloudbreakExceptions, successes);
        }
    }
}
