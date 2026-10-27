package org.prebid.mobile.renderingtestapp.plugplay.bidding.ppm

import android.os.Bundle
import android.view.View
import org.json.JSONArray
import org.json.JSONObject
import org.prebid.mobile.Host
import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.api.data.AdUnitFormat
import org.prebid.mobile.api.exceptions.AdException
import org.prebid.mobile.api.rendering.InterstitialAdUnit
import org.prebid.mobile.renderingtestapp.utils.CustomPbsSettings

/**
 * In-App interstitial against the custom Prebid Server from [CustomPbsSettings].
 * Publishes a "Bid info" JSON summary so UI tests can check the bid response.
 */
class CustomPbsInterstitialFragment : PpmInterstitialFragment() {

    override fun initUi(view: View, savedInstanceState: Bundle?) {
        // Must run before super.initUi, which starts the ad.
        CustomPbsSettings.apply()
        binding.tvBidInfo.visibility = View.VISIBLE
        super.initUi(view, savedInstanceState)
    }

    override fun initInterstitialAd(
        adUnitFormat: AdUnitFormat, adUnitId: String?,
        configId: String?, width: Int, height: Int
    ) {
        super.initInterstitialAd(adUnitFormat, adUnitId, CustomPbsSettings.configId, width, height)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        CustomPbsSettings.restore()
    }

    override fun onAdLoaded(interstitialAdUnit: InterstitialAdUnit?) {
        super.onAdLoaded(interstitialAdUnit)
        binding.tvBidInfo.text = buildBidInfo(interstitialAdUnit).toString()
    }

    override fun onAdFailed(interstitial: InterstitialAdUnit?, exception: AdException?) {
        super.onAdFailed(interstitial, exception)
        binding.tvBidInfo.text = baseInfo().put("error", exception?.message).toString()
    }

    private fun baseInfo() = JSONObject()
        .put("host", Host.CUSTOM.hostUrl)
        .put("accountId", PrebidMobile.getPrebidServerAccountId())
        .put("configId", CustomPbsSettings.configId)

    private fun buildBidInfo(adUnit: InterstitialAdUnit?): JSONObject {
        val info = baseInfo()
        val bidResponse = adUnit?.bidResponse ?: return info.put("error", "no bid response")
        val bid = bidResponse.winningBid ?: return info.put("error", "no winning bid")
        val responseJson = bidResponse.responseJson

        info.put("seat", responseJson.optJSONArray("seatbid")?.optJSONObject(0)?.optString("seat"))
            .put("price", bid.price)
            .put("admLength", bid.adm?.length ?: 0)
            .put("w", bid.width)
            .put("h", bid.height)
            .put("type", bid.type)
            .put("targeting", JSONObject(bidResponse.targeting as Map<*, *>))
            .put("hasWinEvent", !bid.prebid?.winEventUrl.isNullOrEmpty())
            .put("winEventUrl", bid.prebid?.winEventUrl)
            .put("hasImpEvent", !bid.prebid?.impEventUrl.isNullOrEmpty())
            .put("impEventUrl", bid.prebid?.impEventUrl)

        // Present because CustomPbsSettings enables PBS debug.
        val request = responseJson.optJSONObject("ext")?.optJSONObject("debug")?.optJSONObject("resolvedrequest")
        val imp = request?.optJSONArray("imp")?.optJSONObject(0)
        info.put(
            "request", JSONObject()
                .put("publisherId", request?.optJSONObject("app")?.optJSONObject("publisher")?.optString("id"))
                .put("storedImpId", imp?.optJSONObject("ext")?.optJSONObject("prebid")?.optJSONObject("storedrequest")?.optString("id"))
                .put("instl", imp?.optInt("instl"))
                .put("bannerFormats", imp?.optJSONObject("banner")?.optJSONArray("format") ?: JSONArray())
        )
        return info
    }

}
