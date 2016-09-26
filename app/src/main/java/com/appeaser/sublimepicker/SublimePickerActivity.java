/*
 * Copyright 2015 Vikram Kakkar
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

package com.appeaser.sublimepicker;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.appeaser.sublimepickerlibrary.SublimePicker;
import com.appeaser.sublimepickerlibrary.datepicker.SelectedDate;
import com.appeaser.sublimepickerlibrary.helpers.SublimeListenerAdapter;
import com.appeaser.sublimepickerlibrary.helpers.SublimeOptions;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class SublimePickerActivity extends AppCompatActivity {
    SublimePicker mSublimePicker;
    int currentFirstIndex;

    SublimeListenerAdapter mListener = new SublimeListenerAdapter() {
        @Override
        public void onDateRangeSelected(boolean isFirstPick, SelectedDate selectedDate) {
            if (!isFirstPick) {
                long millisDiff = selectedDate.getSecondDate().getTimeInMillis() - selectedDate.getFirstDate().getTimeInMillis();
                int dayDiff = (int) TimeUnit.DAYS.convert(millisDiff, TimeUnit.MILLISECONDS);
                if(dayDiff <= 10){

                }
                else{
                    mSublimePicker.setCurrentItem(currentFirstIndex);
                }
            } else {
                currentFirstIndex = mSublimePicker.getCurrentItem();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        mSublimePicker = (SublimePicker) findViewById(R.id.sublime_picker);
        mSublimePicker.initializePicker(null, mListener);

        mSublimePicker.setCheckinDate(Calendar.getInstance());
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        mSublimePicker.setCheckoutDate(calendar);
    }
}
