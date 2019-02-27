package com.sequenceiq.it.cloudbreak.exception;

public class TestCaseDescriptionMissingException extends RuntimeException {

    public TestCaseDescriptionMissingException() {
        super("Every testcase need a testcase description please be kind and add one "
                + "https://media1.giphy.com/media/LFcBAM7zbjRvO/giphy.gif?cid=3640f6095c76a24c4f4e68316f6f8e6b");
    }

}