/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package net.courtanet.selenium.common.selenium;

import static java.lang.System.getProperty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public enum SeleniumConfiguration {

    SITE_URL_CONTEXT("seleniumContext", "http://www.google.com"), //

    REMOTE_GRID_ENABLE("remoteGridExecution", false), //
    REMOTE_GRID_URL("remoteGridURL", "http://localhost:4444/wd/hub/"), //

    MOBILE_USER_AGENT("mobileUserAgent", false), //

    SCREENSHOT_OUTPUT_DIR("screenshotOutputDir", "target");

    private final String parameter;
    private final String defaultValue;

    private SeleniumConfiguration(String parameter, boolean defaultValue) {
        this(parameter, Boolean.toString(defaultValue));
    }

    private SeleniumConfiguration(String parameter, String defaultValue) {
        this.parameter = parameter;
        this.defaultValue = defaultValue;
    }

    public String propertyValue() {
        return isEmpty(getProperty(parameter)) ? defaultValue : getProperty(parameter);
    }

    public boolean propertyBooleanValue() {
        String value = propertyValue();
        return value != null ? Boolean.valueOf(value) : null;
    }

    public static boolean isMobile() {
        return MOBILE_USER_AGENT.propertyBooleanValue();
    }
}
