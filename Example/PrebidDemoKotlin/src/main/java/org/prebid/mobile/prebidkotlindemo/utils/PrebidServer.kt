package org.prebid.mobile.prebidkotlindemo.utils

import androidx.annotation.StringRes
import org.prebid.mobile.Host
import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.prebidkotlindemo.BuildConfig
import org.prebid.mobile.prebidkotlindemo.R

enum class PrebidServer(
    @StringRes val titleRes: Int,
    val hostUrl: String,
    val accountId: String,
) {

    PREBID(
        R.string.prebid_server_prebid,
        "https://prebid-server-test-j.prebid.org/openrtb2/auction",
        "0689a263-318d-448b-a3d4-b02e8a709d9d",
    ),

    /** The host is not in the repo: set teadsPbsHost in local.properties. */
    TEADS(
        R.string.prebid_server_teads,
        BuildConfig.TEADS_PBS_HOST,
        "outbrain-req",
    );

    val isAvailable: Boolean
        get() = hostUrl.isNotBlank()

    val statusUrl: String
        get() = hostUrl.replace("/openrtb2/auction", "/status")

    /** Host and account are read on every request, so this works after SDK init too. */
    fun apply() {
        Host.CUSTOM.hostUrl = hostUrl
        PrebidMobile.setPrebidServerAccountId(accountId)
    }

}
