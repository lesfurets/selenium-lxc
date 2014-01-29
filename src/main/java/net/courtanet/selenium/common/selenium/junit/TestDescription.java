/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package net.courtanet.selenium.common.selenium.junit;

import static net.courtanet.selenium.common.selenium.SeleniumConfiguration.SCREENSHOT_OUTPUT_DIR;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

public class TestDescription extends TestWatcher {

    private static final Logger LOGGER = Logger.getLogger(TestDescription.class.getSimpleName());

    private Description description;

    @Override
    protected void starting(Description description) {
        this.description = description;
    }

    public Description getDescription() {
        return description;
    }

    public void setDescription(Description description) {
        this.description = description;
    }

    public String getTestQualifiedName() {
        return description.getClassName() + "." + description.getMethodName();
    }

    @Override
    protected void failed(Throwable e, Description description) {
        LOGGER.error("Test " + getTestQualifiedName() + " failed!", e);
    }

    @Override
    protected void succeeded(Description d) {
        LOGGER.info("Test " + getTestQualifiedName() + " succedeed.");
    }

    public void captureBrowser(WebDriver driver) {
        captureBrowser(driver, "");
    }

    public void captureBrowser(WebDriver driver, String suffix) {
        if (driver == null) {
            throw new IllegalStateException("driver is null");
        }
        if (!(driver instanceof TakesScreenshot)) {
            LOGGER.warn("driver " + driver.getClass().getName() + " does not support taking screenshots");
            return;
        }
        try {
            String screenshotFileName = getTestQualifiedName() + (suffix != null ? suffix : "");
            File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String fileName = (screenshotFileName == null || screenshotFileName.isEmpty()) ? "error-"
                            + System.currentTimeMillis() : screenshotFileName;
            LOGGER.info("Trying to take screenshot :" + fileName);
            File destFile = new File(SCREENSHOT_OUTPUT_DIR.propertyValue() + File.separator + fileName + ".png");
            FileUtils.copyFile(scrFile, destFile);
            LOGGER.info("Screenshot taken : " + fileName);
        } catch (Exception e) {
            LOGGER.info("Failed to generate screenshot ");
        }
    }
}
