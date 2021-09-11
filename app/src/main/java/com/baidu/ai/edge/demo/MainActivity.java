package com.baidu.ai.edge.demo;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.ai.edge.core.base.CallException;
import com.baidu.ai.edge.core.base.Consts;
import com.baidu.ai.edge.core.infer.InferManager;
import com.baidu.ai.edge.core.util.FileUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private Button startUIActivityBtn;
    private String name = "";
    private String version = "";
    private String ak;
    private String sk;
    private String apiUrl;
    private String soc;
    private ArrayList<String> socList = new ArrayList<>();
    private int type;

    // 请替换为您的序列号
    private static final String SERIAL_NUM = "F0B6-FCD4-B475-CAC0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermission();
        final AlertDialog.Builder agreementDialog = new AlertDialog.Builder(this)
                .setTitle("允许“百度EasyDL”使用数据？")
                .setMessage("可能同时包含无线局域网和蜂窝移动数据")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("允许", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences sp = getSharedPreferences("demo_auth_info",
                                Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putBoolean("isAgree", true);
                        editor.commit();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                startUICameraActivity();
                            }
                        }).start();
                        dialog.cancel();
                    }
                })
                .setNegativeButton("不允许", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        initConfig();

        TextView modelNameText = findViewById(R.id.model_text);
        modelNameText.setText(name);

        startUIActivityBtn = findViewById(R.id.start_ui_activity);
        startUIActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sp = getSharedPreferences("demo_auth_info", Context.MODE_PRIVATE);
                boolean hasAgree = sp.getBoolean("isAgree", false);
                boolean checkChip = checkChip();
                if (hasAgree) {
                    Log.i(this.getClass().getSimpleName(), "socList:" + socList.toString()
                            + ", Build.HARDWARE is :" + Build.HARDWARE + "soc:" + soc);
                    if (checkChip) {
                        startUICameraActivity();
                    } else {
                        Toast.makeText(getApplicationContext(), "soc not supported, socList:" + socList.toString()
                                        + ", Build.HARDWARE is :" + Build.HARDWARE,
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    agreementDialog.show();
                }
            }
        });
    }

    private boolean checkChip() {
        if (socList.contains(Consts.SOC_DSP) && Build.HARDWARE.equalsIgnoreCase("qcom")) {
            soc = Consts.SOC_DSP;
            return true;
        }
        if (socList.contains(Consts.SOC_ADRENO_GPU) && Build.HARDWARE.equalsIgnoreCase("qcom")) {
            soc = Consts.SOC_ADRENO_GPU;
            return true;
        }
        if (socList.contains(Consts.SOC_NPU)) {
            if (Build.HARDWARE.contains("kirin970")) {
                soc = "npu150";
                return true;
            }
            if (Build.HARDWARE.contains("kirin980")) {
                soc = "npu200";
                return true;
            }
        }
        if (socList.contains(Consts.SOC_NPU_VINCI) && (Build.HARDWARE.contains("kirin810")
                || Build.HARDWARE.contains("kirin820") || Build.HARDWARE.contains("kirin990"))) {
            soc = Consts.SOC_NPU_VINCI;
            return true;
        }
        if (socList.contains(Consts.SOC_ARM_GPU)) {
            try {
                if (InferManager.isSupportOpencl()) {
                    soc = Consts.SOC_ARM_GPU;
                    return true;
                }
            } catch (CallException e) {
                Toast.makeText(getApplicationContext(), e.getErrorCode() + ", " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
        if (socList.contains(Consts.SOC_ARM)) {
            soc = Consts.SOC_ARM;
            return true;
        }
        if (socList.contains(Consts.SOC_XEYE)) {
            soc = Consts.SOC_XEYE;
            return true;
        }
        return false;
    }

    private void startUICameraActivity() {
        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("model_type", type);
        intent.putExtra("serial_num", SERIAL_NUM);

        if (!apiUrl.equals("null")) {
            intent.putExtra("apiUrl", apiUrl);
            intent.putExtra("ak", ak);
            intent.putExtra("sk", sk);
        }

        intent.putExtra("soc", soc);
        startActivityForResult(intent, 1);
    }

    /**
     * 读取json配置
     */
    private void initConfig() {
        try {
            String configJson = FileUtil.readAssetFileUtf8String(getAssets(), "demo/config.json");
            JSONObject jsonObject = new JSONObject(configJson);
            name = jsonObject.getString("modelName");
            type = jsonObject.getInt("modelType");

            if (jsonObject.has("apiUrl")) {
                apiUrl = jsonObject.getString("apiUrl");
                ak = jsonObject.getString("ak");
                sk = jsonObject.getString("sk");
            }

            String str = jsonObject.getString("soc");
            String[] socs = str.split(",");
            socList.addAll(Arrays.asList(socs));
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CAMERA
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
