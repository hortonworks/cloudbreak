package com.sequenceiq.cloudbreak.auth;

import java.util.Optional;
import java.util.Stack;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

@Service
public class ThreadBasedUserCrnProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadBasedUserCrnProvider.class);

    private static final ThreadLocal<Stack<String>> USER_CRN = new ThreadLocal<>();

    @Nullable
    public String getUserCrn() {
        Stack<String> stack = USER_CRN.get();
        return stack != null ? stack.peek() : null;
    }

    public String getAccountId() {
        String userCrn = getUserCrn();
        if (userCrn != null) {
            return Optional.ofNullable(Crn.fromString(userCrn)).orElseThrow(() -> new IllegalStateException("Unable to obtain crn!")).getAccountId();
        } else {
            throw new IllegalStateException("Crn is not set!");
        }
    }

    public void setUserCrn(String userCrn) {
        Stack<String> stack = USER_CRN.get();
        if (stack == null) {
            stack = new Stack<>();
            USER_CRN.set(stack);
        }
        if (!stack.isEmpty() && !stack.peek().equals(userCrn)) {
            LOGGER.error("Trying to push crn to stack {} when it already contains {}", userCrn, stack.get(0));
            // REMOVE IT UNTIL WE FIX
            // throw new IllegalStateException(String.format("Trying to push crn to stack %s when it already contains %s", userCrn, stack.get(0)));
        }
        stack.push(userCrn);
    }

    public void removeUserCrn() {
        Stack<String> stack = USER_CRN.get();
        if (stack != null) {
            stack.pop();
            if (stack.isEmpty()) {
                USER_CRN.remove();
            }
        }
    }
}
