package com.iccsoftware.smartfpscanner;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.iccsoftware.smartfpscanner.Common.Common;
import com.jcminarro.roundkornerlayout.RoundKornerRelativeLayout;

import io.paperdb.Paper;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class Login extends AppCompatActivity {

    private RelativeLayout rootLayout;
    private Button google;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static final int GOOGLE_SIGN_IN = 26;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseDatabase db = FirebaseDatabase.getInstance();
    private DatabaseReference userRef, authed;
    private static final int TIME_FOR_BUTTONS_REVEAL = 2500;

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

        setContentView(R.layout.activity_login);


        /*---   LOCAL DB   ---*/
        Paper.init(this);


        /*---   FIREBASE   ---*/
        userRef = db.getReference("Users");
        authed = db.getReference("AuthedUsers");


        /*---   WIDGETS   ---*/
        rootLayout = (RelativeLayout)findViewById(R.id.loginRootLayout);
        google = (Button)findViewById(R.id.signInWithGoogle);


        /*---   BUTTONS REVEAL ANIM   ---*/
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                google.setVisibility(View.VISIBLE);
            }
        }, TIME_FOR_BUTTONS_REVEAL);


        /*---   GOOGLE API INITIALIZATION   ---*/
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();


        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Snackbar.make(rootLayout, "Unknown Error Occurred", Snackbar.LENGTH_LONG).show();
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
                if (Common.isConnectedToInternet(getBaseContext())) {
                    
                    google.setEnabled(false);
                    signInWithGoogle();
                    
                } else {
                    
                    Snackbar.make(rootLayout, "No Internet Access", Snackbar.LENGTH_SHORT).show();
                    
                }

            }
        });
    }

    private void signInWithGoogle() {

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, GOOGLE_SIGN_IN);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == GOOGLE_SIGN_IN) {

            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

            if (result.isSuccess()){

                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);

            } else {

                google.setEnabled(true);
                Snackbar.make(rootLayout, "Google Sign In Failed", Snackbar.LENGTH_LONG).show();

            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {

        Log.d("LOGIN", "firebaseAuthWithGoogle:" + account.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                authed.child(uid).setValue("true");

                                FirebaseUser user = mAuth.getCurrentUser();
                                updateUI(user);

                            }

                        } else {

                            Snackbar.make(rootLayout, "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });

    }

    private void updateUI(FirebaseUser user) {

        if (FirebaseAuth.getInstance().getCurrentUser() !=  null){

            final String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            userRef.child(currentUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()){

                        Paper.book().write(Common.USER_ID, currentUid);
                        Intent goToHome = new Intent(Login.this, Home.class);
                        startActivity(goToHome);
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        finish();

                    } else {

                        Intent newGoogleUser = new Intent(Login.this, NewUserRegister.class);
                        startActivity(newGoogleUser);
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        finish();

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            });

        }

    }

    @Override
    public void onStart() {
        super.onStart();

        final String localUser = Paper.book().read(Common.USER_ID);
        if (localUser != null){

            if (!localUser.isEmpty()){

                Intent goToHome = new Intent(Login.this, Home.class);
                startActivity(goToHome);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();

            }

        }
    }
}
