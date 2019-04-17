package com.example.ttwil.mapsprototype1;

import android.content.Intent;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;

import org.w3c.dom.Text;

import android.text.format.DateFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class LoginActivity extends AppCompatActivity {

    private TextView dateText;
    private TextView timeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button submitButton = findViewById(R.id.submitButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validate();
            }
        });

        initializeDateTime();
        initializeProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void validate() {
        if (!writeLoginFile()) { return; };

        TextView flowRateTextView = findViewById(R.id.tableFlowRate);
        String flowRateText = (String) flowRateTextView.getText();
        int index = flowRateText.indexOf(' ');
        float flowRate = Float.parseFloat(flowRateText.substring(0,index));

        Intent intent = new Intent(LoginActivity.this, MapActivity.class);
        intent.putExtra("flowRate", flowRate);
        startActivity(intent);
    }

    public void showDatePickerDialog(View v) {
        DialogFragment dialogFragment = DatePickerFragment.newInstance(R.id.dateText);
        dialogFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public void showTimePickerDialog(View v) {
        DialogFragment dialogFragment = TimePickerFragment.newInstance(R.id.timeText);
        dialogFragment.show(getSupportFragmentManager(), "timePicker");
    }

    private void initializeDateTime() {
        dateText = findViewById(R.id.dateText);
        timeText = findViewById(R.id.timeText);

        Calendar c = Calendar.getInstance();
        String date = (c.get(Calendar.MONTH) + 1) + "/" +
                c.get(Calendar.DAY_OF_MONTH) + "/" + c.get(Calendar.YEAR);

        String time = (String) DateFormat.format("h:mm a", c.getTime());

        dateText.setText(date);
        timeText.setText(time);
    }

    private void initializeProducts() {
        final Spinner productSpinner = findViewById(R.id.pesticideOptions);
        productSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                populateTable(productSpinner.getSelectedItemPosition());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        populateTable(0);
    }

    private void populateTable(int i) {
        TextView productName = findViewById(R.id.tableProductName);
        TextView EPA = findViewById(R.id.tableEPA);
        TextView concentration = findViewById(R.id.tableConcentration);
        TextView dilution = findViewById(R.id.tableDilution);
        TextView flowRate = findViewById(R.id.tableFlowRate);
        TextView targetRate = findViewById(R.id.tableTargetRate);

        productName.setText(getResources().getStringArray(R.array.productNames)[i]);
        EPA.setText(getResources().getStringArray(R.array.productEPA)[i]);
        concentration.setText(getResources().getStringArray(R.array.productConcentration)[i]);
        dilution.setText(getResources().getStringArray(R.array.productDilution)[i]);
        flowRate.setText(getResources().getStringArray(R.array.productFlowRate)[i]);
        targetRate.setText(getResources().getStringArray(R.array.productTargetRate)[i]);
    }

    private boolean writeLoginFile() {
        StringBuilder sb =  new StringBuilder();

        sb.append("Benton County Mosquito Control: Adulticiding\n\n");
        sb.append("Pre-Spray Data\n\n");

        // Date
        sb.append("Date: ");
        sb.append(dateText.getText());
        sb.append("\n");

        // Applicator
        sb.append("Applicator: ");
        Spinner applicators = findViewById(R.id.applicatorOptions);
        sb.append(applicators.getSelectedItem().toString());
        sb.append("\n");

        // Truck #
        sb.append("Truck #: ");
        Spinner trucks = findViewById(R.id.truckOptions);
        sb.append(trucks.getSelectedItem().toString());
        sb.append("\n");

        // Time leaving compound
        sb.append("Time Leaving Compound: ");
        sb.append(timeText.getText());
        sb.append("\n");

        // Product Name
        sb.append("Product Used: ");
        TextView productNames = findViewById(R.id.tableProductName);
        sb.append(productNames.getText());
        sb.append("\n");

        // Product EPA number
        sb.append("EPA Regulation #: ");
        TextView EPAs = findViewById(R.id.tableEPA);
        sb.append(EPAs.getText());
        sb.append("\n");

        // Product Concentration
        sb.append("Conc. A.I. (PBO): ");
        TextView concs = findViewById(R.id.tableConcentration);
        sb.append(concs.getText());
        sb.append("\n");

        // Product Dilution
        sb.append("Dilution Rate: ");
        TextView dilutions = findViewById(R.id.tableDilution);
        sb.append(dilutions.getText());
        sb.append("\n");

        // Product Flow Rate
        sb.append("Flow Rate: ");
        TextView flowRates = findViewById(R.id.tableFlowRate);
        sb.append(flowRates.getText());
        sb.append("\n");

        // Product Target Rate
        sb.append("SmartFlow Target Rate: ");
        TextView targetRates = findViewById(R.id.tableTargetRate);
        sb.append(targetRates.getText());
        sb.append("\n\nSessions: \n\n");

        File loginFolder = new File(Environment.getExternalStorageDirectory(), "Login Data");
        if (!loginFolder.exists()) {
            if (!loginFolder.mkdirs()) {return false;}
        }

        File loginFile = new File(loginFolder, "session" + "_login.txt");

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(loginFile);
            fileOutputStream.write(sb.toString().getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }
}