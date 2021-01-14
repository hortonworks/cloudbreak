package com.sequenceiq.it.cloudbreak.dto.mock.verification;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.it.verification.Call;

public class VerificationContext {

    private List<Call> calls;

    private List<String> errors = new ArrayList<>();

    public VerificationContext(List<Call> calls) {
        this.calls = calls;
    }

    public List<Call> getCalls() {
        return calls;
    }

    public void setCalls(List<Call> calls) {
        this.calls = calls;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
