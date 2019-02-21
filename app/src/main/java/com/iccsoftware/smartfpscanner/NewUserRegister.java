package com.iccsoftware.smartfpscanner;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.theartofdev.edmodo.cropper.CropImage;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import dmax.dialog.SpotsDialog;
import id.zelory.compressor.Compressor;
import io.paperdb.Paper;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class NewUserRegister extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    private ImageView exitActivity, help, profileImg, biometrics;
    private TextView activityName;
    private RelativeLayout rootLayout;
    private MaterialEditText username, firstName, lastName, password, confirmPassword;
    private Button signUp;
    private FirebaseDatabase db = FirebaseDatabase.getInstance();
    private DatabaseReference userRef;
    private android.app.AlertDialog mDialog;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference imageRef, imageThumbRef;
    private static final int GALLERY_REQUEST_CODE = 686;
    private static final int CAMERA_REQUEST_CODE = 456;
    private static final int VERIFY_PERMISSIONS_REQUEST = 17;
    private Uri imageUri;
    private String originalImageUrl, thumbDownloadUrl;
    Handler handler;
    Mat mRgba,mGray;
    CamView mOpenCvCameraView;
    private File cascadeFile;
    private CascadeClassifier cascadeClassifier;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:      {
                    Log.i("MainActivity", "OpenCV loaded successfully");
                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.haarhand);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        cascadeFile = new File(cascadeDir, "haarhand.xml");
                        FileOutputStream os = new FileOutputStream(cascadeFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();
                        //Initialize the Cascade Classifier object using the
                        // trained cascade file          c
                        cascadeClassifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
                        if (cascadeClassifier.empty()) {
                            Log.e("mainActivity", "Failed to load cascade classifier");
                            cascadeClassifier = null;
                        } else
                            Log.i("mainActivity", "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());
                        cascadeDir.delete();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("mainActivity", "Failed to load cascade. Exception thrown: " + e);
                    }
                    mOpenCvCameraView.enableView();
                }
                break;
                default:      {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };


    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(OpenCVLoader.initDebug()){
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }else {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this,mLoaderCallback);
        }
    }

    int mAbsoluteFaceSize =0,mRelativeFaceSize=100;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Core.flip(inputFrame.rgba(), mRgba, 1);
        Core.flip(inputFrame.gray(),mGray,1);
        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }
        MatOfRect closedHands = new MatOfRect();
        if (cascadeClassifier != null)
            cascadeClassifier.detectMultiScale(mGray, closedHands, 1.1, 2, 2,new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size(0,0));
        Rect[] facesArray = closedHands.toArray();
        Map<Integer,Integer> rectBuckts = new HashMap();Map<Integer, Rect> rectCue = new HashMap();
        for (int i = 0; i < facesArray.length; i++)  {
            Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);
            Point quatnizedTL=new Point(((int)(facesArray[i].tl().x/100))*100, ((int)(facesArray[i].tl().y/100))*100);
            Point quatnizedBR=new Point(((int)(facesArray[i].br().x/100))*100, ((int)(facesArray[i].br().y/100))*100);
            int bucktID=quatnizedTL.hashCode()+quatnizedBR.hashCode()*2;
            if(rectBuckts.containsKey(bucktID))    {
                rectBuckts.put(bucktID, (rectBuckts.get(bucktID)+1));
                rectCue.put(bucktID, new Rect(quatnizedTL,quatnizedBR));
            }    else    {
                rectBuckts.put(bucktID, 1);
            }
        }
        int maxDetections=0;
        int maxDetectionsKey=0;
        for(Map.Entry<Integer,Integer> e : rectBuckts.entrySet())  {
            if(e.getValue()>maxDetections)    {
                maxDetections=e.getValue();
                maxDetectionsKey=e.getKey();
            }
        }
        if(maxDetections>5)  {
            Imgproc.rectangle(mRgba, rectCue.get(maxDetectionsKey).tl(), rectCue.get(maxDetectionsKey).br(), new Scalar(0, 0, 0, 255), 3);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String currentDateandTime = sdf.format(new Date());
            String fileName = Environment.getExternalStorageDirectory().getPath() + "/sample_picture_" + currentDateandTime + ".jpg";
            mOpenCvCameraView.takePicture(fileName);
            Message msg = handler.obtainMessage();
            msg.arg1 = 1;
            Bundle b=new Bundle();
            b.putString("msg", fileName + " saved");
            msg.setData(b);
            handler.sendMessage(msg);
            rectBuckts.clear();
        }
        return mRgba;

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        /*---     STARTING OPENCV CAMERA ---*/
        mOpenCvCameraView = (CamView) findViewById(R.id.auto_selfie_activity_surface_view);
        mOpenCvCameraView.setCameraIndex(1);
        mOpenCvCameraView.setCvCameraViewListener(this);
        handler = new Handler();

        /*---   FONT MANAGEMENT   ---*/
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Wigrum-Regular.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_new_user_register);


        /*---   LOCAL   ---*/
        Paper.init(this);


        /*---   FIREBASE   ---*/
        userRef = db.getReference("Users");
        imageRef = storage.getReference("Users").child("ProfileImages");
        imageThumbRef = storage.getReference("Users").child("ProfileImages");

        /*---   WIDGETS   ---*/
        exitActivity = (ImageView)findViewById(R.id.exitActivity);
        help = (ImageView)findViewById(R.id.helpIcon);
        profileImg = (ImageView)findViewById(R.id.regProfilePic);
        biometrics = (ImageView)findViewById(R.id.regFingerPrints);
        activityName = (TextView)findViewById(R.id.activityName);
        rootLayout = (RelativeLayout)findViewById(R.id.newUserRootLayout);
        username = (MaterialEditText)findViewById(R.id.usernameRegEdt);
        firstName = (MaterialEditText)findViewById(R.id.userFirstNameRegEdt);
        lastName = (MaterialEditText)findViewById(R.id.userLastNameRegEdt);
        password = (MaterialEditText)findViewById(R.id.userPasswordRegEdt);
        confirmPassword = (MaterialEditText)findViewById(R.id.userPasswordConfirmRegEdt);
        signUp = (Button)findViewById(R.id.registerBtn);


        /*---   EXIT ACTIVITY   ---*/
        exitActivity.setImageResource(R.drawable.ic_cancel_signin);
        exitActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        /*---   ACTIVITY NAME   ---*/
        activityName.setText("SIGN UP");


        /*---   HELP   ---*/
        help.setVisibility(View.GONE);


        /*---   PERMISSIONS HANDLER   ---*/
        if (checkPermissionsArray(Permissions.PERMISSIONS)){


        } else {

            verifyPermissions(Permissions.PERMISSIONS);

        }


        /*---   PROFILE IMAGE   ---*/
        profileImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(username.getText().toString())){

                    Snackbar.make(rootLayout, "Please Pick A UserName", Snackbar.LENGTH_LONG).show();

                } else {

                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            if (!dataSnapshot.child(username.getText().toString()).exists()){

                                openChoiceDialog();

                            } else {

                                Snackbar.make(rootLayout, "Username Unavailable, Pick Another", Snackbar.LENGTH_LONG).show();

                            }

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                }

            }
        });


        /*---   BIOMETRICS   ---*/
        biometrics.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBiometricDialog();
            }
        });


        /*---   SIGN UP   ---*/
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Common.isConnectedToInternet(getBaseContext())){

                    signUpUser();

                } else {

                    Snackbar.make(rootLayout, "No Internet Access !", Snackbar.LENGTH_LONG).show();

                }
            }
        });
    }

    private void openBiometricDialog() {

        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        final LayoutInflater inflater = this.getLayoutInflater();
        View viewOptions = inflater.inflate(R.layout.biometrics_scan_choice,null);

        final ImageView leftFour = (ImageView) viewOptions.findViewById(R.id.leftFourFingers);
        final ImageView rightFour = (ImageView) viewOptions.findViewById(R.id.rightFourFingers);
        final ImageView leftThumb = (ImageView) viewOptions.findViewById(R.id.leftThumb);
        final ImageView rightThumb = (ImageView) viewOptions.findViewById(R.id.rightThumb);


        alertDialog.getWindow().getAttributes().windowAnimations = R.style.PauseDialogAnimation;

        alertDialog.getWindow().setGravity(Gravity.BOTTOM);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


        WindowManager.LayoutParams layoutParams = alertDialog.getWindow().getAttributes();
        //layoutParams.x = 100; // left margin
        layoutParams.y = 100; // bottom margin
        alertDialog.getWindow().setAttributes(layoutParams);



        leftFour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mOpenCvCameraView.enableView();

                View viewOptions = inflater.inflate(R.layout.activity_main,null);
                final Button takePicture = (Button) viewOptions.findViewById(R.id.take_picture);

                takePicture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        mOpenCvCameraView.takePicture("fn");

                    }
                });
                Snackbar.make(rootLayout, "Left Four !", Snackbar.LENGTH_LONG).show();
                alertDialog.dismiss();

            }
        });

        rightFour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOpenCvCameraView.enableView();

                View viewOptions = inflater.inflate(R.layout.activity_main,null);
                final Button takePicture = (Button) viewOptions.findViewById(R.id.take_picture);

                takePicture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        mOpenCvCameraView.takePicture("fn");

                    }
                });

                Snackbar.make(rootLayout, "Right Four !", Snackbar.LENGTH_LONG).show();
                alertDialog.dismiss();
            }
        });

        leftThumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mOpenCvCameraView.enableView();

                View viewOptions = inflater.inflate(R.layout.activity_main,null);
                final Button takePicture = (Button) viewOptions.findViewById(R.id.take_picture);

                takePicture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        mOpenCvCameraView.takePicture("fn");

                    }
                });
                Snackbar.make(rootLayout, "Left Thumbs !", Snackbar.LENGTH_LONG).show();
                alertDialog.dismiss();

            }
        });

        rightThumb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mOpenCvCameraView.enableView();

                View viewOptions = inflater.inflate(R.layout.activity_main,null);
                final Button takePicture = (Button) viewOptions.findViewById(R.id.take_picture);

                takePicture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        mOpenCvCameraView.takePicture("fn");

                    }
                });
                Snackbar.make(rootLayout, "Right Thumbs !", Snackbar.LENGTH_LONG).show();
                alertDialog.dismiss();

            }
        });
        alertDialog.show();

    }

    private void signUpUser() {

        final String usernameTxt = username.getText().toString();
        final String firstNameTxt = firstName.getText().toString();
        final String lastNameTxt = lastName.getText().toString();
        final String passwordText = password.getText().toString();
        final String confirmPasswordText = confirmPassword.getText().toString();

        mDialog = new SpotsDialog(NewUserRegister.this, "Validating . . .");
        mDialog.show();

        if (TextUtils.isEmpty(usernameTxt)){

            mDialog.dismiss();
            Snackbar.make(rootLayout, "Please Pick A UserName", Snackbar.LENGTH_LONG).show();

        } else if (TextUtils.isEmpty(firstNameTxt)){

            mDialog.dismiss();
            Snackbar.make(rootLayout, "Your First Name Can Not Be Left Empty \ud83d\ude01" , Snackbar.LENGTH_LONG).show();

        } else if (TextUtils.isEmpty(lastNameTxt)){

            mDialog.dismiss();
            Snackbar.make(rootLayout, "Your Last Name Can Not Be Left Empty \ud83d\ude01", Snackbar.LENGTH_LONG).show();

        } else if (passwordText.length() < 6){

            mDialog.dismiss();
            Snackbar.make(rootLayout, "Passwords Should Not Be Less Than 6 Characters", Snackbar.LENGTH_LONG).show();

        } else if (!passwordText.equals(confirmPasswordText)){

            mDialog.dismiss();
            Snackbar.make(rootLayout, "Password Do Not Match !", Snackbar.LENGTH_LONG).show();

        } else if (imageUri == null){

            mDialog.dismiss();
            Snackbar.make(rootLayout, "A Profile Picture Is Required !", Snackbar.LENGTH_LONG).show();

        } else {

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.child(usernameTxt).exists()){

                        username.setTextColor(getResources().getColor(R.color.proceed));


                        /*---   PUSH TO FIREBASE   ---*/
                        UserModel newUser = new UserModel(usernameTxt, firstNameTxt, lastNameTxt, passwordText, originalImageUrl, thumbDownloadUrl);
                        userRef.child(usernameTxt).setValue(newUser);


                        mDialog.dismiss();


                        /*---   WRITE TO LOCAL DB   ---*/
                        Paper.book().write(Common.USER_ID, usernameTxt);


                        /*---   DESTROY STACK   ---*/
                        Login.firstActivity.finish();


                        /*---   SIGN IN   ---*/
                        Intent home = new Intent(NewUserRegister.this, Home.class);
                        startActivity(home);
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        finish();

                    } else {

                        mDialog.dismiss();
                        Snackbar.make(rootLayout, "Username Unavailable, Pick Another", Snackbar.LENGTH_LONG).show();
                        username.setTextColor(getResources().getColor(R.color.red));

                    }
                    userRef.removeEventListener(this);
                }


                @Override
                public void onCancelled(DatabaseError databaseError) {
                    mDialog.dismiss();
                }
            });

        }

    }

    private void openChoiceDialog() {

        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        LayoutInflater inflater = this.getLayoutInflater();
        View viewOptions = inflater.inflate(R.layout.image_source_choice,null);

        final ImageView cameraPick = (ImageView) viewOptions.findViewById(R.id.cameraPick);
        final ImageView galleryPick = (ImageView) viewOptions.findViewById(R.id.galleryPick);

        alertDialog.setView(viewOptions);

        alertDialog.getWindow().getAttributes().windowAnimations = R.style.PauseDialogAnimation;

        alertDialog.getWindow().setGravity(Gravity.BOTTOM);
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


        WindowManager.LayoutParams layoutParams = alertDialog.getWindow().getAttributes();
        //layoutParams.x = 100; // left margin
        layoutParams.y = 100; // bottom margin
        alertDialog.getWindow().setAttributes(layoutParams);



        cameraPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (Common.isConnectedToInternet(NewUserRegister.this)){


                    openCamera();

                }else {

                    Snackbar.make(rootLayout, "No Internet Access !", Snackbar.LENGTH_LONG).show();
                }
                alertDialog.dismiss();

            }
        });

        galleryPick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Common.isConnectedToInternet(NewUserRegister.this)){

                    openGallery();

                }else {

                    Snackbar.make(rootLayout, "No Internet Access !", Snackbar.LENGTH_LONG).show();
                }
                alertDialog.dismiss();
            }
        });
        alertDialog.show();

    }

    private void openCamera() {

        final long date = System.currentTimeMillis();
        final String dateShitFmt = String.valueOf(date);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File file=getOutputMediaFile(1);
        imageUri = FileProvider.getUriForFile(
                NewUserRegister.this,
                BuildConfig.APPLICATION_ID + ".provider",
                file);
        //imageUri = Uri.fromFile(file);

        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);

    }

    private void openGallery() {

        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto , GALLERY_REQUEST_CODE);

    }

    private void verifyPermissions(String[] permissions) {

        ActivityCompat.requestPermissions(
                NewUserRegister.this,
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

        int permissionRequest = ActivityCompat.checkSelfPermission(NewUserRegister.this, permission);

        if (permissionRequest != PackageManager.PERMISSION_GRANTED){

            return false;
        } else {

            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK){

            //Uri theUri = imageUri;
            CropImage.activity(imageUri)
                    .start(this);

        }

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK){

            if (data.getData() != null) {
                imageUri = data.getData();

                CropImage.activity(imageUri)
                        .start(this);
            }

        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                mDialog = new SpotsDialog(NewUserRegister.this, "Upload In Progress . . .");
                mDialog.show();

                Uri resultUri = result.getUri();
                String imgURI = resultUri.toString();
                setImage(imgURI, profileImg);

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String userNameForImg = username.getText().toString();

                File thumb_filepath = new File(resultUri.getPath());

                try {
                    Bitmap thumb_bitmap = new Compressor(this)
                            .setMaxWidth(300)
                            .setMaxHeight(300)
                            .setQuality(60)
                            .compressToBitmap(thumb_filepath);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thumb_bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
                    final byte[] thumb_byte = baos.toByteArray();

                    final StorageReference imageRef1 = imageRef.child(userNameForImg).child("FullImages").child("IMG_"+ timeStamp + ".png");

                    final StorageReference imageThumbRef1 = imageThumbRef.child(userNameForImg).child("Thumbnails").child("IMG_"+ timeStamp + ".png");

                    imageRef1.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()) {

                                originalImageUrl = task.getResult().getDownloadUrl().toString();
                                UploadTask uploadTask = imageThumbRef1.putBytes(thumb_byte);

                                uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> thumb_task) {

                                        thumbDownloadUrl = thumb_task.getResult().getDownloadUrl().toString();

                                        if (thumb_task.isSuccessful()){


                                            mDialog.dismiss();
                                            Snackbar.make(rootLayout, "Upload Completed", Snackbar.LENGTH_LONG).show();

                                        } else {
                                            mDialog.dismiss();
                                            Snackbar.make(rootLayout, "Error Occurred While Uploading", Snackbar.LENGTH_LONG).show();
                                        }
                                    }
                                });



                            } else {

                                mDialog.dismiss();
                                Snackbar.make(rootLayout, "Error Uploading", Snackbar.LENGTH_LONG).show();

                            }
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }




            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    private  File getOutputMediaFile(int type){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Identity Check");

        /*---   CREATE DIRECTORY IF NULL   ---*/
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }

        /*---   CREATE FILE NAME   ---*/
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == 1){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".png");
        } else {
            return null;
        }

        return mediaFile;
    }

    private void setImage(String imgUrl, ImageView image){

        ImageLoader loader = ImageLoader.getInstance();

        loader.init(ImageLoaderConfiguration.createDefault(NewUserRegister.this));

        loader.displayImage(imgUrl, image, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {

            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {

            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {

            }
        });

    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
