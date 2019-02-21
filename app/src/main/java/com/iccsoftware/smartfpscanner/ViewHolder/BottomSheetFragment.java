package com.iccsoftware.smartfpscanner.ViewHolder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.iccsoftware.smartfpscanner.R;

/**
 * Created by Scarecrow on 8/6/2018.
 */

public class BottomSheetFragment extends BottomSheetDialogFragment {

    String mTag;

    public static BottomSheetFragment newInstance(String tag){

        BottomSheetFragment f = new BottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("TAG", tag);
        f.setArguments(args);
        return f;

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTag = getArguments().getString("TAG");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_help_sheet, container, false);
        return view;
    }
}
