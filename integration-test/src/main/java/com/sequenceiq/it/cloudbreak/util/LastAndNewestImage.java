package com.sequenceiq.it.cloudbreak.util;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.image.Image;

public record LastAndNewestImage(Image last, Image newest) {
}
