/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package net.courtanet.selenium.common.selenium;

import java.net.URL;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DriverCommand;

/**
 * Custom {@link org.openqa.selenium.remote.RemoteWebDriver} that supports {@link TakesScreenshot}
 */
public class RemoteWebDriver extends org.openqa.selenium.remote.RemoteWebDriver implements TakesScreenshot {

    public RemoteWebDriver(URL remoteAddress, Capabilities desiredCapabilities) {
        super(remoteAddress, desiredCapabilities);
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        try {
            if ((Boolean) getCapabilities().getCapability(CapabilityType.TAKES_SCREENSHOT)) {
                String base64Str = execute(DriverCommand.SCREENSHOT).getValue().toString();
                return target.convertFromBase64Png(base64Str);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
