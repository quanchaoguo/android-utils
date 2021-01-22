package com.example.bluetooth

import android.bluetooth.BluetoothDevice
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


object BLEClassUtil {
    private val TAG = "--" + BLEClassUtil::class.java.simpleName

    /**
     * 取消用户输入
     *
     * @param bleDevice
     * @return
     */
    fun cancelPairingUserInput(bleDevice: BluetoothDevice): Boolean {
        val cancelPairingUserInput: Method
        var result = false
        try {
            cancelPairingUserInput = bleDevice.javaClass.getMethod("cancelPairingUserInput")
            val returnValue = cancelPairingUserInput.invoke(bleDevice) as Boolean
            result = returnValue
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * 手机和蓝牙采集器配对
     *
     * @param bleDevice
     * @param str
     * @return
     * @throws Exception
     */
    fun setPin(bleDevice: BluetoothDevice, str: String): Boolean {
        var value = false
        try {
            val setPin: Method = bleDevice.javaClass.getDeclaredMethod(
                "setPin", *arrayOf<Class<*>>(
                    ByteArray::class.java
                )
            )
            val invoke = setPin.invoke(bleDevice, arrayOf<Any>(str.toByteArray())) as Boolean
            value = invoke
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return value
    }

    /**
     * 绑定
     *
     * @param bleDevice
     * @return
     * @throws Exception
     */
    fun createBond(bleDevice: BluetoothDevice): Boolean {
        var result = false
        try {
            val createBond: Method = bleDevice.javaClass.getMethod("createBond")
            val invoke = createBond.invoke(bleDevice) as Boolean
            result = invoke
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        return result
    }

    /**
     * 取消配对
     *
     * @param cls
     * @param bleDevice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun removeBond(cls: Class<*>, bleDevice: BluetoothDevice?): Boolean {
        val removeBond: Method = cls.javaClass.getMethod("removeBond")
        val returnValue = removeBond.invoke(bleDevice) as Boolean
        return returnValue
    }


    /**
     * 取消绑定
     *
     * @param bleDevice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun cancelBondProcess(bleDevice: BluetoothDevice): Boolean { //        Class.forName("cancelBondProcess")
        val cancelBondProcess: Method = bleDevice.javaClass.getMethod("cancelBondProcess")
        val returnValue = cancelBondProcess.invoke(bleDevice) as Boolean
        return returnValue
    }

}