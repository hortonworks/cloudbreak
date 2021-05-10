package com.sequenceiq.cloudbreak.cloud.azure.image.pageblobv12.async;


import java.util.function.Consumer;

import org.junit.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactorPoc {

    @Test
    public void testOnErrorResume() {
        Flux.just("timeout1", "unknown", "key2")
                .flatMap(k -> callExternalService(k)
                        .doOnError(e -> {
                            System.out.println("Error happened: " + e);
                        })
                        .onErrorResume(error -> {
                            if (error instanceof TimeoutException)
                                return getFromCache(k);
                            else if (error instanceof UnknownKeyException)
                                return registerNewEntry(k);
                            else
                                return Mono.error((Throwable) error);
                        })
                )
                .subscribe(new Logger());

    }

    private static class Logger implements Consumer<String> {

        @Override
        public void accept(String s) {
            System.out.println("finally received:" + s);
        }
    }
    private Mono<String> callExternalService(String input) {
        System.out.println("call external service: " + input);
        switch (input) {
            case "timeout1":
                return Mono.error(new TimeoutException());
            case "unknown":
                return Mono.error(new UnknownKeyException());
        }
        return Mono.just("returning " + input);
    }

    private Mono<String> getFromCache(String input) {
        System.out.println("get from cache: " + input);
        return Mono.just("from cache " + input);
    }

    private Mono<String> registerNewEntry(String input) {
        System.out.println("register: " + input);
        return Mono.just("registered " + input);
    }

    private class TimeoutException extends RuntimeException {

    }

    private class UnknownKeyException extends RuntimeException {

    }

}
