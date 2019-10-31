package com.sequenceiq.cloudbreak.auth;

import java.util.Optional;
import java.util.Stack;

import javax.annotation.Nullable;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

@Service
public class ThreadBasedUserCrnProvider {

    private static final ThreadLocal<Stack<String>> USER_CRN = new InheritableThreadLocal<>();

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
