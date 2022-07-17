package com.example.botimei;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.botimei.adapter.ShowUserAdapter;
import com.example.botimei.model.FileShared;
import com.example.botimei.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    public String username = "";
    List<UserModel> userModelList = new ArrayList<>();
    RecyclerView rv_showAllUsers;
    ShowUserAdapter adapter;
    private static final int REQUEST_CODE = 101;


    CircleImageView cv_showAllReceivedFiles, cv_fileReceived;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String imei;
        cv_fileReceived = findViewById(R.id.cv_fileReceived);
        cv_showAllReceivedFiles = findViewById(R.id.cv_showAllReceivedFiles);
        rv_showAllUsers = findViewById(R.id.rv_showAllUsers);
        rv_showAllUsers.setHasFixedSize(true);
        rv_showAllUsers.setLayoutManager(new LinearLayoutManager(MainActivity.this));


        username = getIntent().getStringExtra("username");
        Log.d("TAG1", "username: " + username);
        Toast.makeText(MainActivity.this, "username: " + username, Toast.LENGTH_SHORT).show();

        cv_fileReceived.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });
        cv_showAllReceivedFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserToReceivedFiles();
            }
        });

        loadUser();

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE);
            return;
        }
        imei= telephonyManager.getDeviceId();
//        Toast.makeText(this, "IMEI : "+imei, Toast.LENGTH_SHORT).show();

    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        final View customLayout = getLayoutInflater().inflate(R.layout.layout_receive_file, null);
//        EditText et_fileCode = customLayout.findViewById(R.id.et_fileCode);

        File file = null;

        Button btn_receive = customLayout.findViewById(R.id.btn_receive);

        builder.setView(customLayout);
        AlertDialog alertDialog = builder.create();

        alertDialog.show();

        btn_receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String keyAES = "AES" + username;
                String keyDES = "DES" + username;

                try {
                    MyEncryptionClass.decryptionAES(file, keyAES);
                    MyEncryptionClass.decryptionDES(file, keyDES);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                ProgressDialog dialog = new ProgressDialog(MainActivity.this);
                dialog.setMessage("Decrypting");

                dialog.show();
//                String code = et_fileCode.getText().toString();

//                if (code.isEmpty()) {
//                    et_fileCode.setError("Empty code");
//                    return;
//                } else {
                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                firebaseUser.getUid();
                Log.d("TAG1", "onDataChange: reached here");
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(UserRegister.FILE_SHARED).child(username);
                reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Log.d("TAG1", "onDataChange: reached here too");
//                                    List<FileShared> model = dataSnapshot.getValue();
//                                    if (dataSnapshot.getKey().equalsIgnoreCase(username)) {
                            DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference().child(UserRegister.FILE_RECEIVED).child(username);
                            reference1.push().setValue(dataSnapshot.getValue()).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    dialog.dismiss();
//                                                    sendUserToReceivedFiles();
                                    Toast.makeText(MainActivity.this, "File Received Successfully", Toast.LENGTH_SHORT).show();
                                }
                            });
//                                    }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, "Wrong Code", Toast.LENGTH_SHORT).show();
                    }
                });
                //                }
            }
        });

    }

    private void sendUserToReceivedFiles() {
        Intent intent = new Intent(MainActivity.this, ShowAllReceivedFiles.class);
        intent.putExtra("username", username);
        startActivity(intent);
    }

    private void loadUser() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser.getUid() != null) {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(UserRegister.RIDER_USERS);
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    userModelList.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        UserModel model = dataSnapshot.getValue(UserModel.class);
                        if (!model.getId().equals(firebaseUser.getUid())) {
                            userModelList.add(model);
                        } else {
                            username = model.getUsername();
                        }
                    }
                    adapter = new ShowUserAdapter(MainActivity.this, userModelList, username);
                    rv_showAllUsers.setAdapter(adapter);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

}