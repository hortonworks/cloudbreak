package com.sequenceiq.cloudbreak.sdx.common.model;

import java.util.Map;

public record SdxFileSystemView(String fileSystemType,
                                Map<String, String> sharedFileSystemLocationsByService) {
}
