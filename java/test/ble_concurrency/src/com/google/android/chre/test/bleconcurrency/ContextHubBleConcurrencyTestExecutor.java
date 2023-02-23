/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.chre.test.bleconcurrency;

import static com.google.common.truth.Truth.assertThat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.hardware.location.ContextHubClient;
import android.hardware.location.ContextHubClientCallback;
import android.hardware.location.ContextHubInfo;
import android.hardware.location.ContextHubManager;
import android.hardware.location.NanoAppBinary;

import androidx.test.InstrumentationRegistry;

import com.google.android.chre.utils.pigweed.ChreRpcClient;
import com.google.android.utils.chre.ChreApiTestUtil;
import com.google.android.utils.chre.ChreTestUtil;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;

import org.junit.Assert;

import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.chre.rpc.proto.ChreApiTest;
import dev.pigweed.pw_rpc.Service;

/**
 * A class that can execute the CHRE BLE concurrency test.
 */
public class ContextHubBleConcurrencyTestExecutor extends ContextHubClientCallback {
    private static final String TAG = "ContextHubBleConcurrencyTestExecutor";

    /**
     * The delay to report results in milliseconds.
     */
    private static final int REPORT_DELAY_MS = 0;

    /**
     * The RSSI threshold for the BLE scan filter.
     */
    private static final int RSSI_THRESHOLD = -128;

    /**
     * The advertisement type for service data.
     */
    private static final int CHRE_BLE_AD_TYPE_SERVICE_DATA_WITH_UUID_16 = 0x16;

    private final BluetoothLeScanner mBluetoothLeScanner;
    private final Context mContext = InstrumentationRegistry.getTargetContext();
    private final ContextHubInfo mContextHub;
    private final ContextHubClient mContextHubClient;
    private final ContextHubManager mContextHubManager;
    private final AtomicBoolean mChreReset = new AtomicBoolean(false);
    private final NanoAppBinary mNanoAppBinary;
    private final long mNanoAppId;
    private final ChreRpcClient mRpcClient;

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            // do nothing
        }

        @Override
        public void onScanFailed(int errorCode) {
            Assert.fail("Failed to start a BLE scan on the host");
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // do nothing
        }
    };

    public ContextHubBleConcurrencyTestExecutor(NanoAppBinary nanoapp) {
        mNanoAppBinary = nanoapp;
        mNanoAppId = nanoapp.getNanoAppId();
        mContextHubManager = mContext.getSystemService(ContextHubManager.class);
        assertThat(mContextHubManager).isNotNull();
        List<ContextHubInfo> contextHubs = mContextHubManager.getContextHubs();
        assertThat(contextHubs.size() > 0).isTrue();
        mContextHub = contextHubs.get(0);
        mContextHubClient = mContextHubManager.createClient(mContextHub, /* callback= */ this);
        Service chreApiService = ChreApiTestUtil.getChreApiService();
        mRpcClient = new ChreRpcClient(mContextHubManager, mContextHub, mNanoAppId,
                List.of(chreApiService), /* callback= */ this);

        BluetoothManager bluetoothManager = mContext.getSystemService(BluetoothManager.class);
        assertThat(bluetoothManager).isNotNull();
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        assertThat(bluetoothAdapter).isNotNull();
        mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        assertThat(mBluetoothLeScanner).isNotNull();
    }

    @Override
    public void onHubReset(ContextHubClient client) {
        mChreReset.set(true);
    }

    /**
     * Should be invoked before run() is invoked to set up the test, e.g. in a @Before method.
     */
    public void init() {
        mContextHubManager.enableTestMode();
        ChreTestUtil.loadNanoAppAssertSuccess(mContextHubManager, mContextHub, mNanoAppBinary);
    }

    /**
     * Runs the test.
     */
    public void run() throws Exception {
        testHostScanFirst();
        Thread.sleep(1000);
        testChreScanFirst();
    }

    /**
     * Cleans up the test, should be invoked in e.g. @After method.
     */
    public void deinit() {
        if (mChreReset.get()) {
            Assert.fail("CHRE reset during the test");
        }

        ChreTestUtil.unloadNanoAppAssertSuccess(mContextHubManager, mContextHub, mNanoAppId);
        mContextHubManager.disableTestMode();
        mContextHubClient.close();
    }

    /**
     * Generates a BLE scan filter that filters only for the known Google beacons:
     * Google Eddystone and Nearby Fastpair.
     */
    private static ChreApiTest.ChreBleScanFilter getDefaultScanFilter() {
        ChreApiTest.ChreBleGenericFilter eddystoneFilter =
                ChreApiTest.ChreBleGenericFilter.newBuilder()
                        .setType(CHRE_BLE_AD_TYPE_SERVICE_DATA_WITH_UUID_16)
                        .setLength(2)
                        .setData(ByteString.copyFrom(HexFormat.of().parseHex("FEAA")))
                        .setMask(ByteString.copyFrom(HexFormat.of().parseHex("FFFF")))
                        .build();
        ChreApiTest.ChreBleGenericFilter nearbyFastpairFilter =
                ChreApiTest.ChreBleGenericFilter.newBuilder()
                        .setType(CHRE_BLE_AD_TYPE_SERVICE_DATA_WITH_UUID_16)
                        .setLength(2)
                        .setData(ByteString.copyFrom(HexFormat.of().parseHex("FE2C")))
                        .setMask(ByteString.copyFrom(HexFormat.of().parseHex("FFFF")))
                        .build();

        return ChreApiTest.ChreBleScanFilter.newBuilder()
                .setRssiThreshold(RSSI_THRESHOLD)
                .setScanFilterCount(2)
                .addScanFilters(eddystoneFilter)
                .addScanFilters(nearbyFastpairFilter)
                .build();
    }

    /**
     * Generates a BLE scan filter that filters only for the known Google beacons:
     * Google Eddystone and Nearby Fastpair. We specify the filter data in (little-endian) LE
     * here as the CHRE code will take BE input and transform it to LE.
     */
    private static List<ScanFilter> getDefaultScanFilterHost() {
        assertThat(CHRE_BLE_AD_TYPE_SERVICE_DATA_WITH_UUID_16)
                .isEqualTo(ScanRecord.DATA_TYPE_SERVICE_DATA_16_BIT);

        ScanFilter scanFilter = new ScanFilter.Builder()
                .setAdvertisingDataTypeWithData(
                        ScanRecord.DATA_TYPE_SERVICE_DATA_16_BIT,
                        ByteString.copyFrom(HexFormat.of().parseHex("AAFE")).toByteArray(),
                        ByteString.copyFrom(HexFormat.of().parseHex("FFFF")).toByteArray())
                .build();
        ScanFilter scanFilter2 = new ScanFilter.Builder()
                .setAdvertisingDataTypeWithData(
                        ScanRecord.DATA_TYPE_SERVICE_DATA_16_BIT,
                        ByteString.copyFrom(HexFormat.of().parseHex("2CFE")).toByteArray(),
                        ByteString.copyFrom(HexFormat.of().parseHex("FFFF")).toByteArray())
                .build();

        return ImmutableList.of(scanFilter, scanFilter2);
    }

    /**
     * Starts a BLE scan and asserts it was started successfully in a synchronous manner.
     * This waits for the event to be received and returns the status in the event.
     *
     * @param scanFilter                The scan filter.
     */
    private void chreBleStartScanSync(ChreApiTest.ChreBleScanFilter scanFilter) throws Exception {
        ChreApiTest.ChreBleStartScanAsyncInput.Builder inputBuilder =
                ChreApiTest.ChreBleStartScanAsyncInput.newBuilder()
                        .setMode(ChreApiTest.ChreBleScanMode.CHRE_BLE_SCAN_MODE_FOREGROUND)
                        .setReportDelayMs(REPORT_DELAY_MS)
                        .setHasFilter(scanFilter != null);
        if (scanFilter != null) {
            inputBuilder.setFilter(scanFilter);
        }

        ChreApiTestUtil util = new ChreApiTestUtil();
        List<ChreApiTest.GeneralSyncMessage> response =
                util.callServerStreamingRpcMethodSync(mRpcClient,
                        "chre.rpc.ChreApiTestService.ChreBleStartScanSync",
                        inputBuilder.build());
        assertThat(response.size() > 0).isTrue();
        for (ChreApiTest.GeneralSyncMessage status: response) {
            assertThat(status.getStatus()).isTrue();
        }
    }

    /**
     * Stops a BLE scan and asserts it was started successfully in a synchronous manner.
     * This waits for the event to be received and returns the status in the event.
     */
    private void chreBleStopScanSync() throws Exception {
        ChreApiTestUtil util = new ChreApiTestUtil();
        List<ChreApiTest.GeneralSyncMessage> response =
                util.callServerStreamingRpcMethodSync(mRpcClient,
                        "chre.rpc.ChreApiTestService.ChreBleStopScanSync");
        assertThat(response.size() > 0).isTrue();
        for (ChreApiTest.GeneralSyncMessage status: response) {
            assertThat(status.getStatus()).isTrue();
        }
    }

    /**
     * Starts a BLE scan on the host side with known Google beacon filters.
     */
    private void startBleScanOnHost() {
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        mBluetoothLeScanner.startScan(getDefaultScanFilterHost(),
                scanSettings, mScanCallback);
    }

    /**
     * Stops a BLE scan on the host side.
     */
    private void stopBleScanOnHost() {
        mBluetoothLeScanner.stopScan(mScanCallback);
    }

    /**
     * Tests with the host starting scanning first.
     */
    private void testHostScanFirst() throws Exception {
        startBleScanOnHost();
        chreBleStartScanSync(getDefaultScanFilter());
        Thread.sleep(1000);
        chreBleStopScanSync();
        stopBleScanOnHost();
    }

    /**
     * Tests with CHRE starting scanning first.
     */
    private void testChreScanFirst() throws Exception {
        chreBleStartScanSync(getDefaultScanFilter());
        startBleScanOnHost();
        Thread.sleep(1000);
        stopBleScanOnHost();
        chreBleStopScanSync();
    }
}