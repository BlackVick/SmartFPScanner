package com.iccsoftware.smartfpscanner;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.iccsoftware.smartfpscanner.Common.Common;

import dmax.dialog.SpotsDialog;
import io.paperdb.Paper;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SignIn extends AppCompatActivity {

    private RelativeLayout rootLayout;
    private Button signInBtn;
    private ImageView exitActivity, help;
    private TextView activityName;
    private EditText username, password;
    private FirebaseDatabase db = FirebaseDatabase.getInstance();
    private DatabaseReference userRef;
    private android.app.AlertDialog mDialog;

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

        setContentView(R.layout.activity_sign_in);


        /*---   LOCAL DB   ---*/
        Paper.init(this);


        /*---   FIREBASE   ---*/
        userRef = db.getReference("Users");


        /*---   WIDGETS   ---*/
        rootLayout = (RelativeLayout)findViewById(R.id.signInRootLayout);
        exitActivity = (ImageView)findViewById(R.id.exitActivity);
        help = (ImageView)findViewById(R.id.helpIcon);
        activityName = (TextView)findViewById(R.id.activityName);
        signInBtn = (Button)findViewById(R.id.signInButton);
        username = (EditText)findViewById(R.id.userNameSignInEdt);
        password = (EditText)findViewById(R.id.passwordSignInEdt);


        /*---   ACTIVITY NAME   ---*/
        activityName.setText("SIGN IN");


        /*---   EXIT ACTIVITY   ---*/
        exitActivity.setImageResource(R.drawable.ic_cancel_signin);
        exitActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        /*---   HELP   ---*/
        help.setVisibility(View.GONE);


        /*---   SIGN IN   ---*/
        signInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

    }

    private void signIn() {

        final String usernameTxt = username.getText().toString();
        final String passwordTxt = password.getText().toString();

        if (Common.isConnectedToInternet(getBaseContext())){

            mDialog = new SpotsDialog(SignIn.this, "Validating . . .");
            mDialog.show();

            if (TextUtils.isEmpty(usernameTxt)){

                mDialog.dismiss();
                Snackbar.make(rootLayout, "Username Can Not Be Blank !", Snackbar.LENGTH_LONG).show();

            } else if (TextUtils.isEmpty(passwordTxt)){

                mDialog.dismiss();
                Snackbar.make(rootLayout, "You Have To Provide Your Password !", Snackbar.LENGTH_LONG).show();

            } else {

                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if (dataSnapshot.child(usernameTxt).exists()){

                            final String dbPassword = dataSnapshot.child(usernameTxt).child("password").getValue().toString();

                            if (passwordTxt.equals(dbPassword)){

                                mDialog.dismiss();

                                /*---   REGISTER IN LOCAL DB   ---*/
                                Paper.book().write(Common.USER_ID, usernameTxt);


                                /*---   DESTROY STACK   ---*/
                                Login.firstActivity.finish();


                                /*---   SIGN IN   ---*/
                                Intent home = new Intent(SignIn.this, Home.class);
                                startActivity(home);
                                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                                finish();


                            } else {

                                mDialog.dismiss();
                                password.setTextColor(getResources().getColor(R.color.red));
                                Snackbar.make(rootLayout, "Password Mismatch", Snackbar.LENGTH_LONG).show();

                            }

                        } else {

                            mDialog.dismiss();
                            username.setTextColor(getResources().getColor(R.color.red));
                            Snackbar.make(rootLayout, "Invalid Username", Snackbar.LENGTH_LONG).show();

                        }

                        userRef.removeEventListener(this);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        mDialog.dismiss();
                    }
                });

            }

        } else {

            Snackbar.make(rootLayout, "No Internet Access !", Snackbar.LENGTH_LONG).show();

        }

    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
