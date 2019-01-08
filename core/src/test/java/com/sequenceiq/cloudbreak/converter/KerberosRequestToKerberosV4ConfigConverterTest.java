package com.sequenceiq.cloudbreak.converter;

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.ActiveDirectoryKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.AmbariKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.FreeIPAKerberosDescriptor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosTypeBase;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.MITKerberosDescriptor;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.v2.KerberosRequestToKerberosConfigConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosTypeResolver;

@RunWith(Parameterized.class)
public class KerberosRequestToKerberosV4ConfigConverterTest extends AbstractConverterTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private KerberosTypeResolver kerberosTypeResolver;

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private KerberosRequestToKerberosConfigConverter underTest;

    private KerberosData testData;

    public KerberosRequestToKerberosV4ConfigConverterTest(KerberosData testData) {
        this.testData = testData;
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Parameterized.Parameters()
    public static Object[] data() {
        return new Object[]{
                KerberosData.FREEIPA,
                KerberosData.CUSTOM,
                KerberosData.ACTIVE_DIRECTORY,
                KerberosData.MIT,
        };
    }

    @Test
    public void testConverterWhenKerberosTypeResolverReturnsASpecificKerberosTypeThenThatShouldBeConvertIntoAKerberosConfig() {
        KerberosV4Request request = testData.getRequest();
        KerberosTypeBase actualType = testData.getActualType();
        KerberosConfig expected = testData.getExpected();
        when(kerberosTypeResolver.propagateKerberosConfiguration(request)).thenReturn(actualType);
        when(conversionService.convert(actualType, KerberosConfig.class)).thenReturn(expected);

        KerberosConfig result = underTest.convert(request);

        Assert.assertNotNull(result);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testConvertWhenConversionComponentDoesNotWorkProperlyAndReturnsNullThenBadRequestExceptionComes() {
        KerberosV4Request request = testData.getRequest();
        KerberosTypeBase actualType = testData.getActualType();
        when(kerberosTypeResolver.propagateKerberosConfiguration(testData.getRequest())).thenReturn(actualType);
        when(conversionService.convert(actualType, KerberosConfig.class)).thenReturn(null);

        thrown.expectMessage("Obtaining KerberosConfig from KerberosTypeBase was unsuccessful, it has returned null. "
                + "Further operations are impossible");
        thrown.expect(BadRequestException.class);

        underTest.convert(request);
    }

    private enum KerberosData {

        FREEIPA {
            @Override
            public KerberosV4Request getRequest() {
                KerberosV4Request request = new KerberosV4Request();
                request.setFreeIpa(new FreeIPAKerberosDescriptor());
                return request;
            }

            @Override
            public KerberosConfig getExpected() {
                return new KerberosConfig();
            }

            @Override
            public KerberosTypeBase getActualType() {
                return new FreeIPAKerberosDescriptor();
            }
        },

        MIT {
            @Override
            public KerberosV4Request getRequest() {
                KerberosV4Request request = new KerberosV4Request();
                request.setMit(new MITKerberosDescriptor());
                return request;
            }

            @Override
            public KerberosConfig getExpected() {
                return new KerberosConfig();
            }

            @Override
            public KerberosTypeBase getActualType() {
                return new MITKerberosDescriptor();
            }
        },

        ACTIVE_DIRECTORY {
            @Override
            public KerberosV4Request getRequest() {
                KerberosV4Request request = new KerberosV4Request();
                request.setActiveDirectory(new ActiveDirectoryKerberosDescriptor());
                return request;
            }

            @Override
            public KerberosConfig getExpected() {
                return new KerberosConfig();
            }

            @Override
            public KerberosTypeBase getActualType() {
                return new ActiveDirectoryKerberosDescriptor();
            }
        },

        CUSTOM {
            @Override
            public KerberosV4Request getRequest() {
                KerberosV4Request request = new KerberosV4Request();
                request.setAmbariDescriptor(new AmbariKerberosDescriptor());
                return request;
            }

            @Override
            public KerberosConfig getExpected() {
                return new KerberosConfig();
            }

            @Override
            public KerberosTypeBase getActualType() {
                return new AmbariKerberosDescriptor();
            }
        };

        public abstract KerberosV4Request getRequest();

        public abstract KerberosConfig getExpected();

        public abstract KerberosTypeBase getActualType();

    }

}