package com.sequenceiq.freeipa.entity.projection;

import com.sequenceiq.freeipa.entity.StackStatus;

public record FreeIpaListView(String domain, String name, String resourceCrn, String environmentCrn, StackStatus stackStatus) {

}
