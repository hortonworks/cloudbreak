package com.sequenceiq.cloudbreak.jvm;

public record MemoryCategory(String name, double reserved, double committed) {
}
