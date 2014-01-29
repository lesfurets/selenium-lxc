/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package net.courtanet.selenium.common.selenium;

import org.apache.log4j.Logger;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

/**
 * Utility class used to execute command avoiding {@link StaleElementReferenceException}
 */
public class SeleniumExecutor {

    private static final Logger LOGGER = Logger.getLogger(SeleniumExecutor.class);

    private static final int DEFAULT_ATTEMPS = 2;

    public static void execute(Runnable runnable) {
        execute(runnable, DEFAULT_ATTEMPS);
    }

    /**
     * execute the given runnable and retry if {@link StaleElementReferenceException} a is catched (i.e. a
     * {@link WebElement} reference is obsolete), until retryCount is 0.
     * 
     * @param runnable the {@link Runnable} to use
     * @param retryCount the retry count to use
     * @throw RuntimeException
     */
    public static void execute(Runnable runnable, int retryCount) {
        try {
            runnable.run();
        } catch (StaleElementReferenceException e) {
            // web element is outdated, wait to page to be loaded
            try {
                Thread.sleep(SeleniumTimeout.RETRY_TIMER.getTimeout());
            } catch (InterruptedException interruptedException) {
                LOGGER.warn("interruptedException catched, ignoring", interruptedException);
            }
            if (retryCount > 0) {
                // if retry count > 0, re-execute runnable
                retryCount--;
                execute(runnable, retryCount);
            } else {
                // otherwise stop and throw an exception
                throw new RuntimeException("element not found", e);
            }
        }
    }
}
