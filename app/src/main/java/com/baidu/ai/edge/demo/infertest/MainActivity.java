package com.baidu.ai.edge.demo.infertest;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.baidu.ai.edge.core.util.FileUtil;
import com.baidu.ai.edge.demo.R;

import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // 请替换为你自己的序列号
    private static final String SERIAL_NUM = "F0B6-FCD4-B475-CAC0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_test);

        Button b = findViewById(R.id.button2);
        final Application app = getApplication();
        initPermission();
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, app.getApplicationInfo().nativeLibraryDir);
                TextView tv = findViewById(R.id.sample_text);
                try {
                    String[] dir_arr = app.getAssets().list("");
                    for (String dir : dir_arr) {
                        if (dir.equals("infer")) {
                            /* 通用ARM */

                            String configJson = FileUtil.readAssetFileUtf8String(getAssets(),
                                    "demo/config.json");
                            JSONObject jsonObject = new JSONObject(configJson);
                            int modelType = jsonObject.getInt("modelType");
                            Log.i(TAG, "Model type is " + modelType);

                            AsyncTask<Void, CharSequence, CharSequence> at = null;
                            switch (modelType) {
                                case 1:
                                    at = new TestInferClassifyTask(app, tv, SERIAL_NUM);
                                    break;
                                case 401:
                                case 2:
                                    at = new TestInferDetectionTask(app, tv, SERIAL_NUM);
                                    break;
                                case 100:
                                    at = new TestInferOcrTask(app, tv, SERIAL_NUM);
                                    break;
                                case 6:
                                    at = new TestInferSegmentTask(app, tv, SERIAL_NUM);
                                    break;
                                case 402:
                                    at = new TestInferPoseTask(app, tv, SERIAL_NUM);
                                    break;
                            }
                            if (at != null) {
                                at.execute();
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        });
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.READ_PHONE_STATE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.

            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
    }
}
