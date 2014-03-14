/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package net.courtanet.selenium.common;

import static net.courtanet.selenium.common.selenium.SeleniumConfiguration.SITE_URL_CONTEXT;

import org.junit.Assert;

import net.courtanet.selenium.common.selenium.junit.SiteTest;

public abstract class MireTest extends SiteTest {

    protected void testPage(String source) {
        getDriver().navigate().to(SITE_URL_CONTEXT.propertyValue() + "/" + source);
        Assert.assertTrue(getDriver().getCurrentUrl().contains("/" + source));

    }

}
