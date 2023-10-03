package com.sequenceiq.cloudbreak.logger;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.springframework.stereotype.Component;

/**
 * This class can be used to emit the LOGGER messages into it and check whether it was called from unit tests
 */
@Component
public class LogVerifier implements Logger {

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public void trace(String msg) {
    }

    @Override
    public void trace(String format, Object arg) {
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
    }

    @Override
    public void trace(String format, Object... arguments) {
    }

    @Override
    public void trace(String msg, Throwable t) {
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return true;
    }

    @Override
    public void trace(Marker marker, String msg) {
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(String msg) {
    }

    @Override
    public void debug(String format, Object arg) {
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
    }

    @Override
    public void debug(String format, Object... arguments) {
    }

    @Override
    public void debug(String msg, Throwable t) {
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return true;
    }

    @Override
    public void debug(Marker marker, String msg) {
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String msg) {
    }

    @Override
    public void info(String format, Object arg) {
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
    }

    @Override
    public void info(String format, Object... arguments) {
    }

    @Override
    public void info(String msg, Throwable t) {
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return true;
    }

    @Override
    public void info(Marker marker, String msg) {
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
    }

    @Override
    public void warn(String format, Object arg) {
    }

    @Override
    public void warn(String format, Object... arguments) {
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
    }

    @Override
    public void warn(String msg, Throwable t) {
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return true;
    }

    @Override
    public void warn(Marker marker, String msg) {
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String msg) {
    }

    @Override
    public void error(String format, Object arg) {
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
    }

    @Override
    public void error(String format, Object... arguments) {
    }

    @Override
    public void error(String msg, Throwable t) {
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return true;
    }

    @Override
    public void error(Marker marker, String msg) {
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
    }
}
