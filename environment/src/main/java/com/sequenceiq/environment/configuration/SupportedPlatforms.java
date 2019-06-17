package com.sequenceiq.environment.configuration;

import java.util.Set;

public class SupportedPlatforms {

    private final Set<String> freeIpa;

    private final Set<String> redbeams;

    public SupportedPlatforms(String[] freeIpa, String[] redbeams) {
        this.freeIpa = freeIpa == null ? Set.of() : Set.of(freeIpa);
        this.redbeams = redbeams == null ? Set.of() : Set.of(redbeams);
    }

    public Set<String> freeIpa() {
        return freeIpa;
    }

    public Set<String> redbeams() {
        return redbeams;
    }

    public boolean supportedPlatformForFreeIpa(String platform) {
        return freeIpa.contains(platform);
    }

    public boolean supportedPlatformForRedBeams(String platform) {
        return redbeams.contains(platform);
    }
}
