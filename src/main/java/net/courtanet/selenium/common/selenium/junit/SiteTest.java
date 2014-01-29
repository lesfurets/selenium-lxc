/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package net.courtanet.selenium.common.selenium.junit;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

import net.courtanet.selenium.common.selenium.SeleniumDriver;

public abstract class SiteTest {

    @Rule
    public TestDescription watchman = new TestDescription();

    private WebDriver driver;

    public WebDriver getDriver() {
        return driver;
    }

    @Before
    public void setUp() {
        driver = SeleniumDriver.INSTANCE.getDriver();
        PageFactory.initElements(getDriver(), this);
    }

    @After
    public void tearDown() {
        if (driver != null) {
            watchman.captureBrowser(driver);
            try {
                driver.quit();
            } catch (Exception e) {
                // ignore failure from grid when closing driver
            }
        }
    }

}
