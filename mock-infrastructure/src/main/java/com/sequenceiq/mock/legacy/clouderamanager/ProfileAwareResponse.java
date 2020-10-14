package com.sequenceiq.mock.legacy.clouderamanager;

import java.util.function.Function;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ProfileAwareResponse<T, R> {

    private final Function<T, R> happyResponseHandler;

//    private final StatefulRoute statefulHappyResponseHandler;

    private final DefaultModelService defaultModelService;

    private int callCounter;

    public ProfileAwareResponse(Function<T, R> happyResponseHandler, DefaultModelService defaultModelService) {
        this.happyResponseHandler = happyResponseHandler;
        this.defaultModelService = defaultModelService;
    }

    public ResponseEntity<R> handle() {
        return handle(null);
    }

    public ResponseEntity<R> handle(T body) {
        if (defaultModelService.containsProfile(DefaultModelService.PROFILE_RETURN_HTTP_500) && callCounter == 0) {
            callCounter++;
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } else {
            callCounter++;
            return new ResponseEntity<>(happyResponseHandler.apply(body), HttpStatus.OK);
        }
    }

    public static <T, R> ProfileAwareResponse<T, R> get(R ret, DefaultModelService defaultModelService) {
        return new ProfileAwareResponse<>(b -> ret, defaultModelService);
    }

    public static <R> ResponseEntity<R> exec(R ret, DefaultModelService defaultModelService) {
        return get(ret, defaultModelService).handle();
    }

    public static ResponseEntity<Void> exec(DefaultModelService defaultModelService) {
        return get((Void) null, defaultModelService).handle();
    }
}
