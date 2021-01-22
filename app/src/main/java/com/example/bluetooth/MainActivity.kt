package com.example.bluetooth

import BluetoothUtils
import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pub.devrel.easypermissions.EasyPermissions
import java.util.*


/*
* 蓝牙模块的 POC 入口
* */
class MainActivity : AppCompatActivity() {

    private lateinit var manager: BluetoothManager  // 蓝牙管理器
    private lateinit var mBluetoothAdapter: BluetoothAdapter    // 蓝牙适配器 蓝牙的一系列操作
    // private var bluetoothReceiver: BroadcastReceiver ?= null
    private var mBluetoothGatt: BluetoothGatt ? = null   // GATT 的基本功能。例如重新连接蓝牙设备，发现蓝牙设备的 Service 等等。
    private lateinit var mBluetoothGattCharacteristic: BluetoothGattCharacteristic  // 这个类定义需要往外围设备写入的数据和读取外围设备发送过来的数据。
    private lateinit var mBluetoothGattService: BluetoothGattService // BluetoothGatt#getService 获得 通过这个类的 getCharacteristic(UUID uuid) 进一步获取 Characteristic 实现蓝牙数据的双向传输。
    private lateinit var connectBluetoothReceiver: BroadcastReceiver
    private lateinit var mService: BluetoothGattService
    private var mBluetoothDevice: BluetoothDevice ? = null
    private var mBluetoothGattCharacteristics =  listOf<BluetoothGattCharacteristic>()
    private lateinit var mGatt:BluetoothGatt
    private val bluetoothDeviceList = arrayListOf<BluetoothDevice>()

    private lateinit var mCharacteristicWrite: BluetoothGattCharacteristic
    private lateinit var mCharacteristicSubscibe: BluetoothGattCharacteristic

    private var mGattCallback = object: BluetoothGattCallback() {

        //发现服务回调,使用发现服务后会首先触发这个回调，我们在这里可以获得对应UUID的蓝牙服务（Services）和特征（Characteristic）
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int ) {
            mGatt = gatt;
            var services = mGatt.services
            Thread{
                // services 服务 (Characteristic 特征 唯一的 UUID 作为标识符)
                for (service in services) {
                    if (service.uuid.equals(UUID.fromString(BLUE_UUID))) {
                        mService = service // 进行通信的。
                        mBluetoothGattCharacteristics = mService.getCharacteristics(); // 进行通信的。
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
                                characteristicWriteData()
                            }

                            /*如果该字符串可订阅*/
                            if (charaProp and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0 || charaProp and BluetoothGattCharacteristic.PROPERTY_INDICATE > 0) {
                                Log.i("如果该字符串可订阅", "可订阅")
                                mCharacteristicSubscibe = mService.getCharacteristic(UUID.fromString(BLUE_UUID_SUBSCIBE));
                                val descriptor: BluetoothGattDescriptor = mCharacteristicSubscibe.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG))
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

//                BluetoothGatt.STATE_CONNECTED;//已经连接
//                BluetoothGatt.STATE_CONNECTING;//正在连接
//                BluetoothGatt.STATE_DISCONNECTED;//已经断开
//                BluetoothGatt.STATE_DISCONNECTING;//正在断开

            if (status ==  BluetoothGatt.GATT_SUCCESS && BluetoothGatt.STATE_CONNECTED == newState){
                mBluetoothGatt!!.discoverServices()
                Handler().postDelayed({
                    mBluetoothGatt!!.discoverServices()
                }, 1000)
            }

        }

        //读取特征的回调
        override fun onCharacteristicRead(gatt:BluetoothGatt , characteristic:BluetoothGattCharacteristic , status:Int ) {
            super.onCharacteristicRead(gatt, characteristic, status);
            // 读数据
        }

        //写入特征数据的回调，写入后会回调一次这个方法，你可以读取一次你写入的数据以确认写入数据无误。
        override fun onCharacteristicWrite(gatt:BluetoothGatt , characteristic:BluetoothGattCharacteristic , status:Int ) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //写出成功 接下来  该去读取蓝牙设备的数据了
                // 这里的READUUID 应该是READ通道的UUID 不过我这边的设备没有用到READ通道  所以我这里就注释了 具体使用 视情况而定
                // var readCharact = gattService.getCharacteristic(UUID.fromString(READUUID));
                // gatt.readCharacteristic(readCharact);
            }

            // 写数据
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
        override fun onCharacteristicChanged(gatt: BluetoothGatt ,characteristic:BluetoothGattCharacteristic) {
            readCharacteristic(characteristic)
        }
    }

    companion object {
        const val PERMS_LOCATION_CODE = 101
        const val REQUEST_BLE_OPEN = 102
        const val ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED";
        const val BLUE_UUID = "0000ff03-0000-1000-8000-00805f9b34fb"
        const val BLUE_UUID_SUBSCIBE = "0000ff01-0000-1000-8000-00805f9b34fb"
        const val BLUE_UUID_WRITE = "0000ff02-0000-1000-8000-00805f9b34fb"
        const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter= manager.getAdapter();

        // bluetoothReceiver()
    }
    // 搜索设备 广播器 （搜成功  搜到 配对成功的一些广播）
//    fun bluetoothReceiver() {
//        bluetoothReceiver = object : BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                val action = intent.action
//                when(action) {
//
//                    BluetoothDevice.ACTION_FOUND -> {
//                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
//                        bluetoothDeviceList.add(device)
//                        Log.i("BluetoothDevice", "扫描到蓝牙" + device.uuids + "名字" + device.name)
//                    }
//
//                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
//                        Log.i("cancelDiscovery", "停止扫描")
//                        Toast.makeText(this@MainActivity, "搜索完成", Toast.LENGTH_LONG).show()
//                        mBluetoothAdapter.cancelDiscovery()
//                    }
//
//                    // TODO: 蓝牙配对广播
//                    ACTION_CONNECTION_STATE_CHANGED -> {
//
//                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.ERROR) //当前的连接状态
//                        mBluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) // 连接的设备信息
//
//                        if (state.equals(BluetoothAdapter.STATE_CONNECTED)) { // 连接成功
//                            Toast.makeText(this@MainActivity, "连接成功blue:" + mBluetoothDevice!!.name, Toast.LENGTH_LONG).show()
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                                mBluetoothGatt = mBluetoothDevice!!.connectGatt(this@MainActivity, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
//                            } else {
//                                mBluetoothGatt = mBluetoothDevice!!.connectGatt(this@MainActivity, false, mGattCallback);
//                            }
//                        }
//                    }
//
//                    BluetoothDevice.ACTION_PAIRING_REQUEST -> {
////                        var type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
////                        mBluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) // 连接的设备信息
////                        context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
////
////                        var isSuccess = false
////                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
////                            isSuccess = mBluetoothDevice!!.setPin("111111".toByteArray())
////
////                        } else {
////                            isSuccess = BLEClassUtil.setPin(mBluetoothDevice!!, "111111");
////                        }
//                    }
//                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
//
//                        mBluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
//
//                        when (mBluetoothDevice!!.bondState) {
//                            BluetoothDevice.BOND_BONDING ->  //正在配对
//                                Toast.makeText(this@MainActivity, "正在配对", Toast.LENGTH_LONG).show()
//                            BluetoothDevice.BOND_BONDED ->  //配对结束
//                                connectBluetooth(mBluetoothDevice!!)
//                            BluetoothDevice.BOND_NONE ->  //取消配对/未配对
//                                Toast.makeText(this@MainActivity, "取消配对/未配对", Toast.LENGTH_LONG).show()
//                        }
//
//                    }
//
//                }
//
//
//            }
//        }
//
//        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
//        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
//        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
//        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
//        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
//        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
//        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
//        filter.addAction(ACTION_CONNECTION_STATE_CHANGED)
//        registerReceiver(bluetoothReceiver, filter)
//    }

    /**
     * Android 6.0 动态申请授权定位信息权限，否则扫描蓝牙列表为空
     */

    fun requestPromiss(v: View) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permsLocation = arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (EasyPermissions.hasPermissions(this,  *permsLocation)) {
                Log.i("EasyPermissions", "有位置权限")
            } else {
                Log.i("EasyPermissions", "无位置权限")
                EasyPermissions.requestPermissions(this, "请求获得设备定位权限", PERMS_LOCATION_CODE , *permsLocation)
                Log.i("EasyPermissions", "申请位置权限")
            }

        }
    }

    fun connectBlue(v: View) {

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        for (device in bluetoothDeviceList) {
            if (device.name != null && device.name.equals("NIO")) {
                Toast.makeText(this@MainActivity, "以查询到 NIO ", Toast.LENGTH_LONG).show()
                try {
                    // 如果想要取消已经配对的设备，只需要将creatBond 改为 removeBond
                    //  连接蓝牙方式 2


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        device.createBond()
                    } else {
                        BLEClassUtil.createBond(device)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
                this.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
            }
        }
    }

    fun connectBluetooth(device: BluetoothDevice) {

        if (mBluetoothGatt != null) {
            mBluetoothGatt?.let {
                it.disconnect()
                it.close()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = device.connectGatt(this@MainActivity, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            mBluetoothGatt = device.connectGatt(this@MainActivity, false, mGattCallback);
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

    fun characteristicWriteData() {
        mCharacteristicWrite = mService.getCharacteristic(UUID.fromString(BLUE_UUID_WRITE));
        mCharacteristicWrite.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        var value = BluetoothUtils.hexStringToBytes("310D0A")
        mCharacteristicWrite.setValue(value);
        mBluetoothGatt!!.writeCharacteristic(mCharacteristicWrite);
    }

    fun sendData(v: View) {
        characteristicWriteData()
    }

    // 搜索设备
    fun broadBlue(v: View) {
        if (!mBluetoothAdapter.isEnabled()){
            return;
        }

        Toast.makeText(this@MainActivity, "开始搜索", Toast.LENGTH_LONG).show()
        mBluetoothAdapter.startDiscovery(); // 搜索设备
    }

    // 开启蓝牙
    fun openBlue(v: View) {
        if (!mBluetoothAdapter.isEnabled) {
            Log.i("closeBluetooth", "正在开启蓝牙")
            mBluetoothAdapter.enable();
            //8.0版本 使用这个可以弹窗询问开启 其他版本则是不提示启动
//            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
//                val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//                startActivityForResult(intent, 1)
//            }
        }
    }

    // 关闭蓝牙
    fun closeBluetooth(v: View) {
        mBluetoothAdapter.disable();
    }


    fun beDiscovered(v: View) {
        if (mBluetoothAdapter.getScanMode() !== BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Log.i("beDiscovered", "蓝牙可被周边搜索到")
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(discoverableIntent)
        }
    }
    // 取消配对
    fun unpairDevice(v: View) {

        val bondedDevices = mBluetoothAdapter.bondedDevices

        if (bondedDevices != null && bondedDevices.size > 0) {
            for (device in bondedDevices) {
                Thread{
                    try {
                        val m = device::class.java.getMethod("removeBond")
                        m.invoke(device, null)
                    } catch (e: java.lang.Exception) {

                    }
                }.start()
            }
        }
    }

    fun newActivity(v: View) {
        val intent = Intent(this, ScanActivity::class.java)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions!!, grantResults!!)
        //调用easypermission结果监听返回
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // unregisterReceiver(bluetoothReceiver)
    }

    // 是否支持蓝牙
    fun isSupportBluetooth(v: View) {
        if (mBluetoothAdapter == null || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Log.i("Bluetooth", "不支持蓝牙")
        } else {
            Log.i("Bluetooth", "支持蓝牙")
        }
    }
    // 打开蓝牙 （8.0 以下）
    fun openBlueIntent(v: View) {
        if (!mBluetoothAdapter.isEnabled) {
            Log.i("openBlue", "正在开启蓝牙")
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLE_OPEN);
        } else {
            Log.i("openBlue", "蓝牙已经是开启状态")
        }
    }
}
