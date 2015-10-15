package com.sequenceiq.cloudbreak.cloud.model;

/**
 *
 * The 'diskPrefix' and 'startLabel' parameters are part of the disk naming pattern on different platforms
 *
 * e.g. on AWS the disk naming pattern is /dev/xvda, /dev/xvdb, ...
 * where the 'diskPrefix' is 'xvd' and the 'startLabel' is the ascii code of 'a' which is '97'
 */
public class ScriptParams {
    private final String diskPrefix;
    private final Integer startLabel;

    public ScriptParams(String diskPrefix, Integer startLabel) {
        this.diskPrefix = diskPrefix;
        this.startLabel = startLabel;
    }

    public String getDiskPrefix() {
        return diskPrefix;
    }

    public Integer getStartLabel() {
        return startLabel;
    }
}
