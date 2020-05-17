package com.sequenceiq.freeipa.client.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DnsRecordTest {

    private DnsRecord underTest;

    @Before
    public void init() {
        underTest = new DnsRecord();
    }

    @Test
    public void testIpRelatedARecordMatch() {
        underTest.setArecord(List.of("192.168.1.2", "10.1.1.1"));
        assertTrue(underTest.isIpRelatedRecord("10.1.1.1", null));
    }

    @Test
    public void testIpRelatedARecordNoMatch() {
        underTest.setArecord(List.of("192.168.1.2", "10.1.1.1"));
        assertFalse(underTest.isIpRelatedRecord("10.1.1.2", null));
    }

    @Test
    public void testIpRelatedPtrRecordMatch() {
        underTest.setIdnsname("2.1");
        underTest.setPtrrecord(List.of("server"));
        assertTrue(underTest.isIpRelatedRecord("10.1.1.2", "1.10.in-addr.arpa."));
    }

    @Test
    public void testIpRelatedPtrRecordCClassMatch() {
        underTest.setIdnsname("2");
        underTest.setPtrrecord(List.of("server"));
        assertTrue(underTest.isIpRelatedRecord("10.3.1.2", "1.3.10.in-addr.arpa."));
    }

    @Test
    public void testIpRelatedPtrRecordAClassMatch() {
        underTest.setIdnsname("2.3.4");
        underTest.setPtrrecord(List.of("server"));
        assertTrue(underTest.isIpRelatedRecord("10.4.3.2", "10.in-addr.arpa."));
    }

    @Test
    public void testIpRelatedPtrRecordDifferentIp() {
        underTest.setIdnsname("2.2");
        underTest.setPtrrecord(List.of("server"));
        assertFalse(underTest.isIpRelatedRecord("10.1.1.2", "1.10.in-addr.arpa."));
    }

    @Test
    public void testIpRelatedPtrRecordDifferentZone() {
        underTest.setIdnsname("2.1");
        underTest.setPtrrecord(List.of("server"));
        assertFalse(underTest.isIpRelatedRecord("10.1.1.2", "2.10.in-addr.arpa."));
    }

    @Test
    public void testIpRelatedPtrRecordFalseIfNotPtrNotARecord() {
        assertFalse(underTest.isIpRelatedRecord("10.1.1.2", "2.10.in-addr.arpa."));
    }

    @Test
    public void testIsSrvRecordTrue() {
        underTest.setSrvrecord(List.of("0 5 5060 example.com."));
        assertTrue(underTest.isSrvRecord());
    }

    @Test
    public void testIsSrvRecordFalse() {
        underTest.setArecord(List.of("192.168.1.2"));
        assertFalse(underTest.isSrvRecord());
    }

    @Test
    public void testIsHostRelatedRecordFalseIfSrvRecrod() {
        underTest.setSrvrecord(List.of("0 5 5060 example.com."));
        assertFalse(underTest.isHostRelatedRecord("example.com.", "example.com"));
    }

    @Test
    public void testIsHostRelatedSrvRecordFalseIfNotSrvRecrod() {
        underTest.setPtrrecord(List.of("server"));
        assertFalse(underTest.isHostRelatedSrvRecord("server.example.com."));
        assertFalse(underTest.isHostRelatedSrvRecord("server"));
    }

    @Test
    public void testIsHostRelatedSrvRecordFalseIfNoMatch() {
        underTest.setSrvrecord(List.of("0 5 5060 example.com.", "0 5 5060 example1.com.", "0 5 5060 www.example2.com."));
        assertFalse(underTest.isHostRelatedSrvRecord("www.example.com."));
        assertFalse(underTest.isHostRelatedSrvRecord("www.example1.com."));
        assertFalse(underTest.isHostRelatedSrvRecord("example2.com."));
    }

    @Test
    public void testIsHostRelatedSrvRecordTrueIfMatch() {
        underTest.setSrvrecord(List.of("0 5 5060 example.com.", "0 5 5060 example1.com."));
        assertTrue(underTest.isHostRelatedSrvRecord("example.com."));
        assertTrue(underTest.isHostRelatedSrvRecord("example1.com."));
    }
}