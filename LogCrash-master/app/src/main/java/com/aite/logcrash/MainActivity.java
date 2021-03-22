package com.aite.logcrash;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.aite.logcrash.logcat.FTPManager;
import com.aite.logcrash.logcat.IFtpProgressReport;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private Button mBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtn = findViewById(R.id.throw_exception_btn);
        mBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.throw_exception_btn:
                String s = null;
                s.startsWith("hello kotlin");
                break;
        }
    }
}