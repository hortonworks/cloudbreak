package com.sequenceiq.cloudbreak.api.endpoint.v4.dto;

import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import com.sequenceiq.cloudbreak.exception.BadRequestException;

public class ResourceAccessDto {

    public static final String NULL_DTO_EXCEPTION_MESSAGE = "AccessDto should not be null";

    public static final String INVALID_RESOURCE_ACCESS_DTO_EXCEPTION_MESSAGE = "One and only one value of the crn and name should be filled!";

    private final String name;

    private final String crn;

    protected ResourceAccessDto(String name, String crn) {
        this.name = name;
        this.crn = crn;
    }

    public String getName() {
        return name;
    }

    public String getCrn() {
        return crn;
    }

    public static void validate(ResourceAccessDto dto) {
        throwIfNull(dto, () -> new IllegalArgumentException(NULL_DTO_EXCEPTION_MESSAGE));
        if (dto.isNotValid()) {
            throw new BadRequestException(INVALID_RESOURCE_ACCESS_DTO_EXCEPTION_MESSAGE);
        }
    }

    public boolean isNotValid() {
        return !isValid();
    }

    public boolean isValid() {
        return isNotEmpty(name) ^ isNotEmpty(crn);
    }

    public static class ResourceAccessDtoBuilder {

        private String name;

        private String crn;

        public static ResourceAccessDtoBuilder aResourceAccessDtoBuilder() {
            return new ResourceAccessDtoBuilder();
        }

        public ResourceAccessDtoBuilder withName(String name) {
            if (isNotEmpty(name)) {
                this.name = name;
            }
            return this;
        }

        public ResourceAccessDtoBuilder withCrn(String crn) {
            if (isNotEmpty(crn)) {
                this.crn = crn;
            }
            return this;
        }

        public ResourceAccessDto build() {
            return new ResourceAccessDto(name, crn);
        }

    }

}
