package com.sequenceiq.periscope.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class DateUtils {

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm");
        }
    };

    private DateUtils() {
        throw new IllegalStateException();
    }

    public static Date toDate(String date, String timeZone) {
        SimpleDateFormat dateFormat = DATE_FORMAT.get();
        dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
        try {
            return dateFormat.parse(date);
        } catch (ParseException e) {
            return null;
        }
    }


}
