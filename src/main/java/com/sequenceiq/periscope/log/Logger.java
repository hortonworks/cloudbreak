package com.sequenceiq.periscope.log;

import org.slf4j.LoggerFactory;

public class Logger implements PeriscopeLogger {

    private static final String DECORATOR = " * * * ";
    private final org.slf4j.Logger sl4jLogger;

    public Logger(Class clazz) {
        this.sl4jLogger = LoggerFactory.getLogger(clazz);
    }

    @Override
    public String getName() {
        return sl4jLogger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return sl4jLogger.isTraceEnabled();
    }

    @Override
    public void trace(long clusterId, String msg) {
        sl4jLogger.trace(getPrefix(clusterId) + msg);
    }

    @Override
    public void trace(long clusterId, String format, Object arg) {
        sl4jLogger.trace(getPrefix(clusterId) + format, arg);
    }

    @Override
    public void trace(long clusterId, String format, Object arg1, Object arg2) {
        sl4jLogger.trace(getPrefix(clusterId) + format, arg1, arg2);
    }

    @Override
    public void trace(long clusterId, String format, Object... arguments) {
        sl4jLogger.trace(getPrefix(clusterId) + format, arguments);
    }

    @Override
    public void trace(long clusterId, String msg, Throwable t) {
        sl4jLogger.trace(getPrefix(clusterId) + msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return sl4jLogger.isDebugEnabled();
    }

    @Override
    public void debug(long clusterId, String msg) {
        sl4jLogger.debug(getPrefix(clusterId) + msg);
    }

    @Override
    public void debug(long clusterId, String format, Object arg) {
        sl4jLogger.debug(getPrefix(clusterId) + format, arg);
    }

    @Override
    public void debug(long clusterId, String format, Object arg1, Object arg2) {
        sl4jLogger.debug(getPrefix(clusterId) + format, arg1, arg2);
    }

    @Override
    public void debug(long clusterId, String format, Object... arguments) {
        sl4jLogger.debug(getPrefix(clusterId) + format, arguments);
    }

    @Override
    public void debug(long clusterId, String msg, Throwable t) {
        sl4jLogger.debug(getPrefix(clusterId) + msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return sl4jLogger.isInfoEnabled();
    }

    @Override
    public void info(long clusterId, String msg) {
        sl4jLogger.info(getPrefix(clusterId) + msg);
    }

    @Override
    public void info(long clusterId, String format, Object arg) {
        sl4jLogger.info(getPrefix(clusterId) + format, arg);
    }

    @Override
    public void info(long clusterId, String format, Object arg1, Object arg2) {
        sl4jLogger.info(getPrefix(clusterId) + format, arg1, arg2);
    }

    @Override
    public void info(long clusterId, String format, Object... arguments) {
        sl4jLogger.info(getPrefix(clusterId) + format, arguments);
    }

    @Override
    public void info(long clusterId, String msg, Throwable t) {
        sl4jLogger.info(getPrefix(clusterId) + msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return sl4jLogger.isWarnEnabled();
    }

    @Override
    public void warn(long clusterId, String msg) {
        sl4jLogger.warn(getPrefix(clusterId) + msg);
    }

    @Override
    public void warn(long clusterId, String format, Object arg) {
        sl4jLogger.warn(getPrefix(clusterId) + format, arg);
    }

    @Override
    public void warn(long clusterId, String format, Object... arguments) {
        sl4jLogger.warn(getPrefix(clusterId) + format, arguments);
    }

    @Override
    public void warn(long clusterId, String format, Object arg1, Object arg2) {
        sl4jLogger.warn(getPrefix(clusterId) + format, arg1, arg2);
    }

    @Override
    public void warn(long clusterId, String msg, Throwable t) {
        sl4jLogger.warn(getPrefix(clusterId) + msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return sl4jLogger.isErrorEnabled();
    }

    @Override
    public void error(long clusterId, String msg) {
        sl4jLogger.error(getPrefix(clusterId) + msg);
    }

    @Override
    public void error(long clusterId, String format, Object arg) {
        sl4jLogger.error(getPrefix(clusterId) + format, arg);
    }

    @Override
    public void error(long clusterId, String format, Object arg1, Object arg2) {
        sl4jLogger.error(getPrefix(clusterId) + format, arg1, arg2);
    }

    @Override
    public void error(long clusterId, String format, Object... arguments) {
        sl4jLogger.error(getPrefix(clusterId) + format, arguments);
    }

    @Override
    public void error(long clusterId, String msg, Throwable t) {
        sl4jLogger.error(getPrefix(clusterId) + msg, t);
    }

    private String getPrefix(long clusterId) {
        return DECORATOR + clusterId + DECORATOR;
    }

}
