package com.sequenceiq.cloudbreak.logger;

public class PartialMessageMetadata {

    private final String partialId;

    private final String partialOrdinal;

    private final String partialLast;

    public PartialMessageMetadata(String partialId, Integer partialOrdinal, Boolean partialLast) {
        this.partialId = partialId;
        this.partialOrdinal = String.valueOf(partialOrdinal);
        this.partialLast = String.valueOf(partialLast);
    }

    public String getPartialId() {
        return partialId;
    }

    public String getPartialOrdinal() {
        return partialOrdinal;
    }

    public String getPartialLast() {
        return partialLast;
    }
}
