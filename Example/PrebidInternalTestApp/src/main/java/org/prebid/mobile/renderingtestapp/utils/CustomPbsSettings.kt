package org.prebid.mobile.renderingtestapp.utils

import org.prebid.mobile.Host
import org.prebid.mobile.PrebidMobile

/**
 * Teads/Outbrain test Prebid Server. Set with the PBS_HOST (required),
 * PBS_ACCOUNT_ID and PBS_CONFIG_ID intent extras.
 */
object CustomPbsSettings {

    const val DEFAULT_HOST_URL = ""
    const val DEFAULT_ACCOUNT_ID = "outbrain-req"
    const val DEFAULT_CONFIG_ID = "outbrain-imp-display"

    var hostUrl = DEFAULT_HOST_URL
    var accountId = DEFAULT_ACCOUNT_ID
    var configId = DEFAULT_CONFIG_ID

    private var previousHostUrl: String? = null
    private var previousPbsDebug = false

    fun apply() {
        previousHostUrl = Host.CUSTOM.hostUrl
        previousPbsDebug = PrebidMobile.getPbsDebug()
        Host.CUSTOM.hostUrl = hostUrl
        PrebidMobile.setPrebidServerAccountId(accountId)
        // Makes the server echo the resolved request in ext.debug, so tests can check what was sent.
        PrebidMobile.setPbsDebug(true)
    }

    /** Account id is reset by AdFragment.onDestroyView. */
    fun restore() {
        previousHostUrl?.let { Host.CUSTOM.hostUrl = it }
        previousHostUrl = null
        PrebidMobile.setPbsDebug(previousPbsDebug)
    }

}
