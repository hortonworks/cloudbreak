package com.sequenceiq.it.cloudbreak.mock;

import java.util.HashSet;
import java.util.Set;

public class ThreadLocalProfiles {

    private static ThreadLocal<Set<String>> profiles = new ThreadLocal<>();

    static {
        profiles.set(new HashSet<>());
    }

    private ThreadLocalProfiles() {
    }

    public static boolean isProfileSet(String profile) {
        return profiles.get().contains(profile);
    }

    public static void setProfile(String profile) {
        Set<String> profileSet = profiles.get();
        profileSet.add(profile);
    }

    public static void clearProfiles() {
        profiles.get().clear();
    }

    public static Set<String> getActiveProfiles() {
        return new HashSet<>(profiles.get());
    }
}
