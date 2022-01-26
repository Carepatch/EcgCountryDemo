package com.proton.algorithm.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.orhanobut.logger.Logger;
import com.proton.ecg.algorithm.bean.AlgorithmResult;
import com.proton.ecg.algorithm.callback.AlgorithmResultListener;
import com.proton.ecg.algorithm.callback.EcgAlgorithmListener;
import com.proton.ecg.algorithm.interfaces.impl.EcgAlgorithm;
import com.proton.ecg.algorithm.interfaces.impl.EcgPatchAlgorithm;
import com.proton.ecgpatch.connector.EcgPatchManager;
import com.proton.ecgpatch.connector.callback.DataListener;
import com.proton.view.EcgRealTimeView;
import com.wms.ble.bean.ScanResult;
import com.wms.ble.callback.OnConnectListener;
import com.wms.ble.callback.OnScanListener;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity----";
    public static final String patchMac = "CA:2F:B8:EF:BF:BE";
    private EcgAlgorithm ecgAlgorithm;
    private EcgRealTimeView realTimeView;
    private int sample = 256;//采样率

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PermissionUtils.getLocationPermission(this);
        PermissionUtils.getReadAndWritePermission(this);
        EcgPatchManager.init(this);

        realTimeView = findViewById(R.id.id_ecg_view);
        findViewById(R.id.idScanPatch).setOnClickListener(v -> {
            scan();
        });
        findViewById(R.id.idConnectPatch).setOnClickListener(v -> {
            connect();
        });
        findViewById(R.id.idAlgorithmResult).setOnClickListener(v -> {
            fetchAlgorithmResult();
        });
        findViewById(R.id.idDisconnect).setOnClickListener(v -> {
            EcgPatchManager.getInstance(patchMac).disConnect();
        });
    }

    private void scan() {
        EcgPatchManager.scanDevice(new OnScanListener() {
            @Override
            public void onDeviceFound(ScanResult scanResult) {
                super.onDeviceFound(scanResult);
                Log.d(TAG, "onDeviceFound: " + scanResult.getMacaddress());
            }
        });
    }

    private void connect() {
        realTimeView.setSample(sample);
        ecgAlgorithm = new EcgPatchAlgorithm(new EcgAlgorithmListener() {
            @Override
            public void receiveEcgFilterData(byte[] ecgData) {
                super.receiveEcgFilterData(ecgData);
                //滤波后的数据
                realTimeView.addEcgData(ecgData);
            }
        });
        EcgPatchManager ecgPatchManager = EcgPatchManager.getInstance(patchMac);
        //设置数据监听
        ecgPatchManager.setDataListener(new DataListener() {
            @Override
            public void receiveEcgRawData(byte[] bytes) {
                super.receiveEcgRawData(bytes);
                //算法滤波,滤波后的数据在算法回调里面获取 EcgAlgorithmListener
                ecgAlgorithm.processEcgData(bytes);
            }
        });
        //连接心电贴,并设置连接监听
        ecgPatchManager.connectEcgPatch(new OnConnectListener() {
            @Override
            public void onConnectSuccess(boolean isNewUUID) {
                super.onConnectSuccess(isNewUUID);
                //连接成功
            }

            @Override
            public void onDisconnect(boolean isManual) {
                super.onDisconnect(isManual);
                if (ecgAlgorithm != null) {
                    //重置算法
                    ecgAlgorithm.reset();
                }
            }
        });
    }

    /**
     * 获取算法结果
     */
    private void fetchAlgorithmResult() {
        //起始时间戳
        long startStamp = System.currentTimeMillis();
        //获取所有心电数据
        byte[] sourceEcgData = ecgAlgorithm.fetchSourceEcgData(sample);
        if (ecgAlgorithm != null) {
            ecgAlgorithm.fullAnalyse(startStamp, sourceEcgData, sample, new AlgorithmResultListener() {
                @Override
                public void receiveAlgorithmResult(AlgorithmResult result) {
                    super.receiveAlgorithmResult(result);
//                    Logger.w("result==%s", result.toString());
                    List<Integer> rPeaks = result.getrPeaks();
                    List<Long> rTime = result.getrTime();
                    List<Double> rrsList = result.getrRs();
                    List<Integer> rHrs = result.getrHr();
                    Logger.w("AlgorithmResult start----");
                    Logger.d(rrsList);
                    Logger.d(rTime);
                    Logger.d(rHrs);
                    Logger.d(rPeaks);
                    Logger.w("AlgorithmResult end----rTime size==%s,rHrs size:%s,rPeaks size:%s", rTime.size(), rHrs.size(),rPeaks.size());
                }
            });
        }
    }

}