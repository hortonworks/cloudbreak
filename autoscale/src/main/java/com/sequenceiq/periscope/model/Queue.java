package com.sequenceiq.periscope.model;

import com.sequenceiq.periscope.api.model.Json;

public class Queue implements Json {

    private final String name;
    private final int capacity;

    public Queue(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

}
