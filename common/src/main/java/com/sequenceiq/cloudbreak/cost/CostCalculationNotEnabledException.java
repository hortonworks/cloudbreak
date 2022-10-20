package com.sequenceiq.cloudbreak.cost;

public class CostCalculationNotEnabledException extends RuntimeException {
    public CostCalculationNotEnabledException() {
    }

    public CostCalculationNotEnabledException(String message) {
        super(message);
    }

    public CostCalculationNotEnabledException(String message, Throwable cause) {
        super(message, cause);
    }

    public CostCalculationNotEnabledException(Throwable cause) {
        super(cause);
    }
}
