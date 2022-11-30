package com.sequenceiq.cloudbreak.util;

public class UserDataReplacer {

    private static final String EXPORT_PREFIX_FORMAT = "export %s=";

    private String userData;

    public UserDataReplacer(String userData) {
        this.userData = userData;
    }

    public UserDataReplacer replace(String parameter, Object newValue) {
        return replaceInternal(parameter, false, newValue);
    }

    public UserDataReplacer replaceQuoted(String parameter, Object newValue) {
        return replaceInternal(parameter, true, newValue);
    }

    private UserDataReplacer replaceInternal(String parameter, boolean quoted, Object newValue) {
        String format = EXPORT_PREFIX_FORMAT + (quoted ? "\"%s\"" : "%s") + "\n";
        String oldValueSearchPattern = String.format(EXPORT_PREFIX_FORMAT, parameter);
        String newFormattedValue = String.format(format, parameter, newValue);

        if (userData.contains(oldValueSearchPattern)) {
            String oldValueRegex = oldValueSearchPattern + ".*\n";
            if (newValue != null) {
                // replace parameter
                userData = userData.replaceAll(oldValueRegex, newFormattedValue);
            } else {
                // remove parameter
                userData = userData.replaceAll(oldValueRegex, "");
            }
        } else if (newValue != null) {
            // add new parameter
            int indexAfterLastExport = userData.indexOf("\n", userData.lastIndexOf("export")) + 1;
            userData = userData.substring(0, indexAfterLastExport) + newFormattedValue + userData.substring(indexAfterLastExport);
        }
        return this;
    }

    public String getUserData() {
        return userData;
    }
}
