package org.prebid.mobile.prebidkotlindemo.ui

import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.prebid.mobile.Host
import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.prebidkotlindemo.R
import org.prebid.mobile.prebidkotlindemo.testcases.TestCase
import org.prebid.mobile.prebidkotlindemo.utils.PrebidServer
import org.prebid.mobile.prebidkotlindemo.utils.Settings
import java.net.HttpURLConnection
import java.net.URL

/** Runs on the Teads server. Needs teadsPbsHost in local.properties and VPN. Skipped otherwise. */
class TeadsInterstitialAdsTest : BaseAdsTest() {

    private var previousServer = PrebidServer.PREBID

    @Before
    fun selectTeadsServer() {
        assumeTrue("teadsPbsHost not set", PrebidServer.TEADS.isAvailable)
        assumeTrue("Teads PBS unreachable - VPN?", isReachable(PrebidServer.TEADS.statusUrl))
        // Same as picking it in the toolbar.
        previousServer = Settings.get().prebidServer
        Settings.get().prebidServer = PrebidServer.TEADS
        PrebidServer.TEADS.apply()
    }

    @After
    fun restoreServer() {
        Settings.get().prebidServer = previousServer
        previousServer.apply()
    }

    @Test
    fun test() {
        testAd(R.string.teads_display_interstitial)
    }

    override fun checkAd(testCase: TestCase) {
        val closeButton = device.wait(Until.findObject(By.res(packageName, "iv_close_interstitial")), timeout)
        assertNotNull("Interstitial not shown", closeButton)

        closeButton.click()
        Thread.sleep(1000)
        device.pressBack()

        // The use case must not change the server picked in the toolbar.
        assertEquals(PrebidServer.TEADS.hostUrl, Host.CUSTOM.hostUrl)
        assertEquals(PrebidServer.TEADS.accountId, PrebidMobile.getPrebidServerAccountId())
    }

    private fun isReachable(url: String): Boolean = try {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 3000
        connection.readTimeout = 3000
        val code = connection.responseCode
        connection.disconnect()
        code == 200
    } catch (e: Exception) {
        false
    }

}
