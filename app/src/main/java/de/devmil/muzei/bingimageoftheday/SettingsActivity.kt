/*
 * Copyright 2014 Devmil Solutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.devmil.muzei.bingimageoftheday

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView

import de.devmil.muzei.bingimageoftheday.events.RequestMarketSettingChangedEvent
import de.devmil.muzei.bingimageoftheday.events.RequestPortraitSettingChangedEvent
import de.greenrobot.event.EventBus

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                    .add(R.id.container, SettingsFragment())
                    .commit()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            setResult(Activity.RESULT_OK)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : Fragment() {

        private var rbLandscape: RadioButton? = null
        private var rbPortrait: RadioButton? = null
        private var spMarket: Spinner? = null
        private var marketAdapter: ArrayAdapter<BingMarket>? = null
        private var btnLicense: Button? = null

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val rootView = inflater.inflate(R.layout.fragment_settings, container, false)
            rbLandscape = rootView.findViewById(R.id.fragment_settings_orientation_landscape) as RadioButton
            rbPortrait = rootView.findViewById(R.id.fragment_settings_orientation_portrait) as RadioButton
            spMarket = rootView.findViewById(R.id.fragment_settings_market) as Spinner
            marketAdapter = MarketAdapter(activity, R.layout.settings_ab_spinner_list_item_dropdown, BingMarket.selectableValues())
            spMarket!!.adapter = marketAdapter
            spMarket!!.setSelection(GetMarketSpinnerSelection())
            btnLicense = rootView.findViewById(R.id.fragment_settings_button_license) as Button

            val settings = Settings(activity, BingImageOfTheDayArtSource.getSharedPreferences(activity))
            val portrait = settings.isOrientationPortrait

            rbLandscape!!.isChecked = !portrait
            rbPortrait!!.isChecked = portrait

            val listener = CompoundButton.OnCheckedChangeListener { compoundButton, checked ->
                if (!checked)
                    return@OnCheckedChangeListener
                val isPortrait = compoundButton === rbPortrait
                settings.isOrientationPortrait = isPortrait
                EventBus.getDefault().post(RequestPortraitSettingChangedEvent(activity))
            }

            rbLandscape!!.setOnCheckedChangeListener(listener)
            rbPortrait!!.setOnCheckedChangeListener(listener)
            spMarket!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                    val market = marketAdapter!!.getItem(i)
                    settings.bingMarket = market
                    EventBus.getDefault().post(RequestMarketSettingChangedEvent(activity))
                }

                override fun onNothingSelected(adapterView: AdapterView<*>) {

                }
            }

            val context = inflater.context

            btnLicense!!.setOnClickListener {
                val licenseActivityIntent = Intent(context, LicenseInfoActivity::class.java)
                startActivity(licenseActivityIntent)
            }

            return rootView
        }

        private fun GetMarketSpinnerSelection(): Int {
            val settings = Settings(activity, BingImageOfTheDayArtSource.getSharedPreferences(activity))

            val marketCode = settings.bingMarket.marketCode

            return (0..marketAdapter!!.count - 1)
                        .firstOrNull { marketAdapter!!.getItem(it)!!.marketCode == marketCode }
                    ?: 0
        }
    }

    internal class MarketAdapter(context: Context, resource: Int, objects: Array<BingMarket>) : ArrayAdapter<BingMarket>(context, resource, objects) {

        internal class ViewHolder {
            var textView: TextView? = null
            var imageView: ImageView? = null
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getView(position, convertView, parent)
        }

        @SuppressLint("InflateParams")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var effectiveConvertView = convertView

            val holder: ViewHolder
            if (effectiveConvertView == null) {
                effectiveConvertView = LayoutInflater.from(context).inflate(R.layout.settings_ab_spinner_list_item_dropdown, null)
                holder = ViewHolder()
                holder.imageView = effectiveConvertView!!.findViewById(R.id.settings_ab_spinner_list_item_dropdown_icon) as ImageView
                holder.textView = effectiveConvertView.findViewById(R.id.settings_ab_spinner_list_item_dropdown_text) as TextView
                effectiveConvertView.tag = holder
            } else {
                holder = effectiveConvertView.tag as ViewHolder
            }

            val market = getItem(position)

            holder.textView!!.text = market!!.toString()
            holder.imageView!!.setImageResource(market.logoResourceId)

            return effectiveConvertView
        }
    }
}
