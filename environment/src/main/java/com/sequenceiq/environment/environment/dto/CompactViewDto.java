package com.sequenceiq.environment.environment.dto;

import java.io.Serializable;

/**
 * DTO for compact view projections, used when only basic environment information is needed.
 */
public class CompactViewDto implements Serializable {

    private final Long id;

    private final String name;

    public CompactViewDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "CompactViewDto{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}

