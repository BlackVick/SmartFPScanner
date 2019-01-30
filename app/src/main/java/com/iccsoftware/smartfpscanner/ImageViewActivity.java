package com.iccsoftware.smartfpscanner;

import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.iccsoftware.smartfpscanner.Common.Common;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ImageViewActivity extends AppCompatActivity {

    private ImageView image;
    private String thumbUrl, imageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);


        /*---   LOCAL INTENT DATA   ---*/
        thumbUrl = getIntent().getStringExtra("ThumbUrl");
        imageUrl = getIntent().getStringExtra("ImageUrl");


        /*---   WIDGET   ---*/
        image = (ImageView)findViewById(R.id.imageViewer);


        if (Common.isConnectedToInternet(getBaseContext())){

            if (imageUrl != null || thumbUrl != null){

                Picasso.with(getBaseContext())
                        .load(thumbUrl) // thumbnail url goes here
                        .into(image, new Callback() {
                            @Override
                            public void onSuccess() {
                                Picasso.with(getBaseContext())
                                        .load(imageUrl) // image url goes here
                                        .placeholder(image.getDrawable())
                                        .into(image);
                            }
                            @Override
                            public void onError() {

                            }
                        });

            } else {

                finish();
                overridePendingTransition(R.anim.scale_out, R.anim.scale_out);

            }

        } else {

            finish();

        }
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.scale_out, R.anim.scale_out);
    }
}
