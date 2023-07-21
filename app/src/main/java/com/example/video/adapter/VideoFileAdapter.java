package com.example.video.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.video.modals.MediaFiles;
import com.example.video.R;
import com.example.video.modals.Utility;
import com.example.video.activities.VideoPlayerActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.util.ArrayList;

import es.dmoral.toasty.Toasty;

public class VideoFileAdapter extends RecyclerView.Adapter<VideoFileAdapter.ViewHolder> {
    private ArrayList<MediaFiles> videoList;
    private Context context;
    BottomSheetDialog bottomSheetDialog;
    private int viewType;

    public VideoFileAdapter(ArrayList<MediaFiles> videoList, Context context, int viewType) {
        this.videoList = videoList;
        this.context = context;
        this.viewType = viewType;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.video_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.videoName.setText(videoList.get(position).getDisplayName());
        String size = videoList.get(position).getSize();
        holder.videoSize.setText(Formatter.formatFileSize(context, Long.parseLong(size)));
        double milliSeconds = Double.parseDouble(videoList.get(position).getDuration());
        holder.videoDuration.setText(Utility.timeConversion((long) milliSeconds));
        Glide.with(context).load(new File(videoList.get(position).getPath())).into(holder.thumbnail);

        if (viewType == 0) {
            holder.menu_more.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    bottomSheetDialog = new BottomSheetDialog(context, R.style.BottomSheetTheme);
                    View bsView = LayoutInflater.from(context).inflate(R.layout.video_bs_layout,
                            view.findViewById(R.id.bottom_sheet));
                    bsView.findViewById(R.id.bs_play).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            holder.itemView.performClick();
                            bottomSheetDialog.dismiss();
                        }
                    });
                    bsView.findViewById(R.id.bs_rename).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                            alertDialog.setTitle(R.string.rename);
                            EditText editText = new EditText(context);
                            editText.setMaxLines(1);
                            String path = videoList.get(position).getPath();
                            final File file = new File(path);
                            String videoName = file.getName();
                            videoName = videoName.substring(0, videoName.lastIndexOf("."));
                            editText.setText(videoName);
                            alertDialog.setView(editText);
                            editText.requestFocus();

                            alertDialog.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                                @SuppressLint("NotifyDataSetChanged")
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if (TextUtils.isEmpty(editText.getText().toString())) {
                                        Toasty.info(context, context.getString(R.string.please_enter_video_name), Toast.LENGTH_SHORT).show();
                                        return;
                                    } else {
                                        String onlyPath = file.getParentFile().getAbsolutePath();
                                        String ext = file.getAbsolutePath();
                                        ext = ext.substring(ext.lastIndexOf("."));
                                        String newPath = onlyPath + "/" + editText.getText().toString() + ext;
                                        File newFile = new File(newPath);
                                        boolean rename = file.renameTo(newFile);
                                        if (rename) {
                                            ContentResolver resolver = context.getApplicationContext().getContentResolver();
                                            resolver.delete(MediaStore.Files.getContentUri("external"),
                                                    MediaStore.MediaColumns.DATA + "=?", new String[]
                                                            {file.getAbsolutePath()});
                                            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                            intent.setData(Uri.fromFile(newFile));
                                            context.getApplicationContext().sendBroadcast(intent);

//                                    notifyDataSetChanged();
//                                    Toast.makeText(context, "Video Renamed", Toast.LENGTH_SHORT).show();
                                            videoList.get(position).setPath(newPath);
                                            holder.videoName.setText(editText.getText().toString() + ext);
                                            Toasty.success(context, R.string.video_renamed, Toast.LENGTH_SHORT).show();
                                            intent.putExtra("video_title", holder.videoName.getText().toString());

                                        } else {
                                            Toasty.error(context, R.string.process_failed, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            });
                            alertDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            });
                            alertDialog.create().show();
                            bottomSheetDialog.dismiss();
                        }
                    });

                    bsView.findViewById(R.id.bs_share).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Uri uri = Uri.parse(videoList.get(position).getPath());
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("video/*");
                            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                            context.startActivity(Intent.createChooser(shareIntent, "Share Video via"));
                            bottomSheetDialog.dismiss();
                        }
                    });

                    bsView.findViewById(R.id.bs_delete).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                            alertDialog.setTitle(R.string.delete);
                            alertDialog.setMessage(R.string.are_you_sure_you_want_to_delete);
                            alertDialog.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Uri contentUri = ContentUris
                                            .withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                                    Long.parseLong(videoList.get(position).getId()));
                                    File file = new File(videoList.get(position).getPath());
                                    boolean delete = file.delete();
                                    if (delete) {
                                        context.getContentResolver().delete(contentUri, null, null);
                                        videoList.remove(position);
                                        notifyItemRemoved(position);
                                        notifyItemRangeChanged(position, videoList.size());
                                        Toast.makeText(context, R.string.video_deleted, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toasty.error(context, R.string.can_t_deleted, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            }).show();
                            bottomSheetDialog.dismiss();
                        }
                    });

                    bsView.findViewById(R.id.bs_properties).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String one = "File: " + videoList.get(position).getDisplayName();
                            String path = videoList.get(position).getPath();
                            int indexOfPath = path.lastIndexOf("/");
                            String two = "Path: " + path.substring(0, indexOfPath);
                            String three = "Size: " + android.text.format.Formatter
                                    .formatFileSize(context, Long.parseLong(videoList.get(position).getSize()));
                            String four = "Length: " + Utility.timeConversion((long) milliSeconds);
                            String nameWithFormat = videoList.get(position).getDisplayName();
                            int index = nameWithFormat.lastIndexOf(".");
                            String format = nameWithFormat.substring(index + 1);
                            String five = "Format: " + format;

                            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                            mediaMetadataRetriever.setDataSource(videoList.get(position).getPath());
                            String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                            String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);

                            String six = "Resolution: " + width + "X" + height;

                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context)
                                    .setTitle("properties")
                                    .setMessage(one + "\n\n" + two + "\n\n" + three + "\n\n" + four + "\n\n" + five + "\n\n" + six)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            dialogInterface.dismiss();
                                        }
                                    });
                            alertDialog.show();
                            bottomSheetDialog.dismiss();
                        }
                    });

                    bottomSheetDialog.setContentView(bsView);
                    bottomSheetDialog.show();
                }
            });
        } else {
            holder.menu_more.setVisibility(View.GONE);
            holder.videoName.setTextColor(Color.WHITE);
            holder.videoSize.setTextColor(Color.WHITE);
        }
        holder.menu_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog = new BottomSheetDialog(context, R.style.BottomSheetTheme);
                View bsView = LayoutInflater.from(context).inflate(R.layout.video_bs_layout,
                        view.findViewById(R.id.bottom_sheet));
                bsView.findViewById(R.id.bs_play).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        holder.itemView.performClick();
                        bottomSheetDialog.dismiss();
                    }
                });
                bsView.findViewById(R.id.bs_rename).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                        alertDialog.setTitle(R.string.rename);
                        EditText editText = new EditText(context);
                        editText.setMaxLines(1);
                        String path = videoList.get(position).getPath();
                        final File file = new File(path);
                        String videoName = file.getName();
                        videoName = videoName.substring(0, videoName.lastIndexOf("."));
                        editText.setText(videoName);
                        alertDialog.setView(editText);
                        editText.requestFocus();

                        alertDialog.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
                            @SuppressLint("NotifyDataSetChanged")
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if (TextUtils.isEmpty(editText.getText().toString())) {
                                    Toasty.warning(context, R.string.please_enter_video_name, Toast.LENGTH_SHORT).show();
                                    return;
                                } else {
                                    String onlyPath = file.getParentFile().getAbsolutePath();
                                    String ext = file.getAbsolutePath();
                                    ext = ext.substring(ext.lastIndexOf("."));
                                    String newPath = onlyPath + "/" + editText.getText().toString() + ext;
                                    File newFile = new File(newPath);
                                    boolean rename = file.renameTo(newFile);
                                    if (rename) {
                                        ContentResolver resolver = context.getApplicationContext().getContentResolver();
                                        resolver.delete(MediaStore.Files.getContentUri("external"),
                                                MediaStore.MediaColumns.DATA + "=?", new String[]
                                                        {file.getAbsolutePath()});
                                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                        intent.setData(Uri.fromFile(newFile));
                                        context.getApplicationContext().sendBroadcast(intent);

//                                    notifyDataSetChanged();
//                                    Toast.makeText(context, "Video Renamed", Toast.LENGTH_SHORT).show();
                                        videoList.get(position).setPath(newPath);
                                        holder.videoName.setText(editText.getText().toString() + ext);
                                        Toasty.info(context, R.string.video_renamed, Toast.LENGTH_SHORT).show();
                                        intent.putExtra("video_title", holder.videoName.getText().toString());

                                    } else {
                                        Toasty.error(context, R.string.process_failed, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
                        alertDialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        alertDialog.create().show();
                        bottomSheetDialog.dismiss();
                    }
                });

                bsView.findViewById(R.id.bs_share).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Uri uri = Uri.parse(videoList.get(position).getPath());
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("video/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        context.startActivity(Intent.createChooser(shareIntent, "Share Video via"));
                        bottomSheetDialog.dismiss();
                    }
                });

                bsView.findViewById(R.id.bs_delete).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                        alertDialog.setTitle(R.string.delete);
                        alertDialog.setMessage(R.string.are_you_sure_you_want_to_delete);
                        alertDialog.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Uri contentUri = ContentUris
                                        .withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                                Long.parseLong(videoList.get(position).getId()));
                                File file = new File(videoList.get(position).getPath());
                                boolean delete = file.delete();
                                if (delete) {
                                    context.getContentResolver().delete(contentUri, null, null);
                                    videoList.remove(position);
                                    notifyItemRemoved(position);
                                    notifyItemRangeChanged(position, videoList.size());
                                    Toasty.info(context, R.string.video_deleted, Toast.LENGTH_SHORT).show();
                                } else {
                                    Toasty.error(context, R.string.can_t_deleted, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).show();
                        bottomSheetDialog.dismiss();
                    }
                });

                bsView.findViewById(R.id.bs_properties).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String one = "File: " + videoList.get(position).getDisplayName();
                        String path = videoList.get(position).getPath();
                        int indexOfPath = path.lastIndexOf("/");
                        String two = "Path: " + path.substring(0, indexOfPath);
                        String three = "Size: " + android.text.format.Formatter
                                .formatFileSize(context, Long.parseLong(videoList.get(position).getSize()));
                        String four = "Length: " + Utility.timeConversion((long) milliSeconds);
                        String nameWithFormat = videoList.get(position).getDisplayName();
                        int index = nameWithFormat.lastIndexOf(".");
                        String format = nameWithFormat.substring(index + 1);
                        String five = "Format: " + format;

                        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                        mediaMetadataRetriever.setDataSource(videoList.get(position).getPath());
                        String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                        String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);

                        String six = "Resolution: " + width + "X" + height;

                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context)
                                .setTitle(R.string.properties)
                                .setMessage(one + "\n\n" + two + "\n\n" + three + "\n\n" + four + "\n\n" + five + "\n\n" + six)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                });
                        alertDialog.show();
                        bottomSheetDialog.dismiss();
                    }
                });

                bottomSheetDialog.setContentView(bsView);
                bottomSheetDialog.show();
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, VideoPlayerActivity.class);
                intent.putExtra("position", position);
                intent.putExtra("video_title", videoList.get(position).getDisplayName());
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("videoArrayList", videoList);
                intent.putExtras(bundle);
                context.startActivity(intent);
                if (viewType == 1) {
                    ((Activity) context).finish();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail, menu_more;
        TextView videoName, videoSize, videoDuration;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            menu_more = itemView.findViewById(R.id.video_menu_more);
            videoName = itemView.findViewById(R.id.video_name);
            videoSize = itemView.findViewById(R.id.video_size);
            videoDuration = itemView.findViewById(R.id.video_duration);
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    public void updateVideoFiles(ArrayList<MediaFiles> files) {
        videoList = new ArrayList<>();
        videoList.addAll(files);
        notifyDataSetChanged();
    }
}
