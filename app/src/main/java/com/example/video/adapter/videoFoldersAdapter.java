package com.example.video.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.video.modals.MediaFiles;
import com.example.video.R;
import com.example.video.activities.VideoFileActivity;

import java.util.ArrayList;

public class videoFoldersAdapter extends RecyclerView.Adapter<videoFoldersAdapter.ViewHolder> {
    private ArrayList<MediaFiles> mediaFiles;
    private ArrayList<String> folderPath;
    private Context context;

    public videoFoldersAdapter(ArrayList<MediaFiles> mediaFiles, ArrayList<String> folderPath, Context context) {
        this.mediaFiles = mediaFiles;
        this.folderPath = folderPath;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.folders_item, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // storage/Media/Videos
        int indexPath = folderPath.get(position).lastIndexOf("/");
        String nameOfFolder = folderPath.get(position).substring(indexPath + 1);
        holder.folderName.setText(nameOfFolder);
        holder.folderPath.setText(folderPath.get(position));
        holder.noOfFiles.setText(noOfFiles(folderPath.get(position)) + context.getString(R.string.videos));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, VideoFileActivity.class);
                intent.putExtra("folderName", nameOfFolder);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return folderPath.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView folderName, folderPath, noOfFiles;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            folderName = itemView.findViewById(R.id.folderName);
            folderPath = itemView.findViewById(R.id.folderPath);
            noOfFiles = itemView.findViewById(R.id.noOfFolders);
        }
    }

    int noOfFiles(String folderName) {
        int fileNo = 0;
        for (MediaFiles mediaFiles : mediaFiles) {
            if (mediaFiles.getPath().substring(0, mediaFiles.getPath().lastIndexOf("/")).endsWith(folderName)) {
                fileNo++;
            }
        }
        return fileNo;
    }
}
