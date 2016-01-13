package com.sequenceiq.periscope.model;

import java.util.List;

import com.sequenceiq.periscope.rest.json.Json;

public class QueueSetup implements Json {

    private final List<Queue> setup;

    public QueueSetup(List<Queue> setup) {
        this.setup = setup;
    }

    public List<Queue> getSetup() {
        return setup;
    }

}
