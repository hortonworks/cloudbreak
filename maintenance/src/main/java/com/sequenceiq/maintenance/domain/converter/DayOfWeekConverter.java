package com.sequenceiq.maintenance.domain.converter;

import java.time.DayOfWeek;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class DayOfWeekConverter extends DefaultEnumConverter<DayOfWeek> {

    @Override
    public DayOfWeek getDefault() {
        return DayOfWeek.SUNDAY;
    }
}
