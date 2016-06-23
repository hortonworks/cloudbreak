package com.sequenceiq.cloudbreak.shell.util

object MessageUtil {

    fun getMessage(exception: Exception): String {
        //if (exception instanceof ResponseExceptionMapper)
        //return String.valueOf(((Map) ((HttpResponseException) exception).getResponse().getData()).get("message"));
        //}
        return exception.message
    }
}
