package com.example.bluetooth

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED
import android.bluetooth.BluetoothDevice.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import java.util.*


/**
 * Author: quanchao.guo
 *
 * Time: 2020/4/10 3:59
 * Description: This is ScanActivity
 */

class ScanActivity : Activity() {

    private lateinit var mBluetoothAdapter: BluetoothAdapter    // 蓝牙适配器 蓝牙的一系列操作
    private lateinit var manager: BluetoothManager  // 蓝牙管理器
    private lateinit var mBluetoothLeScanner: BluetoothLeScanner // 蓝牙扫描器

    private lateinit var mSettings: ScanSettings // 蓝牙扫描设置
    private lateinit var mScanCallback: ScanCallback // 蓝牙扫描回调
    private lateinit var mFilter: ScanFilter // 蓝牙扫描过滤
    private var mFilterList = arrayListOf<ScanFilter>()  // 蓝牙扫描过滤 数组

    private var device: BluetoothDevice ?= null // NIO 可连接的蓝牙设备
    private var mBluetoothGatt: BluetoothGatt ? = null // 蓝牙 Gatt 协议
    private var mGattCallback = object: BluetoothGattCallback() {

        //发现服务回调,使用发现服务后会首先触发这个回调，我们在这里可以获得对应UUID的蓝牙服务（Services）和特征（Characteristic）
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int ) {
            var servicesList = gatt.services

            Thread{
                // services 服务 (Characteristic 特征 唯一的 UUID 作为标识符)
                for (service in servicesList) {
                    if (service.uuid.equals(UUID.fromString(MainActivity.BLUE_UUID))) {

                        var mBluetoothGattCharacteristics = service.getCharacteristics(); // 进行通信的。

                        for (characteristics in mBluetoothGattCharacteristics) {
                            Log.i("characteristics", "uuid" + characteristics.uuid)
                            var charaProp = characteristics.properties

                            /*如果该字符串可读*/
                            if (charaProp and BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                                Log.i("如果该字符串可读", "可读")
                            }

                            /*如果该字符串可写*/
                            if (charaProp and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0 || charaProp and BluetoothGattCharacteristic.PROPERTY_WRITE > 0) {
                                Log.i("如果该字符串可写", "可写")
                            }

                            /*如果该字符串可订阅*/
                            if (charaProp and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0 || charaProp and BluetoothGattCharacteristic.PROPERTY_INDICATE > 0) {
                                Log.i("如果该字符串可订阅", "可订阅")

                                var mCharacteristicSubscibe = service.getCharacteristic(UUID.fromString(
                                    MainActivity.BLUE_UUID_SUBSCIBE
                                ));
                                val descriptor: BluetoothGattDescriptor = mCharacteristicSubscibe.getDescriptor(UUID.fromString(
                                    MainActivity.CLIENT_CHARACTERISTIC_CONFIG
                                ))
                                if (descriptor != null) {
                                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                    mBluetoothGatt!!.writeDescriptor(descriptor);
                                }
                                mBluetoothGatt!!.setCharacteristicNotification(mCharacteristicSubscibe, true)
                            }
                        }
                    }
                }
            }.start()
        }

        //设备状态变化回调，连接成功后会首先触发回调 回调参数分别为 1.蓝牙网关 2.蓝牙状态 3.连接情况

        /**
         * 连接状态改变
         *
         * @param gatt 蓝牙设备的 Gatt 服务连接类
         * @param status 代表是否成功执行了连接操作，
         *               如果为 BluetoothGatt.GATT_SUCCESS 表示成功执行连接操作，
         *               第三个参数才有效，否则说明这次连接尝试不成功
         * @param newState 代表当前设备的连接状态，
         *                 如果 newState == BluetoothProfile.STATE_CONNECTED 说明设备已经连接，
         *                 可以进行下一步的操作了（发现蓝牙服务，也就是 Service）。
         *                 当蓝牙设备断开连接时，这一个方法也会被回调，
         *                 其中的 newState == BluetoothProfile.STATE_DISCONNECTED

         */

        override fun onConnectionStateChange(gatt:BluetoothGatt , status:Int , newState:Int ) {
            super.onConnectionStateChange(gatt, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED){
                // 连接成功
                mBluetoothGatt!!.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // 连接断开
            } else {
                // 其它错误
            }

        }

        //读取特征的回调
        override fun onCharacteristicRead(gatt:BluetoothGatt, characteristic: BluetoothGattCharacteristic, status:Int ) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        //写入特征数据的回调，写入后会回调一次这个方法，你可以读取一次你写入的数据以确认写入数据无误。
        override fun onCharacteristicWrite(gatt:BluetoothGatt, characteristic: BluetoothGattCharacteristic, status:Int ) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
            }
        }

        //读取描述符的回调
        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorRead(gatt, descriptor, status)
        }

        //写入描述符的回调
        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        //可信写入回调，当你写入非法范围的值（比如温度范围10-40，但是你输入了一个50）时可以调用对应方法，切换到这个回调中。处理后续逻辑
        override fun onReliableWriteCompleted(gatt:BluetoothGatt , status: Int) {
            super.onReliableWriteCompleted(gatt, status);
        }

        //读取信号强度的回调 rssi为信号强度值（）
        override fun onReadRemoteRssi(gatt:BluetoothGatt , rssi:Int , status:Int ) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        //蓝牙网卡变化回调
        override fun onMtuChanged(gatt:BluetoothGatt , mtu:Int , status:Int ) {
            super.onMtuChanged(gatt, mtu, status);
        }

        // 监听
        override fun onCharacteristicChanged(gatt: BluetoothGatt ,characteristic: BluetoothGattCharacteristic) {
            readCharacteristic(characteristic)
        }
    }// 蓝牙连接的callback

    private var mbluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            var action = intent.action
            when(action) {
                // 蓝牙的开关状态
                ACTION_STATE_CHANGED -> {
                    Log.i("蓝牙的开关状态", ACTION_STATE_CHANGED)
                }
                // 蓝牙的 打开蓝牙
                ACTION_REQUEST_ENABLE -> {
                    Log.i("蓝牙的 打开蓝牙", ACTION_REQUEST_ENABLE)
                }
                // 蓝牙开始连接
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    Log.i("蓝牙开始连接", ACTION_ACL_CONNECTED)
                }
                // 蓝牙断开连接
                BluetoothDevice.ACTION_ACL_DISCONNECTED-> {
                    Log.i("蓝牙断开连接", ACTION_ACL_DISCONNECTED)
                }
                // 有蓝牙连接后
                ACTION_CONNECTION_STATE_CHANGED -> {
                    Log.i("有蓝牙连接后", ACTION_CONNECTION_STATE_CHANGED)
                }
                // 有蓝牙请求配对
                ACTION_PAIRING_REQUEST -> {
                    Log.i("有蓝牙请求配对", ACTION_PAIRING_REQUEST)
                }
                // 蓝牙配对状态
                ACTION_BOND_STATE_CHANGED -> {
                    Log.i("蓝牙配对状态", ACTION_BOND_STATE_CHANGED)

                }
            }
        }

    }

    companion object {
        const val BLUE_SERVICE_UUIDS = "00001812-0000-1000-8000-00805f9b34fb"
        const val BLUE_NAME = "NIO"
        const val BLUE_ADDRESS = "B8:0E:92:2F:D4:16"
        const val ACTION_REQUEST_ENABLE = "android.bluetooth.adapter.action.STATE_CHANGED" // 打开蓝牙的广播
        const val ACTION_DISCOVERY_STARTED = "android.bluetooth.adapter.action.DISCOVERY_STARTED";  //开始扫描
        const val ACTION_DISCOVERY_FINISHED = "android.bluetooth.adapter.action.DISCOVERY_FINISHED"; //扫描结束
        const val ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED" // 蓝牙连接状态的广播
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_activity)

        manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter= manager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.bluetoothLeScanner
    }

    fun openBLE(v: View) {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, MainActivity.REQUEST_BLE_OPEN);
    }

    fun closeBLE(v: View) {
        mBluetoothAdapter.disable();
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun scanBLE(v: View) {
        //设置一些扫描参数
        mSettings = ScanSettings
            .Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) //例如这里设置的低延迟模式，也就是更快的扫描到周围设备，相应耗电也更厉害
            .build()

        mScanCallback = object : ScanCallback() {
            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
            }

            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)

                if (device == null) device = result.device

                device?.let {
                    Log.e(TAG, "onScanResult: name: " + device!!.name +
                            ", address: " + device!!.getAddress() +
                            ", rssi: " + result.getRssi() + ", scanRecord: " + result.getScanRecord());
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                super.onBatchScanResults(results)
            }
        }

        mFilter = ScanFilter
            .Builder()
            .setDeviceName("NIO")
            .setServiceUuid(ParcelUuid.fromString("00001812-0000-1000-8000-00805f9b34fb"))
            .build()


        mFilterList.add(mFilter)


        mBluetoothLeScanner.startScan(mFilterList, mSettings, mScanCallback)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun stopScanBLE(v: View) {
        mBluetoothLeScanner.stopScan(mScanCallback)
    }

    fun connect(v: View) {

        device?.let {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mBluetoothGatt = device!!.connectGatt(this@ScanActivity, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                mBluetoothGatt = device!!.connectGatt(this@ScanActivity, false, mGattCallback);
            }
        }
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return
        }
        mBluetoothGatt!!.readCharacteristic(characteristic)
        val bytes = characteristic.value
        val str = String(bytes)
        Log.i("监听数据", str)
    }

    fun closeConnect(v:View) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return
        }
        mBluetoothGatt?.disconnect()
        mBluetoothGatt?.close()
        mBluetoothGatt = null
    }

    fun initBroadcast(v:View) {

        var filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        /**
         * 蓝牙的开关状态 (
         * int STATE_OFF = 10; //蓝牙关闭、int STATE_ON = 12; //蓝牙打开、 int STATE_TURNING_OFF = 13; //蓝牙正在关闭 int STATE_TURNING_ON = 11; //蓝牙正在打开)
         */
        filter.addAction(ACTION_STATE_CHANGED)    // 蓝牙的开关状态
        filter.addAction(ACTION_REQUEST_ENABLE)  // 蓝牙的 打开蓝牙
        filter.addAction(ACTION_PAIRING_REQUEST)    // 有蓝牙设备的请求配对广播
        filter.addAction(ACTION_BOND_STATE_CHANGED) // 蓝牙配对状态的广播
        filter.addAction(ACTION_CONNECTION_STATE_CHANGED) // 蓝牙连接状态的广播

        registerReceiver(mbluetoothReceiver, filter)
    }

    fun connectAddress(v:View) {
        if (mBluetoothAdapter == null) {
            return
        }

        if (mBluetoothGatt != null) {
            mBluetoothGatt?.connect()
            return
        }

        device = mBluetoothAdapter.getRemoteDevice(BLUE_ADDRESS)
        device?.let {
            Log.e(TAG, "onScanResult: name: " + device!!.name +
                    ", address: " + device!!.getAddress());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = device!!.connectGatt(this@ScanActivity, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            mBluetoothGatt = device!!.connectGatt(this@ScanActivity, false, mGattCallback);
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mbluetoothReceiver)
    }
}