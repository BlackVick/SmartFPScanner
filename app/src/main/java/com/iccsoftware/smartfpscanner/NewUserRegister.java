package com.iccsoftware.smartfpscanner;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.iccsoftware.smartfpscanner.Common.Common;
import com.iccsoftware.smartfpscanner.Model.UserModel;
import com.rengwuxian.materialedittext.MaterialEditText;

import io.paperdb.Paper;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class NewUserRegister extends AppCompatActivity {

    private ImageView exitActivity, help;
    private TextView activityName;
    private RelativeLayout rootLayout;
    private MaterialEditText username, firstName, lastName, eMail;
    private Button signUp;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseDatabase db = FirebaseDatabase.getInstance();
    private DatabaseReference userRef;
    private String currentUid;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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


        /*---   CURRENT USER   ---*/
        if (mAuth.getCurrentUser() != null)
            currentUid = mAuth.getCurrentUser().getUid();



        /*---   WIDGETS   ---*/
        exitActivity = (ImageView)findViewById(R.id.exitActivity);
        help = (ImageView)findViewById(R.id.helpIcon);
        activityName = (TextView)findViewById(R.id.activityName);
        rootLayout = (RelativeLayout)findViewById(R.id.newUserRootLayout);
        username = (MaterialEditText)findViewById(R.id.usernameRegEdt);
        firstName = (MaterialEditText)findViewById(R.id.userFirstNameRegEdt);
        lastName = (MaterialEditText)findViewById(R.id.userLastNameRegEdt);
        eMail = (MaterialEditText)findViewById(R.id.userEmailRegEdt);
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
        activityName.setText("Sign Up");


        /*---   HELP   ---*/
        help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar.make(rootLayout, "Help Function Still Under Development", Snackbar.LENGTH_LONG).show();
            }
        });


        /*---   EMAIL   ---*/
        if (mAuth.getCurrentUser() != null)
            eMail.setText(mAuth.getCurrentUser().getEmail());
        eMail.setEnabled(false);


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

    private void signUpUser() {

        final String usernameTxt = username.getText().toString();
        final String firstNameTxt = firstName.getText().toString();
        final String lastNameTxt = lastName.getText().toString();
        final String emailTxt = eMail.getText().toString();


        if (TextUtils.isEmpty(usernameTxt)){

            Snackbar.make(rootLayout, "Please Pick A UserName", Snackbar.LENGTH_LONG).show();

        } else if (TextUtils.isEmpty(firstNameTxt)){

            Snackbar.make(rootLayout, "Your First Name Can Not Be Left Empty \ud83d\ude01" , Snackbar.LENGTH_LONG).show();

        } else if (TextUtils.isEmpty(lastNameTxt)){

            Snackbar.make(rootLayout, "Your Last Name Can Not Be Left Empty \ud83d\ude01", Snackbar.LENGTH_LONG).show();

        } else if (TextUtils.isEmpty(emailTxt)){

            Snackbar.make(rootLayout, "How Did You Get Here Without An Email?", Snackbar.LENGTH_LONG).show();

        } else {

            userRef.orderByChild("username").equalTo(usernameTxt).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()){

                        username.setTextColor(getResources().getColor(R.color.proceed));
                        UserModel newUser = new UserModel(usernameTxt, firstNameTxt, lastNameTxt, emailTxt, "", "");
                        if (mAuth.getCurrentUser() != null)
                            currentUid = mAuth.getCurrentUser().getUid();

                        userRef.child(currentUid).setValue(newUser).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                currentUid = mAuth.getCurrentUser().getUid();
                                Paper.book().write(Common.USER_ID, currentUid);

                                Intent goToHome = new Intent(NewUserRegister.this, Home.class);
                                startActivity(goToHome);
                                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                finish();
                            }
                        });

                    } else {

                        Snackbar.make(rootLayout, "Username Already Taken, Please Pick Another", Snackbar.LENGTH_LONG).show();
                        username.setTextColor(getResources().getColor(R.color.red));

                    }
                    userRef.removeEventListener(this);
                }


                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }

    }
}
