package org.prebid.mobile.renderingtestapp.uiAutomator.pages.bidding.ppm;

import android.graphics.Rect;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.json.JSONException;
import org.json.JSONObject;
import org.prebid.mobile.renderingtestapp.uiAutomator.pages.AdBasePage;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Creative-agnostic checks for the "Custom PBS" interstitial. Does not rely on creative text.
 */
public class CustomPbsInterstitialPage extends AdBasePage<CustomPbsInterstitialPage> {

    private static final Pattern NON_BLANK = Pattern.compile("(?s).*\\S.*");

    private static class Locators {
        static BySelector bidInfo = By.res(TAG, "tvBidInfo");
        static BySelector webView = By.clazz("android.webkit.WebView");
        static BySelector closeButton = By.res(TAG, "iv_close_interstitial");
    }

    public CustomPbsInterstitialPage(UiDevice device) {
        super(device);
    }

    public JSONObject getBidInfo() throws JSONException {
        scrollIntoView(Locators.bidInfo);
        UiObject2 label = device.wait(Until.findObject(Locators.bidInfo), TIMEOUT);
        assertNotNull("Bid info label not found", label);
        // Filled on onAdLoaded / onAdFailed.
        device.wait(Until.findObject(By.copy(Locators.bidInfo).textStartsWith("{")), TIMEOUT);
        return new JSONObject(device.findObject(Locators.bidInfo).getText());
    }

    /** C2: a WebView is shown and its accessibility tree (DOM) is not empty. */
    public CustomPbsInterstitialPage creativeShouldNotBeBlank() {
        assertNotNull("Interstitial WebView is not displayed", device.wait(Until.findObject(Locators.webView), TIMEOUT));
        BySelector webViewWithText = By.copy(Locators.webView).hasDescendant(By.text(NON_BLANK));
        assertTrue("Interstitial WebView is blank", device.wait(Until.hasObject(webViewWithText), TIMEOUT));
        return this;
    }

    /** C4: close button shows up within the given time. */
    public CustomPbsInterstitialPage closeButtonShouldBeDisplayedWithin(int seconds) {
        assertNotNull(
                String.format("Close button is not displayed within %d s", seconds),
                device.wait(Until.findObject(Locators.closeButton), seconds * 1000L)
        );
        return this;
    }

    public CustomPbsInterstitialPage interstitialShouldBeClosed() {
        assertTrue("Interstitial is still displayed", device.wait(Until.gone(Locators.closeButton), TIMEOUT));
        assertFalse("WebView is still displayed", device.hasObject(Locators.webView));
        return this;
    }

    /**
     * C5: horizontal offset (px) between the creative's centre and the screen centre.
     * The creative is the largest DOM node smaller than the WebView itself.
     */
    public int creativeHorizontalOffsetPx() {
        Rect webView = device.findObject(Locators.webView).getVisibleBounds();
        Rect creative = null;
        for (UiObject2 node : findCreativeNodes()) {
            Rect bounds = node.getVisibleBounds();
            boolean isWholePage = bounds.width() >= webView.width() && bounds.height() >= webView.height();
            if (!isWholePage && (creative == null || area(bounds) > area(creative))) {
                creative = bounds;
            }
        }
        assertNotNull("No creative node found in the WebView", creative);
        return Math.abs(creative.centerX() - device.getDisplayWidth() / 2);
    }

    /** Saves a screenshot to the app's external files dir, under teads-artifacts/. */
    public CustomPbsInterstitialPage saveScreenshot(String name) {
        device.takeScreenshot(artifactFile(name + ".png"));
        return this;
    }

    public static File artifactFile(String fileName) {
        File dir = new File(
                InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),
                "teads-artifacts"
        );
        dir.mkdirs();
        return new File(dir, fileName);
    }

    /** C6: tap the centre of the creative. */
    public CustomPbsInterstitialPage clickCreative() {
        device.findObject(Locators.webView).click();
        return this;
    }

    private List<UiObject2> findCreativeNodes() {
        return device.findObject(Locators.webView).findObjects(By.pkg(TAG));
    }

    private static int area(Rect rect) {
        return rect.width() * rect.height();
    }
}
