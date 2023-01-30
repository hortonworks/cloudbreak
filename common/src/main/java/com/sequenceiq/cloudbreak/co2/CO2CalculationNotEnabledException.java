package com.sequenceiq.cloudbreak.co2;

public class CO2CalculationNotEnabledException extends RuntimeException {

    public CO2CalculationNotEnabledException() {
    }

    public CO2CalculationNotEnabledException(String message) {
        super(message);
    }

    public CO2CalculationNotEnabledException(String message, Throwable cause) {
        super(message, cause);
    }

    public CO2CalculationNotEnabledException(Throwable cause) {
        super(cause);
    }
}
