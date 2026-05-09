package com.mavenkalabs.adskipper.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Files;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AppLogTest {

    @Mock
    private Context mockContext;

    @Mock
    private ContentResolver mockResolver;

    private AutoCloseable mocksCloseable;

    @Before
    public void setUp() throws Exception {
        mocksCloseable = MockitoAnnotations.openMocks(this);
        when(mockContext.getContentResolver()).thenReturn(mockResolver);
        when(mockResolver.insert(any(), any())).thenReturn(Uri.EMPTY);
        when(mockResolver.openOutputStream(any()))
                .thenReturn(Files.newOutputStream((Files.createTempFile("_adskipper_", null))));
    }

    @After
    public void tearDown() throws Exception {
        AppLog.disable();

        mocksCloseable.close();
    }

    @Test
    public void verifyMessageLoggingWithParams() {
        AppLog.enable(null);
        AppLog.d("TAG", "This is a {0} test with {1} and {2}", "Super", null, 1);
    }


    @Test
    public void verifyExceptionLogging() {
        AppLog.enable(null);
        AppLog.e("TAG", "This is a {0} test with {1} and {2}", new Exception(),"Super", null, 1);
    }

    @Test
    public void verifyMessageLoggingWithParamsWithContext() {
        AppLog.enable(mockContext);
        AppLog.d("TAG", "This is a {0} test with {1} and {2}", "Super", null, 1);
    }


    @Test
    public void verifyExceptionLoggingWithContext() {
        AppLog.enable(mockContext);
        AppLog.e("TAG", "This is a {0} test with {1} and {2}", new Exception(),"Super", null, 1);
    }
}
