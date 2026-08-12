package com.sequenceiq.maintenance.service.model;

public record WindowOccurrence(long windowStart, long windowEnd) {

    public boolean overlaps(WindowOccurrence other) {
        return windowStart < other.windowEnd && other.windowStart < windowEnd;
    }
}
