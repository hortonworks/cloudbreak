package com.sequenceiq.cloudbreak.cloud.model;

import java.util.SortedSet;

public record PlatformDBStorageCapabilities(
        SortedSet<Long> supportedStorageSizeInMb) {
}
