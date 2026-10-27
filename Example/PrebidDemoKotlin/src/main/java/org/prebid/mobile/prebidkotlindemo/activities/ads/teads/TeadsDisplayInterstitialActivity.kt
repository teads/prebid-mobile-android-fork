package org.prebid.mobile.prebidkotlindemo.activities.ads.teads

import android.os.Bundle
import org.prebid.mobile.TargetingParams
import org.prebid.mobile.api.data.AdUnitFormat
import org.prebid.mobile.api.exceptions.AdException
import org.prebid.mobile.api.rendering.InterstitialAdUnit
import org.prebid.mobile.api.rendering.listeners.InterstitialAdUnitListener
import org.prebid.mobile.prebidkotlindemo.activities.BaseAdActivity
import java.util.*

/**
 * Display interstitial for the Teads test Prebid Server.
 * Uses the server picked in the toolbar: on the Prebid server the config id is unknown, so it fails.
 */
class TeadsDisplayInterstitialActivity : BaseAdActivity() {

    companion object {
        const val CONFIG_ID = "outbrain-imp-display"
    }

    private var adUnit: InterstitialAdUnit? = null
    private var previousSubjectToGdpr: Boolean? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The app sets GDPR on without a consent string, so the server drops the bidder.
        previousSubjectToGdpr = TargetingParams.isSubjectToGDPR()
        TargetingParams.setSubjectToGDPR(false)
        createAd()
    }

    private fun createAd() {
        adUnit = InterstitialAdUnit(this, CONFIG_ID, EnumSet.of(AdUnitFormat.BANNER))
        adUnit?.setInterstitialAdUnitListener(object : InterstitialAdUnitListener {
            override fun onAdLoaded(interstitialAdUnit: InterstitialAdUnit?) {
                adUnit?.show()
            }

            override fun onAdDisplayed(interstitialAdUnit: InterstitialAdUnit?) {}
            override fun onAdFailed(interstitialAdUnit: InterstitialAdUnit?, e: AdException?) {}
            override fun onAdClicked(interstitialAdUnit: InterstitialAdUnit?) {}
            override fun onAdClosed(interstitialAdUnit: InterstitialAdUnit?) {}
        })
        adUnit?.loadAd()
    }


    override fun onDestroy() {
        super.onDestroy()
        adUnit?.destroy()
        TargetingParams.setSubjectToGDPR(previousSubjectToGdpr)
    }

}
