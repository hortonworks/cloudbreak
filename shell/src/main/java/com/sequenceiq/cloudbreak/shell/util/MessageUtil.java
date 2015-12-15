package com.sequenceiq.cloudbreak.shell.util;

public final class MessageUtil {
    private MessageUtil() {
    }

    public static String getMessage(Exception exception) {
        //if (exception instanceof ResponseExceptionMapper)
        //return String.valueOf(((Map) ((HttpResponseException) exception).getResponse().getData()).get("message"));
        //}
        return exception.getMessage();
    }
}
