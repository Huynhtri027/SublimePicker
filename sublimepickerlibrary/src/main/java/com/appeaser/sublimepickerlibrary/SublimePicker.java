/*
 * Copyright 2016 Vikram Kakkar
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

package com.appeaser.sublimepickerlibrary;

import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.appeaser.sublimepickerlibrary.datepicker.SelectedDate;
import com.appeaser.sublimepickerlibrary.datepicker.SublimeDatePicker;
import com.appeaser.sublimepickerlibrary.helpers.SublimeListenerAdapter;
import com.appeaser.sublimepickerlibrary.helpers.SublimeOptions;
import com.appeaser.sublimepickerlibrary.timepicker.SublimeTimePicker;
import com.appeaser.sublimepickerlibrary.utilities.SUtils;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A customizable view that provisions picking of a date,
 * time and recurrence option, all from a single user-interface.
 * You can also view 'SublimePicker' as a collection of
 * material-styled (API 23) DatePicker, TimePicker
 * and RecurrencePicker, backported to API 14.
 * You can opt for any combination of these three Pickers.
 */
public class SublimePicker extends FrameLayout
        implements SublimeDatePicker.OnDateChangedListener,
        SublimeDatePicker.DatePickerValidationCallback,
        SublimeTimePicker.TimePickerValidationCallback {
    private static final String TAG = SublimePicker.class.getSimpleName();

    // Used for formatting date range
    private static final long MONTH_IN_MILLIS = DateUtils.YEAR_IN_MILLIS / 12;

    // Container for 'SublimeDatePicker' & 'SublimeTimePicker'
    private LinearLayout llMainContentHolder;

    // Keeps track which picker is showing
    private SublimeOptions.Picker mCurrentPicker, mHiddenPicker;

    // Date picker
    private SublimeDatePicker mDatePicker;

    // Time picker
    private SublimeTimePicker mTimePicker;

    // Callback
    private SublimeListenerAdapter mListener;

    // Client-set options
    private SublimeOptions mOptions;

    // Flags set based on client-set options {SublimeOptions}
    private boolean mDatePickerValid = true, mTimePickerValid = true,
            mDatePickerEnabled, mTimePickerEnabled,
            mDatePickerSyncStateCalled;

    // Used if listener returns
    // null/invalid(zero-length, empty) string
    private DateFormat mDefaultDateFormatter, mDefaultTimeFormatter;

    public SublimePicker(Context context) {
        this(context, null);
    }

    public SublimePicker(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.sublimePickerStyle);
    }

    public SublimePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(createThemeWrapper(context), attrs, defStyleAttr);
        initializeLayout();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SublimePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(createThemeWrapper(context), attrs, defStyleAttr, defStyleRes);
        initializeLayout();
    }

    private static ContextThemeWrapper createThemeWrapper(Context context) {
        final TypedArray forParent = context.obtainStyledAttributes(
                new int[]{R.attr.sublimePickerStyle});
        int parentStyle = forParent.getResourceId(0, R.style.SublimePickerStyleLight);
        forParent.recycle();

        return new ContextThemeWrapper(context, parentStyle);
    }

    private void initializeLayout() {
        Context context = getContext();
        SUtils.initializeResources(context);

        LayoutInflater.from(context).inflate(R.layout.sublime_picker_view_layout,
                this, true);

        mDefaultDateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM,
                Locale.getDefault());
        mDefaultTimeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT,
                Locale.getDefault());
        mDefaultTimeFormatter.setTimeZone(TimeZone.getTimeZone("GMT+0"));

        llMainContentHolder = (LinearLayout) findViewById(R.id.llMainContentHolder);

        mDatePicker = (SublimeDatePicker) findViewById(R.id.datePicker);
        mTimePicker = (SublimeTimePicker) findViewById(R.id.timePicker);
    }

    public void initializePicker(SublimeOptions options, SublimeListenerAdapter listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null.");
        }

        if (options != null) {
            options.verifyValidity();
        } else {
            options = new SublimeOptions();
        }

        mOptions = options;
        mListener = listener;

        processOptions();
        updateDisplay();
    }

    public void setCheckinDate(Calendar day){
        mDatePicker.setCheckinDay(day);
    }

    public void setCheckoutDate(Calendar day){
        mDatePicker.setCheckoutDay(day);
    }

    // Called before 'RecurrencePicker' is shown
    private void updateHiddenPicker() {
        if (mDatePickerEnabled && mTimePickerEnabled) {
            mHiddenPicker = mDatePicker.getVisibility() == View.VISIBLE ?
                    SublimeOptions.Picker.DATE_PICKER : SublimeOptions.Picker.TIME_PICKER;
        } else if (mDatePickerEnabled) {
            mHiddenPicker = SublimeOptions.Picker.DATE_PICKER;
        } else if (mTimePickerEnabled) {
            mHiddenPicker = SublimeOptions.Picker.TIME_PICKER;
        } else {
            mHiddenPicker = SublimeOptions.Picker.INVALID;
        }
    }

    private void updateDisplay() {
        CharSequence switchButtonText;

        if (mCurrentPicker == SublimeOptions.Picker.DATE_PICKER) {

            if (mTimePickerEnabled) {
                mTimePicker.setVisibility(View.GONE);
            }

            mDatePicker.setVisibility(View.VISIBLE);
            llMainContentHolder.setVisibility(View.VISIBLE);

            if (!mDatePickerSyncStateCalled) {
                mDatePickerSyncStateCalled = true;
            }
        } else if (mCurrentPicker == SublimeOptions.Picker.TIME_PICKER) {
            if (mDatePickerEnabled) {
                mDatePicker.setVisibility(View.GONE);
            }

            mTimePicker.setVisibility(View.VISIBLE);
            llMainContentHolder.setVisibility(View.VISIBLE);

        } else if (mCurrentPicker == SublimeOptions.Picker.REPEAT_OPTION_PICKER) {
            updateHiddenPicker();

            if (mDatePickerEnabled || mTimePickerEnabled) {
                llMainContentHolder.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        BaseSavedState bss = (BaseSavedState) state;
        super.onRestoreInstanceState(bss.getSuperState());
        SavedState ss = (SavedState) bss;

        mCurrentPicker = ss.getCurrentPicker();
        mHiddenPicker = ss.getHiddenPicker();
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        super.dispatchRestoreInstanceState(container);
        updateDisplay();
    }

    /**
     * Class for managing state storing/restoring.
     */
    private static class SavedState extends View.BaseSavedState {

        private final SublimeOptions.Picker sCurrentPicker, sHiddenPicker /*One of DatePicker/TimePicker*/;
        private final String sRecurrenceRule;

        /**
         * Constructor called from {@link SublimePicker#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, SublimeOptions.Picker currentPicker,
                           SublimeOptions.Picker hiddenPicker,
                           String recurrenceRule) {
            super(superState);

            sCurrentPicker = currentPicker;
            sHiddenPicker = hiddenPicker;
            sRecurrenceRule = recurrenceRule;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);

            sCurrentPicker = SublimeOptions.Picker.valueOf(in.readString());
            sHiddenPicker = SublimeOptions.Picker.valueOf(in.readString());
            sRecurrenceRule = in.readString();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            dest.writeString(sCurrentPicker.name());
            dest.writeString(sHiddenPicker.name());
            dest.writeString(sRecurrenceRule);
        }

        public SublimeOptions.Picker getCurrentPicker() {
            return sCurrentPicker;
        }

        public SublimeOptions.Picker getHiddenPicker() {
            return sHiddenPicker;
        }

        public String getRecurrenceRule() {
            return sRecurrenceRule;
        }

        @SuppressWarnings("all")
        // suppress unused and hiding
        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private void processOptions() {
        if (mOptions.animateLayoutChanges()) {
            // Basic Layout Change Animation(s)
            LayoutTransition layoutTransition = new LayoutTransition();
            if (SUtils.isApi_16_OrHigher()) {
                layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
            }
            setLayoutTransition(layoutTransition);
        } else {
            setLayoutTransition(null);
        }

        mDatePickerEnabled = true;
        mTimePickerEnabled = mOptions.isTimePickerActive();
        if (mDatePickerEnabled) {
            //int[] dateParams = mOptions.getDateParams();
            //mDatePicker.init(dateParams[0] /* year */,
            //        dateParams[1] /* month of year */,
            //        dateParams[2] /* day of month */,
            //        mOptions.canPickDateRange(),
            //        this);
            mDatePicker.init(mOptions.getDateParams(), mOptions.canPickDateRange(), this);

            long[] dateRange = mOptions.getDateRange();

            if (dateRange[0] /* min date */ != Long.MIN_VALUE) {
                mDatePicker.setMinDate(dateRange[0]);
            }

            if (dateRange[1] /* max date */ != Long.MIN_VALUE) {
                mDatePicker.setMaxDate(dateRange[1]);
            }

            mDatePicker.setValidationCallback(this);

        } else {
            llMainContentHolder.removeView(mDatePicker);
            mDatePicker = null;
        }

        if (mTimePickerEnabled) {
            int[] timeParams = mOptions.getTimeParams();
            mTimePicker.setCurrentHour(timeParams[0] /* hour of day */);
            mTimePicker.setCurrentMinute(timeParams[1] /* minute */);
            mTimePicker.setIs24HourView(mOptions.is24HourView());
            mTimePicker.setValidationCallback(this);

        } else {
            llMainContentHolder.removeView(mTimePicker);
            mTimePicker = null;
        }

        if (!mDatePickerEnabled && !mTimePickerEnabled) {
            removeView(llMainContentHolder);
            llMainContentHolder = null;
        }

        mCurrentPicker = mOptions.getPickerToShow();
        // Updated from 'updateDisplay()' when 'RecurrencePicker' is chosen
        mHiddenPicker = SublimeOptions.Picker.INVALID;
    }

    private void reassessValidity() {
    }

    @Override
    public void onDateChanged(SublimeDatePicker view, SelectedDate selectedDate) {
        // TODO: Consider removing this propagation of date change event altogether
        //mDatePicker.init(selectedDate.getStartDate().get(Calendar.YEAR),
                //selectedDate.getStartDate().get(Calendar.MONTH),
                //selectedDate.getStartDate().get(Calendar.DAY_OF_MONTH),
                //mOptions.canPickDateRange(), this);
        mDatePicker.init(selectedDate, mOptions.canPickDateRange(), this);
    }

    @Override
    public void onRangeChangeListener(boolean isFirstPick, SelectedDate selectedDate) {
        mListener.onDateRangeSelected(isFirstPick, selectedDate);
    }

    @Override
    public void onDatePickerValidationChanged(boolean valid) {
        mDatePickerValid = valid;
        reassessValidity();
    }

    @Override
    public void onTimePickerValidationChanged(boolean valid) {
        mTimePickerValid = valid;
        reassessValidity();
    }
}
