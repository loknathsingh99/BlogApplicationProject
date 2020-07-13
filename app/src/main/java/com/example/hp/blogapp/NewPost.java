package com.example.hp.blogapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import id.zelory.compressor.Compressor;

public class NewPost extends AppCompatActivity {
    private static final int MAX_LENGTH = 100;
    private Toolbar toolbarNewPost;
    private Button submitBtn;
    private EditText newPostText;
    private String user_id;
    private FirebaseAuth mauth;
    private FirebaseFirestore firebaseFirestore;
    private StorageReference storageReference;

    private FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    private ImageView newPostImg;
    private Uri main_uri = null;
    private ProgressBar progressBar;
    private String postText;
    private Bitmap compressedImageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);


        firebaseDatabase = FirebaseDatabase.getInstance();

        //Toolbar
        toolbarNewPost = findViewById(R.id.toolbarNewPost);
        setSupportActionBar(toolbarNewPost);
        getSupportActionBar().setTitle("Add New Post");
        //add back button to parent activity
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Firebase Variables
        mauth = FirebaseAuth.getInstance();
        user_id = mauth.getCurrentUser().getUid();
        firebaseFirestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        databaseReference = firebaseDatabase.getReference("Users/" +mauth.getCurrentUser().getUid()+ "/Post Details");


        newPostImg = findViewById(R.id.newPostImage);
        newPostText = findViewById(R.id.newPostDesc);
        submitBtn = findViewById(R.id.newPostBtn);
        progressBar = findViewById(R.id.newPostProgress);

        newPostImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setMinCropResultSize(512, 512)
                        .setAspectRatio(1, 1)
                        .start(NewPost.this);
            }
        });

        //clicked on submit
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postText = newPostText.getText().toString();
                progressBar.setVisibility(View.VISIBLE);
                if (main_uri != null && !TextUtils.isEmpty(postText)) {
//                    final String timestamp = FieldValue.serverTimestamp().toString();

                    progressBar.setVisibility(View.VISIBLE);
                    final String randomName = UUID.randomUUID().toString();


                    //Create storage path reference
                    StorageReference filePath = storageReference.child("post_images").child(randomName + ".jpg");
                    //url of image to be put in the above path
                    //This uploads image to Post_images folder in Storage..To determine heirchachy u need to use map to put in collections
                    filePath.putFile(main_uri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull final Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {


                                storageReference.child("post_images").child(randomName + ".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        final String downloadUrl = uri.toString();


                                        //upload thumbnail compressed
                                        File imageFile = new File(main_uri.getPath());

                                        try {
                                            compressedImageBitmap = new Compressor(NewPost.this)
                                                    .setMaxHeight(100)
                                                    .setMaxWidth(100)
                                                    .setQuality(1)
                                                    .compressToBitmap(imageFile);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                        //Uploadinf bitmap to firebase
                                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                        compressedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                        byte[] thumbBitmap = baos.toByteArray();

                                        UploadTask thumbImage = storageReference.child("/post_images/thumbs").child(randomName + ".jpg").putBytes(thumbBitmap);
                                        thumbImage.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                            @Override
                                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                if (task.isSuccessful()) {


                                                    storageReference.child("/post_images/thumbs").child(randomName + ".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                        @Override
                                                        public void onSuccess(Uri uri) {
                                                            String thumbUri = uri.toString();


                                                            NewPostModelclass postMap = new NewPostModelclass(postText,"JAVA",downloadUrl,user_id,thumbUri,FieldValue.serverTimestamp().toString());

                                                            //Finally add everything to collection
                                                            //we dont add .document as it must be created and named randomly by firebase automatically
                                                            databaseReference.setValue(postMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        progressBar.setVisibility(View.INVISIBLE);
                                                                        Toast.makeText(NewPost.this, "Successfully Posted", Toast.LENGTH_LONG).show();
                                                                        Intent main = new Intent(NewPost.this, MainActivity.class);
                                                                        startActivity(main);
                                                                        finish();
                                                                    } else {
                                                                        Toast.makeText(NewPost.this, "Error" + task.getException().toString(), Toast.LENGTH_LONG).show();
                                                                        progressBar.setVisibility(View.INVISIBLE);

                                                                    }
                                                                }

                                                            });
                                                        }
                                                    });
                                                } else {
                                                    Toast.makeText(NewPost.this, "Error" + task.getException().toString(), Toast.LENGTH_LONG).show();
                                                    progressBar.setVisibility(View.INVISIBLE);
                                                }
                                            }
                                        });
                                    }
                                });

                            }
                        }
                    });
                } else {
                    Toast.makeText(NewPost.this, "No Image or Description", Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.INVISIBLE);
                }

            }

        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                main_uri = result.getUri();
                newPostImg.setImageURI(main_uri);

//                String tct = main_uri.toString();
//                Toast.makeText(AccounrSetup.this,tct,Toast.LENGTH_LONG).show();

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Toast.makeText(NewPost.this, "Error" + error, Toast.LENGTH_LONG).show();

            }
        }
    }

    public static String random() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(MAX_LENGTH);
        char tempChar;
        for (int i = 0; i < randomLength; i++) {
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

}
