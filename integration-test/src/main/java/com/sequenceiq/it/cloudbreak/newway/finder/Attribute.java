package com.sequenceiq.it.cloudbreak.newway.finder;


import com.sequenceiq.it.cloudbreak.newway.dto.CloudbreakTestDto;

public interface Attribute<T extends CloudbreakTestDto, O> {
    O get(T entity);
}

