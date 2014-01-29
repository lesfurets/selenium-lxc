/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package net.courtanet.selenium.common.selenium;

import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Duration;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Sleeper;
import org.openqa.selenium.support.ui.SystemClock;

public class SeleniumWait extends FluentWait<WebDriver> {

    public SeleniumWait(WebDriver input) {
        super(input, new SystemClock(), new Sleeper() {
            @Override
            public void sleep(Duration duration) {
            }
        });

        ignoring(NoSuchElementException.class, StaleElementReferenceException.class);
        pollingEvery(100, TimeUnit.MILLISECONDS);
    }

    /**
     * Sets how long to wait for the evaluated condition to be true.
     * 
     * @param timeout The timeout duration.
     * @return A self reference.
     */
    public SeleniumWait withTimeout(SeleniumTimeout timeout) {
        withTimeout(timeout.getTimeout(), TimeUnit.MILLISECONDS);
        return this;
    }

    /**
     * @See {@link ExpectedConditions#visibilityOfAllElementsLocatedBy(By)}
     */
    public WebElement untilVisible(By by) {
        return until(visibilityOfElementLocated(by));
    }

    /**
     * @See {@link ExpectedConditions#visibilityOf(WebElement)}
     */
    public WebElement untilVisible(WebElement element) {
        return until(visibilityOf(element));
    }

    /**
     * @See {@link ExpectedConditions#invisibilityOfElementLocated(By)}
     */
    public Boolean untilHidden(By by) {
        return until(invisibilityOfElementLocated(by));
    }

    /**
     * @See {@link ExpectedConditions#presenceOfElementLocated(By)}
     */
    public WebElement untilPresent(By by) {
        return until(presenceOfElementLocated(by));
    }

    public WebElement untilListBoxFilled(WebElement element) {
        return until(new ListBoxFilled(element));
    }


    private static final class ListBoxFilled implements ExpectedCondition<WebElement> {
        private final WebElement element;

        private ListBoxFilled(WebElement element) {
            this.element = element;
        }

        @Override
        public WebElement apply(WebDriver driver) {
            boolean visible = element != null && element.isDisplayed();
            List<WebElement> options = element.findElements(By.tagName("option"));
            if (!visible) {
                throw new NoSuchElementException("Element " + element + " not visible");
            }
            if (options.size() <= 1) {
                throw new NoSuchElementException("Element " + element + " visible but empty");
            }
            return element;
        }

        @Override
        public String toString() {
            return "visibility of not empty list " + element;
        }
    }
}
