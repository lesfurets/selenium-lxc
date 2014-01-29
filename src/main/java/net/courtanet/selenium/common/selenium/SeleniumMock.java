/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package net.courtanet.selenium.common.selenium;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SeleniumMock {

    private static Map<Long, Boolean> mocked_settings_by_thread = new ConcurrentHashMap<>();

    public static boolean isMockedOutput() {
        Boolean customSetting = mocked_settings_by_thread.get(Thread.currentThread().getId());
        return customSetting != null ? customSetting : true;
    }

    public static synchronized void setMockedOutput(boolean value) {
        mocked_settings_by_thread.put(Thread.currentThread().getId(), value);
    }
}
