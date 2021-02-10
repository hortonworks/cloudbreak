package com.sequenceiq.environment.experience.liftie;

import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiftieWorkload {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiftieWorkload.class);

    private String name;

    private String description;

    public LiftieWorkload() {
    }

    public LiftieWorkload(String name, String description) {
        this.name = name;
        this.description = description;
        LOGGER.debug("Liftie workload has been created: {}", toString());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LiftieWorkload.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("description='" + description + "'")
                .toString();
    }

}
