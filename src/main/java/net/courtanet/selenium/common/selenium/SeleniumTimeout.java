/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package net.courtanet.selenium.common.selenium;

import org.openqa.selenium.StaleElementReferenceException;

public enum SeleniumTimeout {

    /**
     * default timeout for navigation
     */
    NAVIGATION(6000), //

    /**
     * default timeout for any wait related to any RPC async callback
     */
    ASYNC_CALLBACK(3000), //


    /**
     * number of milliseconds to wait if {@link StaleElementReferenceException} catched
     */
    RETRY_TIMER(500);//

    private final int timeout;

    private SeleniumTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
    }
}
