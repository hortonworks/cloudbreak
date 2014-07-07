package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.io.IOException;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.SnsRequest;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class SnsMessageParserTest {

    private SnsMessageParser underTest = new SnsMessageParser();

    @Test
    public void testSnsRequestParser() throws IOException {
        String request = FileReaderUtils.readFileFromClasspath("sample-sns-request");
        SnsRequest snsRequest = underTest.parseRequest(request);
        Assert.assertEquals("AWS CloudFormation Notification", snsRequest.getSubject());
        Assert.assertEquals("2014-06-17T09:41:29.564Z", snsRequest.getTimestamp());
    }

    @Test
    public void testSnsCFMessageParser() throws IOException {
        String request = FileReaderUtils.readFileFromClasspath("sample-sns-request");
        SnsRequest snsRequest = underTest.parseRequest(request);

        Map<String, String> result = underTest.parseCFMessage(snsRequest.getMessage());

        Assert.assertEquals("sns-test-200", result.get("StackName"));
        Assert.assertEquals("DELETE_COMPLETE", result.get("ResourceStatus"));

    }

}
