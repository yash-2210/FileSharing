package com.example.botimei.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.botimei.R;
import com.example.botimei.ShareFile;
import com.example.botimei.model.FileShared;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ShowFileReceivedAdapter extends RecyclerView.Adapter<ShowFileReceivedAdapter.viewholder> {

    Context context;
    List<FileShared> fileSharedList = new ArrayList<>();
    String data [];

    public ShowFileReceivedAdapter(Context context, List<FileShared> fileSharedList) {
        this.context = context;
        this.fileSharedList = fileSharedList;
    }

    @NonNull
    @Override
    public viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_file_received, parent, false);
        return new viewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewholder holder, int position) {
        FileShared model = fileSharedList.get(position);

        String getBase64audio = model.getData();
//        System.out.println(getBase64audio);

        String decodefile = extractCharacters(model.getFile());
//        System.out.println("Demo Decode: "+decodefile);

        byte[] decodedBytes = Base64.getDecoder().decode(model.getFilename());
        String decodedString = new String(decodedBytes);
        String s = " ";
        for(int index = 0; index < decodedString.length(); index+=9) {
            String temp = decodedString.substring(index, index+8);
            int num = Integer.parseInt(temp,2);
            char letter = (char) num;
            s = s+letter;
        }
        data = s.split(";");

        holder.tv_fileName.setText(data[3]);
        holder.tv_fileSender.setText("sender: " + model.getSender());


        holder.tv_imei.setText("Device ID: ");
        holder.tv_contact.setText("Contact: ");
        holder.tv_SMS.setText("SMS: ");

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decodeAndWriteToFile(context,getBase64audio,data[3]);
                Toast.makeText(context,"File Downloaded Successfully",Toast.LENGTH_SHORT).show();
                holder.tv_imei.setText("Device ID: " + data[0]);
                holder.tv_contact.setText("Contact: " + data[1]);
                holder.tv_SMS.setText("SMS: " + data[2]);
//                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(model.getFileUrl()));
//                context.startActivity(browserIntent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return fileSharedList.size();
    }

    class viewholder extends RecyclerView.ViewHolder {

        TextView tv_fileName, tv_fileSender, tv_imei, tv_contact, tv_SMS;

        public viewholder(@NonNull View itemView) {
            super(itemView);

            tv_fileName = itemView.findViewById(R.id.tv_fileName);
            tv_fileSender = itemView.findViewById(R.id.tv_fileSender);
            tv_imei = itemView.findViewById(R.id.tv_imei);
            tv_contact = itemView.findViewById(R.id.tv_contact);
            tv_SMS = itemView.findViewById(R.id.tv_SMS);

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

    public static String extractCharacters(String string) {
        List<Integer> primeSeries = new ArrayList<>();
        for (int i = 2; i <= string.length(); i++) {
            if (isPrime(i)) {
                primeSeries.add(i);
            }
        }

        StringBuilder result = new StringBuilder();
        int index = 0; // Start with index 1 since we need to extract after 2 positions initially
        int semicolonIndex = string.lastIndexOf(";");
        // System.out.println(semicolonIndex);

        for (int step : primeSeries) {
            index = index + step;
            if (index >= string.length() || index == semicolonIndex) {
                break;
            }
            result.append(string.charAt(index));
            index = index + 1;
        }

        return result.toString();
    }

    public static void decodeAndWriteToFile(Context context, String base64EncodedAudio, String filename) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(base64EncodedAudio);
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), filename);
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(decodedBytes);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
