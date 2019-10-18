package com.sequenceiq.cloudbreak.api.endpoint.v4.dto;

import static com.sequenceiq.cloudbreak.util.NullUtil.throwIfNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import com.sequenceiq.cloudbreak.exception.BadRequestException;

public class GeneralAccessDto {

    private final String name;

    private final String crn;

    public GeneralAccessDto(String name, String crn) {
        this.name = name;
        this.crn = crn;
    }

    public String getName() {
        return name;
    }

    public String getCrn() {
        return crn;
    }

    public static void validate(GeneralAccessDto dto) {
        throwIfNull(dto, () -> new IllegalArgumentException("AccessDto should not be null"));
        if (dto.isNotValid()) {
            throw new BadRequestException("One and only one value of the crn and name should be filled!");
        }
    }

    public boolean isNotValid() {
        return !isValid();
    }

    public boolean isValid() {
        return isNotEmpty(name) ^ isNotEmpty(crn);
    }

}
