package com.sequenceiq.cloudbreak.datalakedr;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DatalakeDrSkipOptions {

    private final boolean skipValidation;

    private final boolean skipAtlasMetadata;

    private final boolean skipRangerAudits;

    private final boolean skipRangerMetadata;

    @JsonCreator
    public DatalakeDrSkipOptions(
            @JsonProperty("skipValidation") boolean skipValidation,
            @JsonProperty("skipAtlasMetadata") boolean skipAtlasMetadata,
            @JsonProperty("skipRangerAudits") boolean skipRangerAudits,
            @JsonProperty("skipRangerMetadata") boolean skipRangerMetadata) {
        this.skipValidation = skipValidation;
        this.skipAtlasMetadata = skipAtlasMetadata;
        this.skipRangerAudits = skipRangerAudits;
        this.skipRangerMetadata = skipRangerMetadata;
    }

    public boolean isSkipValidation() {
        return skipValidation;
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
                "skipValidation=" + skipValidation +
                ", skipAtlasMetadata=" + skipAtlasMetadata +
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
        return skipValidation == other.skipValidation && skipAtlasMetadata == other.skipAtlasMetadata &&
                skipRangerAudits == other.skipRangerAudits && skipRangerMetadata == other.skipRangerMetadata;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skipValidation, skipAtlasMetadata, skipRangerAudits, skipRangerMetadata);
    }
}
