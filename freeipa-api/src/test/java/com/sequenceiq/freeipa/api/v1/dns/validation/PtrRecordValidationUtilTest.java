package com.sequenceiq.freeipa.api.v1.dns.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PtrRecordValidationUtilTest {
    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    public void setUp() {
        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    }

    @Test
    public void testValidIpInZone() {
        boolean valid = PtrRecordValidationUtil.isIpInZoneRange("192.168.1.10", "1.168.192.in-addr.arpa.", context);
        assertTrue(valid);
    }

    @Test
    public void testInvalidIpNotInZone() {
        boolean valid = PtrRecordValidationUtil.isIpInZoneRange("192.168.2.10", "1.168.192.in-addr.arpa", context);
        assertFalse(valid);
        verify(context).buildConstraintViolationWithTemplate("Ip 192.168.2.10 is not in the provided reverse dns zone: 1.168.192.in-addr.arpa");
        verify(violationBuilder).addConstraintViolation();
    }

    @Test
    public void testEmptyDnsZone() {
        boolean valid = PtrRecordValidationUtil.isIpInZoneRange("192.168.1.10", "", context);
        assertTrue(valid);
    }

    @Test
    public void testNullDnsZone() {
        boolean valid = PtrRecordValidationUtil.isIpInZoneRange("192.168.1.10", null, context);
        assertTrue(valid);
    }

    @Test
    public void testNullIp() {
        boolean valid = PtrRecordValidationUtil.isIpInZoneRange(null, "1.168.192.in-addr.arpa", context);
        verify(context).buildConstraintViolationWithTemplate("Ip parameter is missing.");
        assertFalse(valid);
    }

    @Test
    public void testEmptyIp() {
        boolean valid = PtrRecordValidationUtil.isIpInZoneRange("", "1.168.192.in-addr.arpa.", context);
        verify(context).buildConstraintViolationWithTemplate("Ip parameter is missing.");
        assertFalse(valid);
    }
}
