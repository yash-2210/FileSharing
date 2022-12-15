package com.example.botimei.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.botimei.R;
import com.example.botimei.model.FileShared;

import java.util.Base64;
import java.util.List;

public class ShowFileSharedAdapter extends RecyclerView.Adapter<ShowFileSharedAdapter.viewholder> {

    List<FileShared> fileSharedList;
    Context context;

    public ShowFileSharedAdapter(Context context, List<FileShared> fileSharedList) {
        this.fileSharedList = fileSharedList;
        this.context = context;
    }

    @NonNull
    @Override
    public viewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_file_shared, parent, false);
        return new viewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull viewholder holder, int position) {
        FileShared model = fileSharedList.get(position);
        byte[] decodedBytes = Base64.getDecoder().decode(model.getFilename());
        String decodedString = new String(decodedBytes);
        String s = " ";
        for(int index = 0; index < decodedString.length(); index+=9) {
            String temp = decodedString.substring(index, index+8);
            int num = Integer.parseInt(temp,2);
            char letter = (char) num;
            s = s+letter;
        }
        String data [] = s.split(";");
        holder.tv_fileName.setText(data[3]);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(model.getFileUrl()));
                context.startActivity(browserIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return fileSharedList.size();
    }

    class viewholder extends RecyclerView.ViewHolder {

        TextView tv_fileName;

        public viewholder(@NonNull View itemView) {
            super(itemView);
            tv_fileName = itemView.findViewById(R.id.tv_fileName);
        }
    }
}
