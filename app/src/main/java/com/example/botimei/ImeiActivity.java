package com.example.botimei;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ImeiActivity extends AppCompatActivity {
    String IMEINumber;
    TextView textView;
    private static final int REQUEST_CODE = 101;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imei);
        textView=findViewById(R.id.IMEI);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(ImeiActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ImeiActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE);
            return;
        }
        IMEINumber = telephonyManager.getDeviceId();
        Toast.makeText(this, "IMEI : "+IMEINumber, Toast.LENGTH_SHORT).show();
        System.out.println("IMEI Number :"+IMEINumber+"\n");
        textView.setText(IMEINumber);
        /* commented text
//        String result = convertStringToBinary(IMEINumber);

//        System.out.println(result);

         */
        long no= Long.parseLong(IMEINumber);
        decimalToBinary(no);
        // pretty print the binary format
        /* commented text
//        System.out.println(prettyBinary(result, 8, " "));

         */
        }

@Override

public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {

    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    switch (requestCode) {

        case REQUEST_CODE: {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                  Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show();
            }
            else {
//                  Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
            }

        }

    }
}
    public static String convertStringToBinary(String input) {

        StringBuilder result = new StringBuilder();
        char[] chars = input.toCharArray();
        for (char aChar : chars) {
            result.append(
                    String.format("%8s", Integer.toBinaryString(aChar))   // char -> int, auto-cast
                            .replaceAll(" ", "0")                         // zero pads
            );
        }
        return result.toString();

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static String prettyBinary(String binary, int blockSize, String separator) {

        List<String> result = new ArrayList<>();
        int index = 0;
        while (index < binary.length()) {
            result.add(binary.substring(index, Math.min(index + blockSize, binary.length())));
            index += blockSize;
        }

        return result.stream().collect(Collectors.joining(separator));
    }
    static void printBinary(long[] binary, int id)
    {
        System.out.print("Yash");
        // Iteration over array
        for (int i = id - 1; i >= 0; i--) {
            System.out.print("Hello");
            System.out.print(binary[i] + "");
        }
    }

    // Function converting decimal to binary
    public static void decimalToBinary(long num)
    {
        // Creating and assigning binary array size
        long[] binary = new long[50];
        int id = 0;

        // Number should be positive
        while (num > 0) {
            binary[id++] = num % 2;
            num = num / 2;
        }

        // Print Binary
        printBinary(binary, id);
    }

    // Main Driver Code

}