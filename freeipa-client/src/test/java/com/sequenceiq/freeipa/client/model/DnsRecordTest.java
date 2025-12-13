package com.sequenceiq.freeipa.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DnsRecordTest {

    private DnsRecord underTest;

    @BeforeEach
    public void init() {
        underTest = new DnsRecord();
    }

    @Test
    void testIpRelatedARecordMatch() {
        underTest.setArecord(List.of("192.168.1.2", "10.1.1.1"));
        assertTrue(underTest.isIpRelatedRecord("10.1.1.1", null));
    }

    @Test
    void testIpRelatedARecordNoMatch() {
        underTest.setArecord(List.of("192.168.1.2", "10.1.1.1"));
        assertFalse(underTest.isIpRelatedRecord("10.1.1.2", null));
    }

    @Test
    void testIpRelatedPtrRecordMatch() {
        underTest.setIdnsname("2.1");
        underTest.setPtrrecord(List.of("server"));
        assertTrue(underTest.isIpRelatedRecord("10.1.1.2", "1.10.in-addr.arpa."));
    }

    @Test
    void testIpRelatedPtrRecordCClassMatch() {
        underTest.setIdnsname("2");
        underTest.setPtrrecord(List.of("server"));
        assertTrue(underTest.isIpRelatedRecord("10.3.1.2", "1.3.10.in-addr.arpa."));
    }

    @Test
    void testIpRelatedPtrRecordAClassMatch() {
        underTest.setIdnsname("2.3.4");
        underTest.setPtrrecord(List.of("server"));
        assertTrue(underTest.isIpRelatedRecord("10.4.3.2", "10.in-addr.arpa."));
    }

    @Test
    void testIpRelatedPtrRecordDifferentIp() {
        underTest.setIdnsname("2.2");
        underTest.setPtrrecord(List.of("server"));
        assertFalse(underTest.isIpRelatedRecord("10.1.1.2", "1.10.in-addr.arpa."));
    }

    @Test
    void testIpRelatedPtrRecordDifferentZone() {
        underTest.setIdnsname("2.1");
        underTest.setPtrrecord(List.of("server"));
        assertFalse(underTest.isIpRelatedRecord("10.1.1.2", "2.10.in-addr.arpa."));
    }

    @Test
    void testIpRelatedPtrRecordFalseIfNotPtrNotARecord() {
        assertFalse(underTest.isIpRelatedRecord("10.1.1.2", "2.10.in-addr.arpa."));
    }

    @Test
    void testIsSrvRecordTrue() {
        underTest.setSrvrecord(List.of("0 5 5060 example.com."));
        assertTrue(underTest.isSrvRecord());
    }

    @Test
    void testIsSrvRecordFalse() {
        underTest.setArecord(List.of("192.168.1.2"));
        assertFalse(underTest.isSrvRecord());
    }

    @Test
    void testIsSshfpRecordTrue() {
        underTest.setIdnsname("server");
        underTest.setSshfprecord(List.of("1 1 ABCDEF"));
        assertTrue(underTest.isSshfpRecord());
    }

    @Test
    void testIsSshfpRecordFalse() {
        underTest.setArecord(List.of("192.168.1.2"));
        assertFalse(underTest.isSshfpRecord());
    }

    @Test
    void testIsHostRelatedRecordFalseIfSrvRecrod() {
        underTest.setSrvrecord(List.of("0 5 5060 example.com."));
        assertFalse(underTest.isHostRelatedRecord("example.com.", "example.com"));
    }

    @Test
    void testIsHostRelatedSrvRecordFalseIfNotSrvRecrod() {
        underTest.setPtrrecord(List.of("server"));
        assertFalse(underTest.isHostRelatedSrvRecord("server.example.com."));
        assertFalse(underTest.isHostRelatedSrvRecord("server"));
    }

    @Test
    void testIsHostRelatedSrvRecordFalseIfNoMatch() {
        underTest.setSrvrecord(List.of("0 5 5060 example.com.", "0 5 5060 example1.com.", "0 5 5060 www.example2.com."));
        assertFalse(underTest.isHostRelatedSrvRecord("www.example.com."));
        assertFalse(underTest.isHostRelatedSrvRecord("www.example1.com."));
        assertFalse(underTest.isHostRelatedSrvRecord("example2.com."));
    }

    @Test
    void testIsHostRelatedSrvRecordTrueIfMatch() {
        underTest.setSrvrecord(List.of("0 5 5060 example.com.", "0 5 5060 example1.com."));
        assertTrue(underTest.isHostRelatedSrvRecord("example.com."));
        assertTrue(underTest.isHostRelatedSrvRecord("example1.com."));
    }

    @Test
    void testIsHostRelatedRecordWhenARecord() {
        underTest.setIdnsname("server");
        underTest.setArecord(List.of("192.168.1.2"));
        assertTrue(underTest.isHostRelatedRecord("server.example.com", "example.com"));
        assertFalse(underTest.isHostRelatedRecord("server1.example.com", "example.com"));
    }

    @Test
    void testIsHostRelatedRecordWhenPtrRecord() {
        underTest.setPtrrecord(List.of("server.example.com."));
        assertTrue(underTest.isHostRelatedRecord("server.example.com", "example.com"));
        assertFalse(underTest.isHostRelatedRecord("server1.example.com", "example.com"));
    }

    @Test
    void testIsHostRelatedRecordWhenSshfpRecord() {
        underTest.setIdnsname("server");
        underTest.setSshfprecord(List.of("1 1 ABCDEF"));
        assertTrue(underTest.isHostRelatedRecord("server.example.com", "example.com"));
        assertFalse(underTest.isHostRelatedRecord("server1.example.com", "example.com"));
    }

    @Test
    void testCalcZoneFromNsRecordWithValidNsRecord() {
        underTest.setIdnsname("@");
        underTest.setNsrecord(List.of("ns1.example.com."));
        underTest.setDn("idnsname=191.84.10.in-addr.arpa.,cn=dns,dc=hybrid,dc=xcu2-8y8x,dc=wl,dc=cloudera,dc=site");

        Optional<String> result = underTest.calcZoneFromNsRecord();

        assertTrue(result.isPresent());
        assertEquals("191.84.10.in-addr.arpa.", result.get());
    }

    @Test
    void testCalcZoneFromNsRecordWithValidForwardZone() {
        underTest.setIdnsname("@");
        underTest.setNsrecord(List.of("ns1.example.com.", "ns2.example.com."));
        underTest.setDn("idnsname=example.com.,cn=dns,dc=hybrid,dc=xcu2-8y8x,dc=wl,dc=cloudera,dc=site");

        Optional<String> result = underTest.calcZoneFromNsRecord();

        assertTrue(result.isPresent());
        assertEquals("example.com.", result.get());
    }

    @Test
    void testCalcZoneFromNsRecordWithNonNsRecord() {
        underTest.setIdnsname("server");
        underTest.setArecord(List.of("192.168.1.1"));
        underTest.setDn("idnsname=server,idnsname=example.com.,cn=dns,dc=hybrid,dc=xcu2-8y8x,dc=wl,dc=cloudera,dc=site");

        Optional<String> result = underTest.calcZoneFromNsRecord();

        assertFalse(result.isPresent());
    }

    @Test
    void testCalcZoneFromNsRecordWithEmptyNsRecord() {
        underTest.setIdnsname("@");
        underTest.setNsrecord(List.of());
        underTest.setDn("idnsname=example.com.,cn=dns,dc=hybrid,dc=xcu2-8y8x,dc=wl,dc=cloudera,dc=site");

        Optional<String> result = underTest.calcZoneFromNsRecord();

        assertFalse(result.isPresent());
    }

    @Test
    void testCalcZoneFromNsRecordWithNullNsRecord() {
        underTest.setIdnsname("@");
        underTest.setNsrecord(null);
        underTest.setDn("idnsname=example.com.,cn=dns,dc=hybrid,dc=xcu2-8y8x,dc=wl,dc=cloudera,dc=site");

        Optional<String> result = underTest.calcZoneFromNsRecord();

        assertFalse(result.isPresent());
    }

    @Test
    void testCalcZoneFromNsRecordWithNullDn() {
        underTest.setIdnsname("@");
        underTest.setNsrecord(List.of("ns1.example.com."));
        underTest.setDn(null);

        Optional<String> result = underTest.calcZoneFromNsRecord();

        assertFalse(result.isPresent());
    }

    @Test
    void testCalcZoneFromNsRecordWithMalformedDn() {
        underTest.setIdnsname("@");
        underTest.setNsrecord(List.of("ns1.example.com."));
        underTest.setDn("malformed-dn-without-comma");

        Optional<String> result = underTest.calcZoneFromNsRecord();

        assertTrue(result.isPresent());
        assertEquals("malformed-dn-without-comma", result.get());
    }
}