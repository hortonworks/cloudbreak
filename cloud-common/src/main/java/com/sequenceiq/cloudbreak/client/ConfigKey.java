package com.sequenceiq.cloudbreak.client;


public class ConfigKey {

    private boolean secure;

    private boolean debug;

    public ConfigKey(boolean secure, boolean debug) {
        this.secure = secure;
        this.debug = debug;
    }

    public boolean isSecure() {
        return secure;
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        } else if (this == o) {
            return true;
        }

        ConfigKey configKey = (ConfigKey) o;

        return secure == configKey.secure && debug == configKey.debug;
    }

    @Override
    public int hashCode() {
        int result = secure ? 1 : 0;
        result = 31 * result + (debug ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ConfigKey{"
                + "secure=" + secure
                + ", debug=" + debug
                + '}';
    }
}
