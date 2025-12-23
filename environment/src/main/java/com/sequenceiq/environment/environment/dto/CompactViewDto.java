package com.sequenceiq.environment.environment.dto;

import com.sequenceiq.environment.environment.domain.CompactView;

/**
 * DTO for compact view projections, used when only basic environment information is needed.
 */
public class CompactViewDto extends CompactView {

    public CompactViewDto(Long id, String name) {
        super(id, name);
    }

    @Override
    public String toString() {
        return "CompactViewDto{" +
                "id=" + getId() +
                ", name='" + getName() + '\'' +
                '}';
    }
}

