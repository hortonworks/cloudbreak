package com.sequenceiq.it.cloudbreak.dto.mock.verification;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.dto.mock.Verification;
import com.sequenceiq.it.verification.Call;

public class TextBodyLambdaVerification implements Verification {

    private Predicate<String> check;

    public TextBodyLambdaVerification(Predicate<String> check) {
        this.check = check;
    }

    @Override
    public void handle(String path, Method method, VerificationContext context) {
        List<Call> accepted = new ArrayList<>();
        boolean res = false;
        for (Call call : context.getCalls()) {
            res |= check.test(call.getPostBody().toString());
        }
        if (!res) {
            context.getErrors().add("The check did not find in any call");
        }
        context.setCalls(accepted);
    }
}
