/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package net.courtanet.selenium.common.selenium;

import static net.courtanet.selenium.common.selenium.SeleniumConfiguration.REMOTE_GRID_ENABLE;
import static net.courtanet.selenium.common.selenium.SeleniumConfiguration.REMOTE_GRID_URL;
import static net.courtanet.selenium.common.selenium.SeleniumConfiguration.isMobile;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 * @see <a href="http://seleniumhq.org/docs/03_webdriver.html">Selenium 2.0 and WebDriver</a>
 */
public enum SeleniumDriver {

    INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(SeleniumDriver.class);

    private static final String IPHONE_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 5_0 like Mac OS X)" //
                    + " AppleWebKit/534.46 (KHTML, like Gecko)" //
                    + " Version/5.1 Mobile/9A334 Safari/7534.48.3";

    public WebDriver getDriver() {
        LOGGER.info("Starting Firefox WebDriver for LesFurets");

        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("dom.max_script_run_time", "999");

        if (isMobile()) {
            // use iphone user agent
            LOGGER.info("Using Firefox with iPhone user agent");
            profile.setPreference("general.useragent.override", IPHONE_USER_AGENT);
        }

        DesiredCapabilities capability = DesiredCapabilities.firefox();
        capability.setCapability(FirefoxDriver.PROFILE, profile);

        if (REMOTE_GRID_ENABLE.propertyBooleanValue()) {
            // use selenium grid
            LOGGER.info("Using remote WebDriver : " + REMOTE_GRID_URL.propertyValue());
            try {
                return new RemoteWebDriver(new URL(REMOTE_GRID_URL.propertyValue()), capability);
            } catch (MalformedURLException e) {
                throw new RuntimeException("invalid grid URL: " + REMOTE_GRID_URL.propertyValue(), e);
            }
        } else {
            // use local firefox
            return new FirefoxDriver(capability);
        }
    }
}
