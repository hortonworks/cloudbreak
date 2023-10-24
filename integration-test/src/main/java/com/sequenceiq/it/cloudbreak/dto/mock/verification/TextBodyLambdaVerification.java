package com.sequenceiq.it.cloudbreak.dto.mock.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.dto.mock.Verification;
import com.sequenceiq.it.verification.Call;

public class TextBodyLambdaVerification implements Verification {

    private Predicate<String> check;

    private Integer times;

    public TextBodyLambdaVerification(Predicate<String> check) {
        this.check = check;
    }

    public TextBodyLambdaVerification(Predicate<String> check, int times) {
        this.check = check;
        this.times = times;
    }

    @Override
    public void handle(String path, Method method, VerificationContext context) {
        List<Call> accepted = new ArrayList<>();
        int timesMatched = 0;
        for (Call call : context.getCalls()) {
            boolean checkPassed = check.test(call.getPostBody().toString());
            if (checkPassed) {
                timesMatched++;
            }
        }
        if (times == null && timesMatched < 1) {
            context.getErrors().add("The check did not find in any call");
        }
        if (times != null && timesMatched != times) {
            context.getErrors().add("The check did not find with " + times + " expected times. Check found " + timesMatched + " times");
        }
        context.setCalls(accepted);
    }
}
