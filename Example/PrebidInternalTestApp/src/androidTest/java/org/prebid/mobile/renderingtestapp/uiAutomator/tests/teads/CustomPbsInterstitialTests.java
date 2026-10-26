package org.prebid.mobile.renderingtestapp.uiAutomator.tests.teads;

import android.Manifest;
import android.app.Activity;
import android.app.UiAutomation;
import android.os.Bundle;
import android.os.RemoteException;

import androidx.navigation.Navigation;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.prebid.mobile.rendering.sdk.JSLibraryManager;
import org.prebid.mobile.renderingtestapp.R;
import org.prebid.mobile.renderingtestapp.uiAutomator.pages.AdBasePage;
import org.prebid.mobile.renderingtestapp.uiAutomator.pages.bidding.ppm.CustomPbsInterstitialPage;
import org.prebid.mobile.renderingtestapp.uiAutomator.utils.BaseUiAutomatorTest;
import org.prebid.mobile.renderingtestapp.utils.CustomPbsSettings;

import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Interstitial against the Teads/Outbrain test Prebid Server.
 * IDs refer to the acceptance criteria. SHOULD checks use assumptions, so a miss shows as "skipped".
 * <p>
 * Instrumentation args: pbsHost (required), pbsAccountId, pbsConfigId, expectedSeat.
 */
public class CustomPbsInterstitialTests extends BaseUiAutomatorTest {

    private static final int LOAD_TIMEOUT_MS = 10000;
    private static final int CLOSE_BUTTON_TIMEOUT_S = 5;
    private static final int CENTER_TOLERANCE_PX = 8;
    private static final int JS_LIBRARIES_TIMEOUT_S = 20;

    private String expectedSeat;

    @Override
    protected int maxTries() {
        // D3: must pass without retries.
        return 1;
    }

    @Before
    @Override
    public void setup() throws RemoteException, InterruptedException {
        Bundle args = InstrumentationRegistry.getArguments();
        CustomPbsSettings.INSTANCE.setHostUrl(args.getString("pbsHost", CustomPbsSettings.DEFAULT_HOST_URL));
        CustomPbsSettings.INSTANCE.setAccountId(args.getString("pbsAccountId", CustomPbsSettings.DEFAULT_ACCOUNT_ID));
        CustomPbsSettings.INSTANCE.setConfigId(args.getString("pbsConfigId", CustomPbsSettings.DEFAULT_CONFIG_ID));
        expectedSeat = args.getString("expectedSeat", "outbrain");

        // D2
        assumeTrue("pbsHost instrumentation arg not set", !CustomPbsSettings.INSTANCE.getHostUrl().isEmpty());
        assumeTrue("PBS unreachable - VPN?", isServerReachable(CustomPbsSettings.INSTANCE.getHostUrl()));
        // The SDK refuses to render until mraid.js / omsdk.js are downloaded from the public CDN.
        assertTrue(
                "SDK JS libraries not downloaded - can the device resolve cdn.jsdelivr.net?",
                waitForSdkJsLibraries()
        );

        // HomePage.allowPermissionsIfNeeded() does not match the location dialog on recent Android versions.
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.grantRuntimePermission(INTERNAL_APP_PACKAGE, Manifest.permission.ACCESS_FINE_LOCATION);
        uiAutomation.grantRuntimePermission(INTERNAL_APP_PACKAGE, Manifest.permission.ACCESS_COARSE_LOCATION);
        super.setup();
    }

    @Test
    public void testBidResponse() throws JSONException {
        JSONObject bidInfo = openExample()
                .sdkEventShouldBePresent(AdBasePage.SdkEvent.onAdLoaded, LOAD_TIMEOUT_MS)
                .getBidInfo();
        writeArtifact("bid_info.json", bidInfo.toString(2));

        // B1
        assertEquals(CustomPbsSettings.INSTANCE.getHostUrl(), bidInfo.getString("host"));
        assertEquals(CustomPbsSettings.INSTANCE.getAccountId(), bidInfo.getString("accountId"));
        JSONObject request = bidInfo.getJSONObject("request");
        assertEquals(CustomPbsSettings.INSTANCE.getAccountId(), request.getString("publisherId"));
        assertEquals(CustomPbsSettings.INSTANCE.getConfigId(), request.getString("storedImpId"));

        // B2
        assertEquals("imp.instl", 1, request.getInt("instl"));
        assertTrue("imp.banner.format is empty", request.getJSONArray("bannerFormats").length() > 0);

        // B3
        assertTrue("price should be > 0", bidInfo.getDouble("price") > 0);
        assertTrue("adm is empty", bidInfo.getInt("admLength") > 0);
        JSONObject targeting = bidInfo.getJSONObject("targeting");
        assertTrue("hb_pb missing", targeting.has("hb_pb"));
        assertTrue("hb_bidder missing", targeting.has("hb_bidder"));

        // A8 on device (SHOULD)
        assumeTrue("A8 win/imp event URLs missing", bidInfo.getBoolean("hasWinEvent") && bidInfo.getBoolean("hasImpEvent"));

        // B4 (SHOULD)
        assumeTrue("B4 seat is " + bidInfo.optString("seat"), expectedSeat.equals(bidInfo.optString("seat")));
        assumeTrue("B4 hb_bidder is " + targeting.optString("hb_bidder"), expectedSeat.equals(targeting.optString("hb_bidder")));
    }

    @Test
    public void testRenderAndClose() {
        openExample()
                // C1
                .sdkEventShouldBePresent(AdBasePage.SdkEvent.onAdLoaded, LOAD_TIMEOUT_MS)
                .sdkEventShouldNotBePresent(AdBasePage.SdkEvent.onAdFailed, 0)
                .showPrebidInterstitial()
                // C2
                .creativeShouldNotBeBlank()
                .saveScreenshot("interstitial_shown")
                // C4
                .closeButtonShouldBeDisplayedWithin(CLOSE_BUTTON_TIMEOUT_S)
                .closeInterstitial()
                .interstitialShouldBeClosed()
                .sdkEventShouldBePresent(AdBasePage.SdkEvent.onAdClosed)
                // C3
                .sdkEventShouldBePresent(AdBasePage.SdkEvent.onAdDisplayed)
                .sdkEventShouldNotBePresent(AdBasePage.SdkEvent.onAdFailed, 0);
    }

    /** C5 (SHOULD) */
    @Test
    public void testCreativeLayout() {
        CustomPbsInterstitialPage page = openExample()
                .sdkEventShouldBePresent(AdBasePage.SdkEvent.onAdLoaded, LOAD_TIMEOUT_MS)
                .showPrebidInterstitial()
                .creativeShouldNotBeBlank()
                .saveScreenshot("interstitial_layout");

        int offset = page.creativeHorizontalOffsetPx();
        page.closeInterstitial();

        assumeTrue("C5 creative is " + offset + "px off centre", offset <= CENTER_TOLERANCE_PX);
    }

    /** C6 (SHOULD) */
    @Test
    public void testClickOpensBrowser() {
        CustomPbsInterstitialPage page = openExample()
                .sdkEventShouldBePresent(AdBasePage.SdkEvent.onAdLoaded, LOAD_TIMEOUT_MS)
                .showPrebidInterstitial()
                .creativeShouldNotBeBlank()
                .clickCreative();
        try {
            page.browserShouldOpen().closeBrowser();
        } catch (AssertionError | NullPointerException e) {
            assumeTrue("C6 browser did not open: " + e.getMessage(), false);
        }
        page.closeButtonShouldBeDisplayedWithin(CLOSE_BUTTON_TIMEOUT_S)
                .closeInterstitial()
                .sdkEventShouldBePresent(AdBasePage.SdkEvent.onAdClicked);
    }

    /** D1 */
    @Test
    public void testUnknownConfigIdFails() throws JSONException {
        CustomPbsSettings.INSTANCE.setConfigId("teads-unknown-config-id");
        JSONObject bidInfo = openExample()
                .sdkEventShouldBePresent(AdBasePage.SdkEvent.onAdFailed, LOAD_TIMEOUT_MS)
                .sdkEventShouldNotBePresent(AdBasePage.SdkEvent.onAdLoaded, 0)
                .getBidInfo();
        assertFalse("price must not be set", bidInfo.has("price"));
        // Fail for the right reason: the server rejects the unknown stored imp with HTTP 400.
        String error = bidInfo.optString("error");
        assertTrue("unexpected error: " + error, error.contains("400"));
    }

    /** Opens the Custom PBS screen directly, without going through the example list. */
    private CustomPbsInterstitialPage openExample() {
        Bundle fragmentArgs = new Bundle();
        fragmentArgs.putString(
                getStringResource(R.string.key_title),
                getStringResource(R.string.demo_bidding_in_app_interstitial_custom_pbs)
        );
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Activity activity = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .iterator()
                    .next();
            Navigation.findNavController(activity, R.id.nav_host_fragment)
                    .navigate(R.id.navigation_custom_pbs_interstitial, fragmentArgs);
        });
        return new CustomPbsInterstitialPage(device);
    }

    private static void writeArtifact(String fileName, String content) {
        try (FileWriter writer = new FileWriter(CustomPbsInterstitialPage.artifactFile(fileName))) {
            writer.write(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean waitForSdkJsLibraries() throws InterruptedException {
        JSLibraryManager manager = JSLibraryManager.getInstance(InstrumentationRegistry.getInstrumentation().getTargetContext());
        for (int i = 0; i < JS_LIBRARIES_TIMEOUT_S; i++) {
            // Starts the download, or reading into memory, when not done yet.
            if (manager.checkIfScriptsDownloadedAndStartDownloadingIfNot()) {
                return true;
            }
            Thread.sleep(1000);
        }
        return false;
    }

    private static boolean isServerReachable(String auctionUrl) {
        try {
            URL statusUrl = new URL(auctionUrl.replace("/openrtb2/auction", "/status"));
            HttpURLConnection connection = (HttpURLConnection) statusUrl.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            int code = connection.getResponseCode();
            connection.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
