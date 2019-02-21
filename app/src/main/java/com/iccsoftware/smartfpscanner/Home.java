package com.iccsoftware.smartfpscanner;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.iccsoftware.smartfpscanner.Common.Common;
import com.iccsoftware.smartfpscanner.Common.Permissions;
import com.iccsoftware.smartfpscanner.Model.UserModel;
import com.iccsoftware.smartfpscanner.ViewHolder.BottomSheetFragment;
import com.rohitarya.picasso.facedetection.transformation.FaceCenterCrop;
import com.rohitarya.picasso.facedetection.transformation.core.PicassoFaceDetector;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

import dmax.dialog.SpotsDialog;
import id.zelory.compressor.Compressor;
import io.paperdb.Paper;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class Home extends AppCompatActivity {

    private ImageView exitActivity, help;
    private TextView activityName;
    private RelativeLayout rootLayout;
    private Toolbar mToolbar;
    private BottomSheetFragment bottomSheetFragment;

    /*---   PROFILE   ---*/
    private ImageView userImage, leftThumb, leftFore, leftIndex, leftRing, leftTiny, rightThumb, rightFore, rightIndex, rightRing, rightTiny;
    private TextView username, fullName;

    private FirebaseDatabase db = FirebaseDatabase.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private DatabaseReference userRef, fingerPrintRef;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference fingerPrintImageRef, userImageRef;
    private String currentUid;
    private android.app.AlertDialog mDialog;
    private static final int IMAGE_REQUEST_CODE = 2007;
    private static final int VERIFY_PERMISSIONS_REQUEST = 17;
    private Uri imageUri;
    private String originalImageUrl, thumbDownloadUrl;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        System.loadLibrary("opencv_java3");

        /*---   FONT MANAGEMENT   ---*/
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Wigrum-Regular.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_home);


        /*---   IMAGE FACE DETECTION   ---*/
        PicassoFaceDetector.initialize(this);


        /*---   LOCAL   ---*/
        Paper.init(this);
        currentUid = Paper.book().read(Common.USER_ID);


        /*---   FIREBASE   ---*/
        userRef = db.getReference("Users");
        fingerPrintRef = db.getReference("FingerPrints");
        fingerPrintImageRef = storage.getReference("UserData");
        fingerPrintImageRef = storage.getReference("UserData");
        userImageRef = storage.getReference("UserData");


        /*---   WIDGETS   ---*/
        exitActivity = (ImageView)findViewById(R.id.exitActivity);
        help = (ImageView)findViewById(R.id.helpIcon);
        activityName = (TextView)findViewById(R.id.activityName);
        rootLayout = (RelativeLayout)findViewById(R.id.homeRootLayout);
        username = (TextView)findViewById(R.id.usernameTxt);
        fullName = (TextView)findViewById(R.id.fullNameTxt);
        userImage = (ImageView)findViewById(R.id.userImage);
        //uploadImage = (ImageView)findViewById(R.id.uploadProfilePicture);
        //leftThumb = (ImageView)findViewById(R.id.leftThumb);
        //leftFore = (ImageView)findViewById(R.id.leftForeFinger);
        //leftIndex = (ImageView)findViewById(R.id.leftIndexFinger);
        //leftRing = (ImageView)findViewById(R.id.leftRingFinger);
        //leftTiny = (ImageView)findViewById(R.id.leftTinyFinger);
        //rightThumb = (ImageView)findViewById(R.id.rightThumb);
        //rightFore = (ImageView)findViewById(R.id.rightForeFinger);
        //rightIndex = (ImageView)findViewById(R.id.rightIndexFinger);
        //rightRing = (ImageView)findViewById(R.id.rightRingFinger);
        //rightTiny = (ImageView)findViewById(R.id.rightTinyFinger);


        /*---   INITIALIZE BOTTOM SHEET   ---*/
        bottomSheetFragment = BottomSheetFragment.newInstance("Quick Help");


        /*---   TOOLBAR   ---*/
        mToolbar = (Toolbar)findViewById(R.id.generalToolbar);
        setSupportActionBar(mToolbar);


        /*---   EXIT ACTIVITY   ---*/
        exitActivity.setImageResource(R.drawable.ic_exit_app);
        exitActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PicassoFaceDetector.releaseDetector();
                finish();
            }
        });


        /*---   ACTIVITY NAME   ---*/
        activityName.setText("Home");


        /*---   HELP   ---*/
        help.setVisibility(View.GONE);


        /*---   CURRENT USER   ---*/
        userRef.child(currentUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Common.currentUser = dataSnapshot.getValue(UserModel.class);

                if (!Common.currentUser.getImageThumb().equals("")){

                    Picasso
                            .with(getBaseContext())
                            .load(Common.currentUser.getImageThumb())
                            .networkPolicy(NetworkPolicy.OFFLINE)
                            .noPlaceholder()
                            .transform(new FaceCenterCrop(400, 400))
                            .into(userImage, new Callback() {
                                @Override
                                public void onSuccess() {

                                }

                                @Override
                                public void onError() {
                                    Picasso
                                            .with(getBaseContext())
                                            .load(Common.currentUser.getImageThumb())
                                            .noPlaceholder()
                                            .transform(new FaceCenterCrop(400, 400))
                                            .into(userImage);
                                }
                            });

                    userImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent viewImage = new Intent(Home.this, ImageViewActivity.class);
                            viewImage.putExtra("ThumbUrl", Common.currentUser.getImageThumb());
                            viewImage.putExtra("ImageUrl", Common.currentUser.getImage());
                            startActivity(viewImage);
                            overridePendingTransition(R.anim.scale_in, R.anim.scale_out);
                        }
                    });

                }

                username.setText("Welcome ,  "+Common.currentUser.getUsername());
                fullName.setText("Name : "+Common.currentUser.getLastName()+" "+Common.currentUser.getFirstName());

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });


        /*---   PERMISSIONS HANDLER   ---*/
        if (checkPermissionsArray(Permissions.PERMISSIONS)){


        } else {

            verifyPermissions(Permissions.PERMISSIONS);

        }


    }

    private void verifyPermissions(String[] permissions) {

        ActivityCompat.requestPermissions(
                Home.this,
                permissions,
                VERIFY_PERMISSIONS_REQUEST
        );
    }

    private boolean checkPermissionsArray(String[] permissions) {

        for (int i = 0; i < permissions.length; i++){

            String check = permissions[i];
            if (!checkPermissions(check)){
                return false;
            }

        }
        return true;
    }

    private boolean checkPermissions(String permission) {

        int permissionRequest = ActivityCompat.checkSelfPermission(Home.this, permission);

        if (permissionRequest != PackageManager.PERMISSION_GRANTED){

            return false;
        } else {

            return true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.general_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.general_action_help) {



        }

        if (id == R.id.general_action_settings){



        }

        if (id == R.id.general_action_logout){

            Paper.book().destroy();
            Intent signoutIntent = new Intent(Home.this, Login.class);
            signoutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(signoutIntent);
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        }

        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        PicassoFaceDetector.releaseDetector();
    }

    @Override
    protected void onStop() {
        super.onStop();
        PicassoFaceDetector.releaseDetector();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PicassoFaceDetector.releaseDetector();
    }

    @Override
    public void onBackPressed() {
        PicassoFaceDetector.releaseDetector();
        finish();
    }
}
