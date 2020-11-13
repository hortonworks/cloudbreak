package com.sequenceiq.cloudbreak.auth.altus;

public class RightUtil {

    private RightUtil() {
    }

    public static boolean isReadRight(String action) {
        if (action == null) {
            return false;
        }
        String[] parts = action.split("/");
        if (parts.length == 2 && parts[1] != null && parts[1].equals("read")) {
            return true;
        }
        return false;
    }
}
