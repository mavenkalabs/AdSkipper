package com.mavenkalabs.adskipper.util;

import org.junit.Test;

public class AppLogTest {
    @Test
    public void verifyMessageLoggingWithParams() {
        AppLog.enable(null);
        AppLog.d("TAG", "This is a {0} test with {1} and {2}", "Super", null, 1);
        AppLog.disable();
    }
}
