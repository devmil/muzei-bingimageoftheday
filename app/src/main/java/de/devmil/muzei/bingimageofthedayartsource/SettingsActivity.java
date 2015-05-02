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
package de.devmil.muzei.bingimageofthedayartsource;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import de.devmil.muzei.bingimageofthedayartsource.events.RequestMarketSettingChangedEvent;
import de.devmil.muzei.bingimageofthedayartsource.events.RequestPortraitSettingChangedEvent;
import de.greenrobot.event.EventBus;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new SettingsFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home)
        {
            setResult(Activity.RESULT_OK);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends Fragment {

        private RadioButton rbLandscape;
        private RadioButton rbPortrait;
        private Spinner spMarket;
        private ArrayAdapter<BingMarket> marketAdapter;
        private Button btnLicense;

        public SettingsFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
            rbLandscape = (RadioButton) rootView.findViewById(R.id.fragment_settings_orientation_landscape);
            rbPortrait = (RadioButton) rootView.findViewById(R.id.fragment_settings_orientation_portrait);
            spMarket = (Spinner) rootView.findViewById(R.id.fragment_settings_market);
            marketAdapter = new MarketAdapter(getActivity(),R.layout.settings_ab_spinner_list_item_dropdown, BingMarket.selectableValues());
            spMarket.setAdapter(marketAdapter);
            spMarket.setSelection(GetMarketSpinnerSelection());
            btnLicense = (Button)rootView.findViewById(R.id.fragment_settings_button_license);

            final SharedPreferences prefs = BingImageOfTheDayArtSource.getSharedPreferences(inflater.getContext());
            boolean portrait = prefs.getBoolean(BingImageOfTheDayArtSource.PREF_ORIENTATION_PORTRAIT, BingImageOfTheDayArtSource.isPortraitDefault(inflater.getContext()));

            rbLandscape.setChecked(!portrait);
            rbPortrait.setChecked(portrait);

            CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                    if (!checked)
                        return;
                    boolean portrait = compoundButton == rbPortrait;
                    SharedPreferences prefs = BingImageOfTheDayArtSource.getSharedPreferences(getActivity());
                    prefs.edit().putBoolean(BingImageOfTheDayArtSource.PREF_ORIENTATION_PORTRAIT, portrait).commit();
                    EventBus.getDefault().post(new RequestPortraitSettingChangedEvent(getActivity()));
                }
            };

            rbLandscape.setOnCheckedChangeListener(listener);
            rbPortrait.setOnCheckedChangeListener(listener);
            spMarket.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    BingMarket market = marketAdapter.getItem(i);
                    prefs.edit().putString(BingImageOfTheDayArtSource.PREF_MARKET_CODE, market.getMarketCode()).commit();
                    EventBus.getDefault().post(new RequestMarketSettingChangedEvent(getActivity()));
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            final Context context = inflater.getContext();

            btnLicense.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent licenseActivityIntent = new Intent(context, LicenseInfoActivity.class);
                    startActivity(licenseActivityIntent);
                }
            });

            return rootView;
        }

        private int GetMarketSpinnerSelection(){
            String marketCode = BingImageOfTheDayArtSource.getSharedPreferences(getActivity())
                    .getString(BingImageOfTheDayArtSource.PREF_MARKET_CODE, BingImageOfTheDayArtSource.DEFAULT_MARKET.getMarketCode());

            for (int i = 0; i<marketAdapter.getCount(); i++)
                if(marketAdapter.getItem(i).getMarketCode().equals(marketCode))
                    return i;
            return 0;
        }
    }

    static class MarketAdapter extends ArrayAdapter<BingMarket>
    {
        public MarketAdapter(Context context, int resource, BingMarket[] objects) {
            super(context, resource, objects);
        }

        static class ViewHolder
        {
            TextView textView;
            ImageView imageView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder;
            if(convertView == null)
            {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.settings_ab_spinner_list_item_dropdown, null);
                holder = new ViewHolder();
                holder.imageView = (ImageView)convertView.findViewById(R.id.settings_ab_spinner_list_item_dropdown_icon);
                holder.textView = (TextView)convertView.findViewById(R.id.settings_ab_spinner_list_item_dropdown_text);
                convertView.setTag(holder);
            }
            else
            {
                holder = (ViewHolder)convertView.getTag();
            }

            BingMarket market = getItem(position);

            holder.textView.setText(market.toString());
            holder.imageView.setImageResource(market.getLogoResourceId());

            return convertView;
        }
    }
}
