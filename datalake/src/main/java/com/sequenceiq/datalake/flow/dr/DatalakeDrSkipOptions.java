package com.sequenceiq.datalake.flow.dr;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DatalakeDrSkipOptions {

    private final boolean skipAtlasMetadata;

    private final boolean skipRangerAudits;

    private final boolean skipRangerMetadata;

    @JsonCreator
    public DatalakeDrSkipOptions(
            @JsonProperty("skipAtlasMetadata") boolean skipAtlasMetadata,
            @JsonProperty("skipRangerAudits") boolean skipRangerAudits,
            @JsonProperty("skipRangerMetadata") boolean skipRangerMetadata) {
        this.skipAtlasMetadata = skipAtlasMetadata;
        this.skipRangerAudits = skipRangerAudits;
        this.skipRangerMetadata = skipRangerMetadata;
    }

    public boolean isSkipAtlasMetadata() {
        return skipAtlasMetadata;
    }

    public boolean isSkipRangerAudits() {
        return skipRangerAudits;
    }

    public boolean isSkipRangerMetadata() {
        return skipRangerMetadata;
    }

    @Override
    public String toString() {
        return "DatalakeDrSkipOptions{" +
                "skipAtlasMetadata=" + skipAtlasMetadata +
                ", skipRangerAudits=" + skipRangerAudits +
                ", skipRangerMetadata=" + skipRangerMetadata +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DatalakeDrSkipOptions other = (DatalakeDrSkipOptions) o;
        return skipAtlasMetadata == other.skipAtlasMetadata && skipRangerAudits == other.skipRangerAudits && skipRangerMetadata == other.skipRangerMetadata;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skipAtlasMetadata, skipRangerAudits, skipRangerMetadata);
    }
}
