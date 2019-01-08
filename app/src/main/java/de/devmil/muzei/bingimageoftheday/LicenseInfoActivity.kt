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
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.util.Linkify
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import de.devmil.common.licensing.LicenseManager
import de.devmil.common.licensing.PackageInfo
import de.devmil.common.utils.LogUtil

class LicenseInfoActivity : Activity() {

    private var licenseManager: LicenseManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license_info)

        licenseManager = LicenseManager(this, R.raw.licenseinfo)

        val listView = findViewById(R.id.activity_license_info_listView) as ListView

        val adapter = LicenseEntryAdapter(this, licenseManager!!.licenseInfo!!.packages.toTypedArray())

        listView.adapter = adapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            setResult(Activity.RESULT_OK)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    internal inner class LicenseEntryAdapter(context: Context, items: Array<PackageInfo>) : ArrayAdapter<PackageInfo>(context, 0, items) {

        internal inner class ViewHolder {
            var image: ImageView? = null
            var name: TextView? = null
            var copyright: TextView? = null
            var url: TextView? = null
            var licenseButton: Button? = null
        }

        @SuppressLint("InflateParams")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var effectiveconvertView = convertView
            if (effectiveconvertView == null) {
                effectiveconvertView = LayoutInflater.from(context).inflate(R.layout.activity_license_info_entry, null)
                val vh = ViewHolder()
                vh.image = effectiveconvertView!!.findViewById(R.id.activity_license_info_entry_image) as ImageView
                vh.name = effectiveconvertView.findViewById(R.id.activity_license_info_entry_name) as TextView
                vh.copyright = effectiveconvertView.findViewById(R.id.activity_license_info_entry_copyright) as TextView
                vh.url = effectiveconvertView.findViewById(R.id.activity_license_info_entry_url) as TextView
                vh.licenseButton = effectiveconvertView.findViewById(R.id.activity_license_info_entry_licensebutton) as Button
                effectiveconvertView.tag = vh
            }

            val holder = effectiveconvertView.tag as ViewHolder

            val item = getItem(position)

            //context.getResources().getIdentifier() doesn't work any more!?
            val drawableId = R.drawable::class.java.getField(item!!.iconName).getInt(null)

            holder.image!!.setImageResource(drawableId)
            holder.name!!.text = item.name
            holder.copyright!!.text = item.copyright
            @Suppress("DEPRECATION")
            holder.url!!.text = Html.fromHtml("<a href=\"" + item.url + "\">" + item.url + "</a>")
            holder.url!!.autoLinkMask = Linkify.WEB_URLS
            holder.url!!.tag = item
            holder.url!!.setOnClickListener { v ->
                try {
                    val pi = v.tag as PackageInfo
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(pi.url)

                    startActivity(intent)
                } catch (e: Exception) {
                    LogUtil.LOGW(TAG, "Error parsing package url", e)
                }
            }
            holder.licenseButton!!.text = item.license.name
            holder.licenseButton!!.tag = item
            holder.licenseButton!!.setOnClickListener { v ->
                val pi = v.tag as PackageInfo
                val scrollView = ScrollView(context)
                val tvMessage = TextView(context)
                tvMessage.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 6f)
                tvMessage.typeface = Typeface.MONOSPACE
                tvMessage.text = pi.license.content
                scrollView.addView(tvMessage)
                val builder = AlertDialog.Builder(context)
                val dlg = builder.setTitle(pi.license.name)
                        .setView(scrollView)
                        .setNegativeButton("OK") { dialog, _ -> dialog.dismiss() }
                        .create()
                dlg.show()
            }

            return effectiveconvertView
        }
    }

    companion object {

        private val TAG = LicenseInfoActivity::class.java.simpleName
    }
}
