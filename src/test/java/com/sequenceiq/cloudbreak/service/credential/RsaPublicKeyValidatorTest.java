package com.sequenceiq.cloudbreak.service.credential;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.Credential;

public class RsaPublicKeyValidatorTest {

    private RsaPublicKeyValidator rsaPublicKeyValidator;

    @Before
    public void setUp() {
        rsaPublicKeyValidator = new RsaPublicKeyValidator();
    }

    @Test
    public void validPublicKeyWillNotFail() {
        rsaPublicKeyValidator.validate(
                azureCredential(
                        "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
                                + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
                                + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
                                + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
                                + "KR495VFmuOepLYz5I8Dn sequence-eu")
        );
    }

    @Test(expected = BadRequestException.class)
    public void inValidPublicKeyWillFailWhenMissingUser() {
        rsaPublicKeyValidator.validate(
                azureCredential(
                        "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
                                + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
                                + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
                                + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
                                + "KR495VFmuOepLYz5I8Dn")
        );
    }

    @Test(expected = BadRequestException.class)
    public void inValidPublicKeyWillFailWhenMissingSshRsa() {
        rsaPublicKeyValidator.validate(
                azureCredential(
                        "AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
                                + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
                                + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
                                + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
                                + "KR495VFmuOepLYz5I8Dn sequence-eu")
        );
    }

    @Test(expected = BadRequestException.class)
    public void inValidPublicKeyWillFailWhenTooShortBody() {
        rsaPublicKeyValidator.validate(
                azureCredential(
                        "ssh-rsa AAAA sequence-eu")
        );
    }

    @Test(expected = BadRequestException.class)
    public void inValidPublicKeyWillFailWhenMissingUserAndSshRsa() {
        rsaPublicKeyValidator.validate(
                azureCredential(
                        "AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
                                + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
                                + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
                                + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
                                + "KR495VFmuOepLYz5I8Dn")
        );
    }

    @Test(expected = BadRequestException.class)
    public void inValidPublicKeyWillFailWhenTotallyInvalidString() {
        rsaPublicKeyValidator.validate(
                azureCredential(
                        "ImBAD")
        );
    }

    private Credential azureCredential(String publicKey) {
        Credential credential = new AwsCredential();
        credential.setPublicKey(publicKey);
        return credential;
    }
}