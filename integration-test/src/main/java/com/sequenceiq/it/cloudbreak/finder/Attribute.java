package com.sequenceiq.it.cloudbreak.finder;


import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;

public interface Attribute<T extends CloudbreakTestDto, O> {
    O get(T entity);
}

