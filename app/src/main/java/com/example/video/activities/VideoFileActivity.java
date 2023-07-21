package com.example.video.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;

import com.example.video.modals.MediaFiles;
import com.example.video.R;
import com.example.video.adapter.VideoFileAdapter;

import java.util.ArrayList;

public class VideoFileActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    public static final String MY_PREF = "my pref";
    RecyclerView recyclerView;
    private ArrayList<MediaFiles> videoFilesArrayList = new ArrayList<>();
    @SuppressLint("StaticFieldLeak")
    static VideoFileAdapter videoFileAdapter;
    String folderName;
    SwipeRefreshLayout swipeRefreshLayout;
    String sortOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_file);
        folderName = getIntent().getStringExtra("folderName");
        getSupportActionBar().setTitle(folderName);
        recyclerView = findViewById(R.id.videos_rv);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_videos);
        showVideoFiles();
        SharedPreferences.Editor editor = getSharedPreferences(MY_PREF, MODE_PRIVATE).edit();
        editor.putString("playlistFolderName", folderName);
        editor.apply();
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                showVideoFiles();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void showVideoFiles() {
        videoFilesArrayList = fetchMedia(folderName);
        videoFileAdapter = new VideoFileAdapter(videoFilesArrayList, this,0);
        recyclerView.setAdapter(videoFileAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,
                RecyclerView.VERTICAL, false));
        videoFileAdapter.notifyDataSetChanged();
    }

    private ArrayList<MediaFiles> fetchMedia(String folderName) {
        SharedPreferences preferences = getSharedPreferences(MY_PREF, MODE_PRIVATE);
        String sort_value = preferences.getString("sort", "abcd");

        ArrayList<MediaFiles> videoFiles = new ArrayList<>();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        if (sort_value.equals("sortName")) {
            sortOrder = MediaStore.MediaColumns.DISPLAY_NAME + " ASC";
        } else if (sort_value.equals("sortSize")) {
            sortOrder = MediaStore.MediaColumns.SIZE + " DESC";
        } else if (sort_value.equals("sortDate")) {
            sortOrder = MediaStore.MediaColumns.DATE_ADDED + " DESC";
        } else {
            sortOrder = MediaStore.MediaColumns.DURATION + " DESC";
        }
        String selection = MediaStore.Video.Media.DATA + " like?";
        String[] selectionArg = new String[]{"%" + folderName + "%"};
        Cursor cursor = getContentResolver().query(uri, null,
                selection, selectionArg, sortOrder);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.video_menu, menu);
        MenuItem mediaItem = menu.findItem(R.id.search_video);
        SearchView searchView = (SearchView) mediaItem.getActionView();
        searchView.setOnQueryTextListener(this);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences preferences = getSharedPreferences(MY_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        int id = item.getItemId();
        if (id == R.id.refresh_files) {
            finish();
            startActivity(getIntent());
        } else if (id == R.id.sort_by) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.sort_by)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            editor.apply();
                            finish();
                            startActivity(getIntent());
                            dialogInterface.dismiss();
                        }
                    });
            String[] items = {getString(R.string.name_a_to_z), getString(R.string.size), getString(R.string.date), getString(R.string.length)};
            alertDialog.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    switch (i) {
                        case 0:
                            editor.putString("sort", "sortName");
                            break;
                        case 1:
                            editor.putString("sort", "sortSize");
                            break;
                        case 2:
                            editor.putString("sort", "sortDate");
                            break;
                        case 3:
                            editor.putString("sort", "sortLength");
                            break;
                    }
                }
            });
            alertDialog.create().show();

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        String input = s.toLowerCase();
        ArrayList<MediaFiles> mediaFiles = new ArrayList<>();
        for (MediaFiles media : videoFilesArrayList) {
            if (media.getTitle().toLowerCase().contains(input)) {
                mediaFiles.add(media);
            }
        }
        VideoFileActivity.videoFileAdapter.updateVideoFiles(mediaFiles);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        String input = s.toLowerCase();
        ArrayList<MediaFiles> mediaFiles = new ArrayList<>();
        for (MediaFiles media : videoFilesArrayList) {
            if (media.getTitle().toLowerCase().contains(input)) {
                mediaFiles.add(media);
            }
        }
        VideoFileActivity.videoFileAdapter.updateVideoFiles(mediaFiles);
        return true;
    }
}