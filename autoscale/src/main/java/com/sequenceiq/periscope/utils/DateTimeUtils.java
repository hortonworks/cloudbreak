package com.sequenceiq.periscope.utils;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.stereotype.Service;

@Service
public class DateTimeUtils {

    public DateTime getCurrentDate(String timeZone) {
        return getCurrentDateTime(timeZone).toLocalDateTime().toDateTime();
    }

    public DateTime getDateTime(Date date, String timeZone) {
        return new DateTime(date).withZone(getTimeZone(timeZone));
    }

    private DateTime getCurrentDateTime(String timeZone) {
        return DateTime.now(getTimeZone(timeZone));
    }

    private DateTimeZone getTimeZone(String timeZone) {
        return DateTimeZone.forID(timeZone);
    }

}
