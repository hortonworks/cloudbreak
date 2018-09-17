package com.sequenceiq.it.cloudbreak.newway;


import javax.inject.Inject;

import org.kohsuke.randname.RandomNameGenerator;
import org.springframework.stereotype.Component;

@Component
public class RandomNameCreator {

    @Inject
    private RandomNameGenerator generator;

    public String getRandomNameForMock() {
        return "mock-" + generator.next().replaceAll("_", "-");
    }
}
