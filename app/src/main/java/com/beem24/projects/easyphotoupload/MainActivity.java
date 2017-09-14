package com.beem24.projects.easyphotoupload;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.beem24.projects.easyphotoupload.core.ImageCompressTask;
import com.beem24.projects.easyphotoupload.listeners.IImageCompressTaskListener;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    Button selectImage;
    ImageView selectedImage;

    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final int REQUEST_PICK_PHOTO = 101;

    //create a single thread pool to our image compression class.
    private ExecutorService mExecutorService = Executors.newFixedThreadPool(1);

    private ImageCompressTask imageCompressTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectedImage = (ImageView) findViewById(R.id.iv_selected_photo);
        selectImage = (Button) findViewById(R.id.btn_select_image);

        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestPermission();
            }
        });
    }

    void requestPermission() {

        if(PackageManager.PERMISSION_GRANTED !=
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            }else {
                //Yeah! I want both block to do the same thing, you can write your own logic, but this works for me.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            }
        }else {
            //Permission Granted, lets go pick photo
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_PHOTO);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_PICK_PHOTO && resultCode == RESULT_OK &&
                data != null) {
            //extract absolute image path from Uri
            Uri uri = data.getData();
            Cursor cursor = MediaStore.Images.Media.query(getContentResolver(), uri, new String[]{MediaStore.Images.Media.DATA});

            if(cursor != null && cursor.moveToFirst()) {
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));

                //Create ImageCompressTask and execute with Executor.
                imageCompressTask = new ImageCompressTask(this, path, iImageCompressTaskListener);

                mExecutorService.execute(imageCompressTask);
            }
        }
    }

    //image compress task callback
    private IImageCompressTaskListener iImageCompressTaskListener = new IImageCompressTaskListener() {
        @Override
        public void onComplete(List<File> compressed) {
            //photo compressed. Yay!

            //prepare for uploads.

            File file = compressed.get(0);

            Log.d("ImageCompressor", "New photo size ==> " + file.length()); //log new file size.

            selectedImage.setImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
        }

        @Override
        public void onError(Throwable error) {
            //very unlikely, but it might happen on a device with extremely low storage.
            //log it, log.WhatTheFuck?, or show a dialog asking the user to delete some files....etc, etc
            Log.wtf("ImageCompressor", "Error occurred", error);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //clean up!
        mExecutorService.shutdown();

        mExecutorService = null;
        imageCompressTask = null;
    }
}
