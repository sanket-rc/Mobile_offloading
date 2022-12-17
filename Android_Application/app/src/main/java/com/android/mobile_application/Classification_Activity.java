package com.android.mobile_application;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class Classification_Activity extends AppCompatActivity {
    // Initialize variables
    ImageView imageView;
    Button btnOpen;
    Button btnNext;
    Bitmap captureImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classify);

        // Assign variables
        imageView = findViewById(R.id.image_view);
        btnOpen = findViewById(R.id.bt_open);
        btnNext = (Button)findViewById(R.id.nav_btn);

        // Request for Camera permission
        if(ContextCompat.checkSelfPermission( Classification_Activity.this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(Classification_Activity.this,
                    new String[]{
                            Manifest.permission.CAMERA
                    }, 100);
        }

        btnOpen.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                // to Launch (or) Open the Camera
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent,100);
            }
        });

        btnNext.setOnClickListener((view) -> openActivitySecond());
    }

    public void openActivitySecond(){
        // Check if the image is captured or not (captureImage is empty)
        if (captureImage != null) {
            Intent intent = new Intent(this, ImageDataActivity.class);
            intent.putExtra("bitmapImage", captureImage);
            startActivity(intent);
        }else{
            DialogBox dialogBox = new DialogBox();
            dialogBox.show(getSupportFragmentManager(), "Error Dialogue");
        }
    }

    // Display the image that is captures
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            captureImage = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(captureImage);
        }
    }
}