package com.sequenceiq.it.cloudbreak;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.opentest4j.MultipleFailuresError;
import org.opentest4j.TestAbortedException;

public class RetryOnGatewayTimeout {

    private static final String OUT_OF_ATTEMPTS_MESSAGE = "Ran out of retries but still getting timeout from the API.";

    private static final List<WebApplicationException> STORED_EXCEPTIONS = new LinkedList<>();

    private RetryOnGatewayTimeout() {
    }

    /**
     * Executes the provided function and returns it's return value – if has – but retries the execution by the provided amount if a
     * WebApplicationException occurs with a status code 504.
     * If this 504 happens all the time and the retry mechanism runs out of attempt quantity then it will throw a
     * {@code org.opentest4j.MultipleFailuresError} which contains all the previous exception's message.
     *
     * @param method          The method which has to be executed.
     * @param attemptQuantity The maximum amount of attempts what is going to happen if comes an 504.
     * @param <T>             The type of the return value of the provided function.
     * @return                The output value of the given function.
     */
    public static <T> T retry(Callable<T> method, int attemptQuantity) {
        try {
            return doRetryAndReturn(method, attemptQuantity);
        } catch (WebApplicationException e) {
            STORED_EXCEPTIONS.add(e);
            return processResult(e, method, attemptQuantity);
        } catch (Exception e) {
            throw new TestAbortedException("Unexpected exception has occured.", e);
        }
    }

    /**
     * Executes the provided function but retries the execution by the provided amount if a WebApplicationException occurs with a status code 504.
     * If this 504 happens all the time and the retry mechanism runs out of attempt quantity then it will throw a
     * {@code org.opentest4j.MultipleFailuresError} which contains all the previous exception's message.
     *
     * @param method          The method which has to be executed.
     * @param attemptQuantity The maximum amount of attempts what is going to happen if comes an 504.
     */
    public static void retry(Runnable method, int attemptQuantity) {
        try {
            doRetry(method, attemptQuantity);
        } catch (WebApplicationException e) {
            STORED_EXCEPTIONS.add(e);
            if (isResponseGatewayTimedOut(e)) {
                attemptQuantity--;
                retry(method, attemptQuantity);
            } else {
                throw e;
            }
        }
    }

    private static <T> T processResult(WebApplicationException e, Callable<T> method, int attemptQuantity) {
        if (!isResponseGatewayTimedOut(e)) {
            throw e;
        }
        attemptQuantity--;
        return retry(method, attemptQuantity);

    }

    private static <T> T doRetryAndReturn(Callable<T> method, int attemptQuantity) throws Exception {
        if (attemptQuantity > 0) {
            return method.call();
        } else {
            throw new MultipleFailuresError(OUT_OF_ATTEMPTS_MESSAGE, STORED_EXCEPTIONS);
        }
    }

    private static void doRetry(Runnable method, int attemptQuantity) {
        if (attemptQuantity > 0) {
            method.run();
        } else {
            throw new MultipleFailuresError(OUT_OF_ATTEMPTS_MESSAGE, STORED_EXCEPTIONS);
        }
    }

    private static boolean isResponseGatewayTimedOut(WebApplicationException e) {
        try (Response response = e.getResponse()) {
            return response.getStatus() == HttpServletResponse.SC_GATEWAY_TIMEOUT;
        }
    }

}
