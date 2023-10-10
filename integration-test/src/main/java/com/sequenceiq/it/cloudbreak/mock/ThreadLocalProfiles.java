package com.sequenceiq.it.cloudbreak.mock;

import java.util.HashSet;
import java.util.Set;

public class ThreadLocalProfiles {

    private static ThreadLocal<Set<String>> profiles = new ThreadLocal<>();

    private ThreadLocalProfiles() {
    }

    public static boolean isProfileSet(String profile) {
        return profiles.get().contains(profile);
    }

    public static void setProfile(String profile) {
        Set<String> profileSet = profiles.get();
        if (profileSet != null) {
            profileSet.add(profile);
        } else {
            profileSet = new HashSet<>();
            profileSet.add(profile);
            profiles.set(profileSet);
        }
    }

    public static void clearProfiles() {
        Set<String> profile = profiles.get();
        if (profile != null) {
            profiles.get().clear();
        }
    }

    public static Set<String> getActiveProfiles() {
        Set<String> result = profiles.get();
        return result == null ? new HashSet<>() : result;
    }
}
