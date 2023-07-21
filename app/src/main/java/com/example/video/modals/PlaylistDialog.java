package com.example.video.modals;

import static com.example.video.activities.VideoFileActivity.MY_PREF;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.video.R;
import com.example.video.adapter.VideoFileAdapter;
import com.example.video.modals.MediaFiles;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

public class PlaylistDialog extends BottomSheetDialogFragment {
    ArrayList<MediaFiles> arrayList = new ArrayList<>();
    VideoFileAdapter videoFileAdapter;
    BottomSheetDialog bottomSheetDialog;
    RecyclerView recyclerView;
    TextView folder;

    public PlaylistDialog(ArrayList<MediaFiles> arrayList, VideoFileAdapter videoFileAdapter) {
        this.arrayList = arrayList;
        this.videoFileAdapter = videoFileAdapter;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        bottomSheetDialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.playlist_bs_layout, null);
        bottomSheetDialog.setContentView(view);

        recyclerView = view.findViewById(R.id.playlist_rv);
        folder = view.findViewById(R.id.playlistName);

        SharedPreferences preferences = this.getActivity().getSharedPreferences(MY_PREF, Context.MODE_PRIVATE);
        String folderName = preferences.getString("playlistFolderName", "abc");
        folder.setText(folderName);

        arrayList = fetchMedia(folderName);
        videoFileAdapter = new VideoFileAdapter(arrayList, getContext(),1);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(videoFileAdapter);
        videoFileAdapter.notifyDataSetChanged();

        return bottomSheetDialog;
    }

    private ArrayList<MediaFiles> fetchMedia(String folderName) {


        ArrayList<MediaFiles> videoFiles = new ArrayList<>();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String selection = MediaStore.Video.Media.DATA + " like?";
        String[] selectionArg = new String[]{"%" + folderName + "%"};
        Cursor cursor = getContext().getContentResolver().query(uri, null,
                selection, selectionArg, null);
        if (cursor != null && cursor.moveToNext()) {
            do {
                @SuppressLint("Range") String id = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media._ID));
                @SuppressLint("Range") String title = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.TITLE));
                @SuppressLint("Range") String displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
                @SuppressLint("Range") String size = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE));
                @SuppressLint("Range") String duration = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DURATION));
                @SuppressLint("Range") String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
                @SuppressLint("Range") String dateAdded = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED));

                MediaFiles mediaFiles = new MediaFiles(id, title, displayName, size, duration, path,
                        dateAdded);
                videoFiles.add(mediaFiles);

            } while (cursor.moveToNext());
        }
        return videoFiles;
    }
}
