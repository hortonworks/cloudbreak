package com.sequenceiq.cloudbreak.util;

import java.security.PublicKey;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PublicKeyReaderUtilTest {

    @Test
    public void testRsaKeyMustUseAlgorithmRSA() throws PublicKeyReaderUtil.PublicKeyParseException {
        PublicKey rsaKey = PublicKeyReaderUtil.load("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQDfz7EED7gE8qbDKg6tUMtL3yW/" +
                "W1kiGzWBbVhOMiFYjnVje8mNF5jNe8rj+tg6fKA9dMpULrTIaFC5Dbhq4c0vCRqTbzrPkrXCURAdWx1lb+NzfTIOqLVeUZlnh" +
                "NPXBPVss+pnkawsI4HUiMRuScQnzrbvY9HcLhb69Qe9sZvSx1R46F9BEOZduX3QJxV2NBer6zJAZxv/YJt/ObzSFSnWN5isG+X" +
                "x/tIUZHighuqH7k0pzanp8IH11O6qXuCyqk9c7110w/vlQ+N8N3UhtAOhPCljKjYJo0KgF9C//l9eVKi/QICGVbr6vzvAyiWg" +
                "uf1lMHUEmsDlGQqfq/nnKCQYlz2V6oOaPt6ggmdmNCppFOSXri0dm1X4TnLASpIFetdjFGTU+sH75SmSrPlYn5HOxcqeVGgHNIZ2" +
                "taY+u12DWtY7cqFXvHE6pFmPme2Z0wQrAq/rH2BqGl3pkWVDIItbtsVsVTlS6E1Y/dXJkjaBiBZTc7aeBLa58oj5F4JYb" +
                "sc= test@test.local");
        Assertions.assertTrue(rsaKey.getAlgorithm().equals("RSA"));
    }
}