package com.sequenceiq.distrox.api.v1.distrox.validation.volume;

public interface RootVolumeSizeProvider {

    int getForPlatform(String platform);
}
