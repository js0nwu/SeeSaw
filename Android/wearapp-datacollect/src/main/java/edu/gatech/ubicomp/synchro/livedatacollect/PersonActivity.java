package edu.gatech.ubicomp.synchro.livedatacollect;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class PersonActivity extends Activity {

    private TextView personText;

    private void permissionsCheckWarning() {
        int writePermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int vibratePermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE);
        if (writePermissionCheck != PackageManager.PERMISSION_GRANTED || vibratePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "write permission: " + (writePermissionCheck == PackageManager.PERMISSION_GRANTED ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
            Toast.makeText(getApplicationContext(), "vibrate permission: " + (vibratePermissionCheck == PackageManager.PERMISSION_GRANTED ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
            AlertDialog alertDialog = new AlertDialog.Builder(PersonActivity.this).create();
            alertDialog.setTitle("About to goof");
            alertDialog.setMessage("A required permission is missing");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);
        personText = (TextView) findViewById(R.id.personText);
        permissionsCheckWarning();
        final Context context = this.getApplicationContext();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "no permissions", Toast.LENGTH_SHORT).show();
            Log.i("location", "no permission");
            ActivityCompat.requestPermissions(
                    this,
                    Config.APP_PERMISSIONS, 1);

        }


    }

    public void personLess(View v) {
        int personValue = Integer.parseInt(personText.getText().toString());
        personValue--;
        personValue = Math.max(0, personValue);
        personText.setText("" + personValue);
    }

    public void personMore(View v) {
        int personValue = Integer.parseInt(personText.getText().toString());
        personValue++;
        personValue = Math.max(0, personValue);
        personText.setText("" + personValue);
    }

    public void startExperiment(View v) {
        Intent sessionIntent = new Intent(PersonActivity.this, ExperimentActivity.class);
        sessionIntent.putExtra("person", personText.getText().toString());
        PersonActivity.this.startActivity(sessionIntent);
    }

    public void startManual(View v) {
        Intent sessionIntent = new Intent(PersonActivity.this, StartActivity.class);
        PersonActivity.this.startActivity(sessionIntent);
    }

    public void startContinuous(View v) {
        Intent sessionIntent = new Intent(PersonActivity.this, Experiment2Activity.class);
        sessionIntent.putExtra("person", personText.getText().toString());
        PersonActivity.this.startActivity(sessionIntent);
    }
}
