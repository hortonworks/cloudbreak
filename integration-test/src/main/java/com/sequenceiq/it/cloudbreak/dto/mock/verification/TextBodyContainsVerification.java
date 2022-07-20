package com.sequenceiq.it.cloudbreak.dto.mock.verification;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.dto.mock.Verification;
import com.sequenceiq.it.verification.Call;

public class TextBodyContainsVerification implements Verification {

    private String body;

    private int times;

    public TextBodyContainsVerification(String body, int times) {
        this.body = body;
        this.times = times;
    }

    @Override
    public void handle(String path, Method method, VerificationContext context) {
        List<Call> accepted = new ArrayList<>();
        for (Call call : context.getCalls()) {
            int count = StringUtils.countMatches(call.getPostBody(), body);
            if (count == times) {
                accepted.add(call);
            }
        }
        if (accepted.isEmpty()) {
            context.getErrors().add(body + " did not find in any call with " + times + " expected times.");
        }
        context.setCalls(accepted);
    }
}
