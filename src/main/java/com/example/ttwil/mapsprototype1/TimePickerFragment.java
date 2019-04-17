package com.example.ttwil.mapsprototype1;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import android.text.format.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    public static TimePickerFragment newInstance(int id) {
        TimePickerFragment fragment = new TimePickerFragment();

        Bundle args = new Bundle();
        args.putInt("id", id);
        fragment.setArguments(args);

        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {


        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR);
        int minute = c.get(Calendar.MINUTE);

        return new TimePickerDialog(getActivity(), TimePickerDialog.THEME_HOLO_LIGHT ,this, hour, minute, DateFormat.is24HourFormat(getActivity()));
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        if (getArguments().getInt("id", 0) == 0) {
            return;
        }

        Date date = new Date();
        date.setHours(hourOfDay);
        date.setMinutes(minute);

        TextView textView = getActivity().findViewById(getArguments().getInt("id", 0));

        if (textView == null) { return; }

        String text;
        if (hourOfDay > 12) {
            text = hourOfDay - 12 + ":" + minute + " PM";
        } else {
            text = hourOfDay + ":" + minute + " AM";
        }
        textView.setText(text);
    }
}

