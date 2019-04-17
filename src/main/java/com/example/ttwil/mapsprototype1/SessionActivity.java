package com.example.ttwil.mapsprototype1;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.se.omapi.Session;
import android.support.annotation.RequiresApi;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class SessionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        setScreenProperties();

        initializeFoggingAreaText();
        initializeFoggingAreaNotes();
        initializeSpinners();
        initializeTemp();
        initializeSubmitButton();
    }

    public void showTimePickerDialog(View view) {
        DialogFragment dialogFragment = TimePickerFragment.newInstance(R.id.timeStart3);
        dialogFragment.show(getSupportFragmentManager(), "timePicker");
    }

    private void setScreenProperties() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void initializeFoggingAreaText() {
        final EditText foggingAreaText = findViewById(R.id.foggingAreaInputText);
        foggingAreaText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Display String Length
                TextView charCount = findViewById(R.id.foggingAreaCharCount);
                charCount.setText(foggingAreaText.getText().length() + " / 250");

                // Shorten text if too long
                if (foggingAreaText.getText().length() > 250) {
                    String text = String.valueOf(foggingAreaText.getText());
                    while (text.length() > 250) {
                        text = text.substring(0, text.length() - 1);
                    }
                    foggingAreaText.setText(text);
                    foggingAreaText.setSelection(foggingAreaText.getText().length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        foggingAreaText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });
    }

    private void initializeFoggingAreaNotes() {
        final EditText foggingAreaText = findViewById(R.id.foggingAreaNotesText);
        foggingAreaText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Display String Length
                TextView charCount = findViewById(R.id.foggingNotesCharCount);
                charCount.setText(foggingAreaText.getText().length() + " / 250");

                // Shorten text if too long
                if (foggingAreaText.getText().length() > 250) {
                    String text = String.valueOf(foggingAreaText.getText());
                    while (text.length() > 250) {
                        text = text.substring(0, text.length() - 1);
                    }
                    foggingAreaText.setText(text);
                    foggingAreaText.setSelection(foggingAreaText.getText().length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        foggingAreaText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });
    }

    private void initializeSpinners() {
        // MAP AREA
        Spinner mapAreaSpinner = findViewById(R.id.mapAreaSpinner);
            // Update Fogging Areas

        // WIND DIRECTION
        Spinner windDirectionSpinner = findViewById(R.id.windDirection);
        windDirectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // Make it impossible to select the first item
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // WIND SPEED
        Spinner windSpeedSpinner = findViewById(R.id.windSpeed);
        windSpeedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1 || position >= 12) {
                    findViewById(R.id.windSpeedWarning).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.windSpeedWarning).setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initializeTemp() {

    }

    private void initializeSubmitButton() {
        Button btnStartSession = findViewById(R.id.btnStartSession);
        btnStartSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SessionActivity.this, "Starting New Session", Toast.LENGTH_SHORT).show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    addSessionToFile();
                }
                finish();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean addSessionToFile() {
        File dataFile = new File(Environment.getExternalStorageDirectory(), "Login Data/session_login.txt");
        if (!dataFile.exists()) { return false; }

        // Create text for the end of file
        StringBuilder sb = new StringBuilder();

        // Map Area
        sb.append("Map Area: ");
        Spinner mapAreas = findViewById(R.id.mapAreaSpinner);
        sb.append(mapAreas.getSelectedItem());
        sb.append("\n");

        // Fogging Area
        sb.append("Fogging Area Completed: ");
        TextView foggingArea = findViewById(R.id.foggingAreaInputText);
        sb.append(foggingArea.getText());
        sb.append("\n");

        // Fogging Area Notes
        sb.append("Fogging Area Notes: ");
        TextView foggingAreaNotes = findViewById(R.id.foggingAreaNotesText);
        sb.append(foggingAreaNotes.getText());
        sb.append("\n");

        // Wind Direction
        sb.append("Wind Direction: ");
        Spinner windDirection = findViewById(R.id.windDirection);
        sb.append(windDirection.getSelectedItem());
        sb.append("\n");

        // Wind Speed
        sb.append("Wind Speed: ");
        Spinner windSpeed = findViewById(R.id.windSpeed);
        sb.append(windSpeed.getSelectedItem());
        sb.append("\n");

        // Time Start
        sb.append("Time Start: ");
        TextView timeStart = findViewById(R.id.timeStart3);
        sb.append(timeStart.getText());
        sb.append("\n");

        // Append text to end of file
        try {
            Files.write(dataFile.toPath(), sb.toString().getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }
}