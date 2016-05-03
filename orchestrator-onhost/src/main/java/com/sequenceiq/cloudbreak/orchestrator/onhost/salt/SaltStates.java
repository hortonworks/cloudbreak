package com.sequenceiq.cloudbreak.orchestrator.onhost.salt;

import com.google.gson.reflect.TypeToken;
import com.suse.salt.netapi.calls.LocalCall;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SaltStates {

    public static LocalCall<Object> consul() {
        return applyState("consul");
    }

    public static LocalCall<Object> ambariServer() {
        return applyState("ambari.server");
    }

    public static LocalCall<Object> ambariAgent() {
        return applyState("ambari.agent");
    }

    public static LocalCall<Object> highstate() {
        Optional<List<?>> arg = Optional.empty();
        Optional<Map<String, ?>> kwarg = Optional.empty();

        LocalCall<Object> highstate =
                new LocalCall<>("state.highstate", arg, kwarg,
                        new TypeToken<Object>() {
                        });
        return highstate;
    }



    private static LocalCall<Object> applyState(String state) {
        Optional<List<?>> arg = Optional.of(Arrays.asList(state));
        Optional<Map<String, ?>> kwarg = Optional.empty();

        LocalCall<Object> ambariAgent =
                new LocalCall<>("state.apply", arg, kwarg,
                        new TypeToken<Object>() {
                        });
        return ambariAgent;
    }


}
