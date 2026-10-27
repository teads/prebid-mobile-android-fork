/*
 *    Copyright 2018-2019 Prebid.org, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.prebid.mobile.prebidkotlindemo.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.MobileAds
import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.prebidkotlindemo.R
import org.prebid.mobile.prebidkotlindemo.databinding.ActivityMainBinding
import org.prebid.mobile.prebidkotlindemo.testcases.*
import org.prebid.mobile.prebidkotlindemo.utils.PrebidServer
import org.prebid.mobile.prebidkotlindemo.utils.Settings

class MainActivity : AppCompatActivity() {

    private var integrationKind: IntegrationKind? = null
    private var adFormat: AdFormat? = null
    private var searchRequest = ""

    private lateinit var binding: ActivityMainBinding
    private lateinit var testCaseAdapter: TestCaseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        initSpinners()
        initSearch()
        initList()
        updateServerSubtitle()

        PrebidMobile.checkGoogleMobileAdsCompatibility(MobileAds.getVersion().toString())
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        if (item.itemId == R.id.settings) {
            startActivity(SettingsActivity.getIntent(this))
            return true
        }
        if (item.itemId == R.id.prebid_server) {
            showServerPicker()
            return true
        }
        return false
    }

    private fun showServerPicker() {
        val servers = PrebidServer.values()
        AlertDialog.Builder(this)
            .setTitle(R.string.prebid_server)
            .setSingleChoiceItems(
                servers.map { getString(it.titleRes) }.toTypedArray(),
                servers.indexOf(Settings.get().prebidServer)
            ) { dialog, which ->
                val server = servers[which]
                if (server.isAvailable) {
                    Settings.get().prebidServer = server
                    server.apply()
                    updateServerSubtitle()
                } else {
                    Toast.makeText(this, R.string.prebid_server_teads_missing_host, Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun updateServerSubtitle() {
        supportActionBar?.subtitle = getString(Settings.get().prebidServer.titleRes)
    }


    private fun initSpinners() {
        binding.spinnerIntegrationKind.apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                IntegrationKind.values().map { it.adServer }.toMutableList().apply {
                    add(0, "All")
                }
            )
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, l: Long) {
                    integrationKind = if (position == 0) null else IntegrationKind.values()[position - 1]
                    Settings.get().lastIntegrationKindId = position
                    updateList()
                }

                override fun onNothingSelected(adapterView: AdapterView<*>) {}
            }
            setSelection(Settings.get().lastIntegrationKindId)
        }
        binding.spinnerAdType.apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                AdFormat.values().map { it.description }.toMutableList().apply {
                    add(0, "All")
                }
            )
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, l: Long) {
                    adFormat = if (position == 0) null else AdFormat.values()[position - 1]
                    Settings.get().lastAdFormatId = position
                    updateList()
                }

                override fun onNothingSelected(adapterView: AdapterView<*>) {}
            }
            setSelection(Settings.get().lastAdFormatId)
        }
    }

    private fun initSearch() {
        binding.etSearch.addTextChangedListener {
            searchRequest = it.toString()
            updateList()
        }
    }

    private fun initList() {
        binding.rvAdTypes.apply {
            testCaseAdapter = TestCaseAdapter { showAd(it) }
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = testCaseAdapter
        }
    }

    private fun updateList() {
        val list = TestCaseRepository.getList().filter {
            if (integrationKind != null && it.integrationKind != integrationKind) {
                return@filter false
            }

            if (adFormat != null && it.adFormat != adFormat) {
                return@filter false
            }

            if (searchRequest.isNotBlank() && !getString(it.titleStringRes).contains(
                    searchRequest,
                    ignoreCase = true
                )
            ) {
                return@filter false
            }

            return@filter true
        }
        testCaseAdapter.setList(list)
    }

    private fun showAd(testCase: TestCase) {
        TestCaseRepository.lastTestCase = testCase
        startActivity(Intent(this, testCase.activity))
    }

}

