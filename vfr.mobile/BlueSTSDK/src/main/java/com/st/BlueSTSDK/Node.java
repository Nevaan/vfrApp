/*******************************************************************************
* COPYRIGHT(c) 2015 STMicroelectronics
*
* Redistribution and use in source and binary forms, with or without modification,
* are permitted provided that the following conditions are met:
*   1. Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
*   2. Redistributions in binary form must reproduce the above copyright notice,
*      this list of conditions and the following disclaimer in the documentation
*      and/or other materials provided with the distribution.
*   3. Neither the name of STMicroelectronics nor the names of its contributors
*      may be used to endorse or promote products derived from this software
*      without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
* FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
* DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
* SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
* CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
* OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*
******************************************************************************/
package com.st.BlueSTSDK;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.util.SparseArray;

import com.st.BlueSTSDK.Features.FeatureGenPurpose;
import com.st.BlueSTSDK.Utils.BLENodeDefines;
import com.st.BlueSTSDK.Utils.BleAdvertiseParser;
import com.st.BlueSTSDK.Utils.InvalidBleAdvertiseFormat;
import com.st.BlueSTSDK.Utils.NumberConversion;
import com.st.BlueSTSDK.Utils.UnwrapTimestamp;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** Represent an object that can export some data (feature) using the ble connection
 *
 * @author STMicroelectronics - Central Labs.
 * @version 1.0
 * */
public class Node{
    private static final String TAG = Node.class.getCanonicalName();
    /**
     * wait this time before retry to send a command to the ble api
     */
    private static final long RETRY_COMMAND_DELAY_MS =300;

	/** Node type: type the board that is advertising connection */
    public enum Type {
        /** unknown board type */
        GENERIC,
        /** STEVAL-WESU1 board */
        STEVAL_WESU1,
        /** SensorTile board */
        SENSOR_TILE,

        BLUE_COIN,
        /** board based on a x NUCLEO board */
        NUCLEO
    }//Type

    /** State of the node */
    public enum State {
        /** dummy initial state */
        Init,
        /** the node is waiting for a connection, it is sending advertise message*/
        Idle,
        /** we open a connection with the node */
        Connecting,
        /** we are connected with the node, this status can be fired 2 times if we do a secure
         * connection using bt pairing */
        Connected,
        /** we are closing the node connection */
        Disconnecting,
        /** we saw the advertise message for some time but now we do not receive anymore*/
        Lost,
        /** we were connected with the node but now it is disappear without
         disconnecting */
        Unreachable,
        /** dummy final state */
        Dead
    }//State

    /**
     * Interface used for notify change on the ble connection
     * @author STMicroelectronics - Central Labs.
     */
    public interface BleConnectionParamUpdateListener {

        /**
         * method call when have new information about the rssi value
         * @param node node that update its rssi value
         * @param newRSSIValue new rssi
         */
        @WorkerThread
        void onRSSIChanged(Node node,int newRSSIValue);

        /**
         * method call when we have new information about the tx power
         * @param node node that update its tx power
         * @param newPower  new transmission power
         */
        @SuppressWarnings("UnusedDeclaration")
        void onTxPowerChange(Node node, int newPower);
    }//BleConnectionParamUpdateListener

    /**
     * Interface where notify that the node change its internal state
     * @author STMicroelectronics - Central Labs.
     */
    public interface NodeStateListener {

        /**
         * function call when one node change its status
         * @param node note that change its status
         * @param newState new node status
         * @param prevState previous node status
         */
        @WorkerThread
        void onStateChange(Node node, State newState, State prevState);
    }//NodeStateListener


    /**
     * test if a characteristics can be read
     * @param characteristic characteristic to read
     * @return true if we can read it
     */
    private static boolean charCanBeRead(BluetoothGattCharacteristic characteristic){
        return characteristic!=null &&
                (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ )!=0;
    }//charCanBeRead

    /**
     * test if a characteristics can be write
     * @param characteristic characteristic to write
     * @return true if we can write it
     */
    private static boolean charCanBeWrite(BluetoothGattCharacteristic characteristic){
        return characteristic!=null &&
                (characteristic.getProperties() &
                (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE |
                        BluetoothGattCharacteristic.PROPERTY_WRITE))!=0;
    }//charCanBeWrite

    /**
     *
     * test if a characteristics can be notify
     * @param characteristic characteristic to notify
     * @return true if we can receive notification from it
     */
    private static boolean charCanBeNotify(BluetoothGattCharacteristic characteristic){
        return characteristic!=null &&
                (characteristic.getProperties() &
                        (BluetoothGattCharacteristic.PROPERTY_NOTIFY))!=0;
    }//charCanBeNotify


    /**
     * check if the node is doing the pairing and set the mNodeWillPair variable
     * @return true if the node is doing the bonding, false otherwise
     */
    private boolean isPairing(){
        return mDevice.getBondState()==BluetoothDevice.BOND_BONDING;
    }

    /** callback method to use when we send ble commands */
    private class GattNodeConnection extends BluetoothGattCallback {

        private UnwrapTimestamp mUnwrapTimestamp = new UnwrapTimestamp();

        /**
         * if we are connecting it start to scan the device service/characteristics otherwise it
         * change the node status to idle or unreachable.
         * if there is an error the node status go to dead
         * @param gatt connection with the device
         * @param status command status
         * @param newState new node status
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState){
            Log.d(TAG,"Node: "+Node.this.getName()+" Status: "+status+" newState: " +
                    ""+newState+" boundState:"+mDevice.getBondState());
            if(status==BluetoothGatt.GATT_SUCCESS){
                if(newState==BluetoothGatt.STATE_CONNECTED){
                    if(!isPairing()) { //if it is pairing we do it when it finish
                        //wait a bit for see if we will do the secure pairing or not,
                        //if the device will be in pair status the scan is aborted and will be
                        //done when the pairing will be completed
                        mBleThread.postDelayed(mScanServicesTask, RETRY_COMMAND_DELAY_MS);
                    }//if !pairing
                }else if (newState == BluetoothGatt.STATE_DISCONNECTED){
                    if(mConnection!=null) {
                        mConnection.close();
                        cleanConnectionData();
                    }//if
                    if (mUserAskToDisconnect){
                        //disconnect completed
                        Node.this.updateNodeStatus(State.Idle);
                    }else{
                        //we disconnect but the user didn't ask it
                        Node.this.updateNodeStatus(State.Unreachable);
                    }//if else
                }//if-else
            }else{
                Node.this.updateNodeStatus(State.Dead);
                if(mConnection!=null) {
                    mConnection.close();
                    cleanConnectionData();
                }//if
            }//if status
        }//onConnectionStateChange


        /**
         * update the node rssi information
         * @param gatt connection
         * @param rssi rssi with the device
         * @param status true if the read is successfully
         */
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status){
            if(status == BluetoothGatt.GATT_SUCCESS){
                Node.this.updateRssi(rssi);
            }else{
                Log.e(TAG, "Impossible retrieve the rssi value");
            }//if-else
        }//onReadRemoteRssi

        /**
         * if present we build the debug service for be able to send/receive the debug information
         * @param debugService debug service
         * @return object that we can use for communicate with the debug console,
         * can be null if we didn't find all the needed characteristics
         */
        private Debug buildDebugService(BluetoothGattService debugService){
            List<BluetoothGattCharacteristic> charList = debugService.getCharacteristics();
            BluetoothGattCharacteristic term=null,err=null;
            //search the term and err characteristics
            for(BluetoothGattCharacteristic temp : charList){
                if(temp.getUuid().equals(BLENodeDefines.Services.Debug.DEBUG_TERM_UUID))
                    term=temp;
                else if(temp.getUuid().equals(BLENodeDefines.Services.Debug.DEBUG_STDERR_UUID))
                    err=temp;
            }//for
            //if both are present we build the object
            if(term!=null && err!=null)
                return new Debug(Node.this,term,err);
            else
                return null;
        }//buildDebugService

        private void buildKnowUuid(BluetoothGattCharacteristic characteristic,
                                    List<Class<? extends Feature>> featureList){
            List<Feature> temp = new ArrayList<>();
            for(Class<? extends Feature> feature : featureList){
                Feature f = buildFeatureFromClass(feature);
                if(f!=null) {
                    f.setEnable(true);
                    mAvailableFeature.add(f);
                    temp.add(f);
                }
            }
            if(!temp.isEmpty())
                mCharFeatureMap.put(characteristic,temp);

        }

        /**
         * build and add the exported features from a ble characteristics
         * @param characteristic characteristics that is handle by the sdk
         */
        private void buildFeature(BluetoothGattCharacteristic characteristic){

            //extract the part of the uuid that contains the feature inside this
            // characteristics
            int featureMask = BLENodeDefines.FeatureCharacteristics.extractFeatureMask
                    (characteristic.getUuid());
            List<Feature> temp = new ArrayList<>();

            //we do the search in reverse order for have the feature in he correct order in case
            //of characteristics that export multiple feature

            long mask= 1L<<31; //1<<31
            //we test all the 32bit of the feature mask
            for(int i=0; i<32; i++ ) {
                if ((featureMask & mask) != 0) { //if the bit is up
                    Feature f =mMaskToFeature.get((int)mask);
                    if (f != null) {
                        f.setEnable(true);
                        temp.add(f);
                    }//if
                }//if
                mask = mask>>1;
            }//for

            //if it is a valid characteristics, we add it on the map
            if(temp.size()!=0){
                mCharFeatureMap.put(characteristic,temp);
            }//if
        }//buildFeature

        /**
         * build a generic feature from a compatible characteristics
         * @param characteristic characteristics that export the data
         */
        private void buildGenericFeature(BluetoothGattCharacteristic characteristic){
            Feature f= null;
            for (Feature fs:mAvailableFeature ) {
                if (fs  instanceof FeatureGenPurpose){
                    if(((FeatureGenPurpose)fs).getFeatureChar().getUuid().toString()
                            .compareToIgnoreCase(characteristic.getUuid().toString()) == 0)
                            f = fs;
                }
            }
            if ( f == null) {
                f = new FeatureGenPurpose(Node.this, characteristic);
                f.setEnable(true);
                mAvailableFeature.add(f);
            }
            List<Feature> temp = new ArrayList<>(1);
            temp.add(f);
            mCharFeatureMap.put(characteristic, temp);
        }//buildGenericFeature

        /**
         * scan all the service searching for know characteristics + enable the found feature
         * @param gatt connection with the device
         * @param status operation result
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status){
            Log.d(TAG,"onServicesDiscovered status:"+status+" boundState:" + mDevice.getBondState());


            if(status == BluetoothGatt.GATT_FAILURE) {
                Node.this.updateNodeStatus(State.Dead);
                mNScanRequest.decrementAndGet();
                return;
            }//if

            //we are pairing -> avoid to do the scanning and wait to be paired
            if(isPairing()){
                mNScanRequest.decrementAndGet();
                return;
            }//if

            //for avoid to update the data after that we connect
            //we can have an auto connect -> return if the previous status is unreachable
            if(Node.this.mState!=State.Connecting && Node.this.mState!=State.Unreachable) {
                mNScanRequest.decrementAndGet();
                return;
            }//if

            //List<BluetoothGattService> knowServices = filterKnowServices(gatt.getServices());
            List<BluetoothGattService> nodeServices = gatt.getServices();
            if(nodeServices.size()==0) { // the list is empty -> exit
                Node.this.updateNodeStatus(State.Dead);
                mNScanRequest.decrementAndGet();
                return;
            }//if

            mCharFeatureMap.clear();
            for(BluetoothGattService service : nodeServices){
                //check if it is a specific service
                if(service.getUuid().equals(BLENodeDefines.Services.Debug.DEBUG_SERVICE_UUID))
                    mDebugConsole = buildDebugService(service);
                else if(service.getUuid().equals(BLENodeDefines.Services.Config.CONFIG_CONTROL_SERVICE_UUID)) {
                    List<BluetoothGattCharacteristic> controlChar = service.getCharacteristics();
                    //check for the initialization characteristics
                    for(BluetoothGattCharacteristic characteristic : controlChar) {
                        if (characteristic.getUuid().equals(BLENodeDefines.Services.Config.FEATURE_COMMAND_UUID))
                            mFeatureCommand = characteristic;
                        if (characteristic.getUuid().equals(BLENodeDefines.Services.Config.REGISTERS_ACCESS_UUID))
                            mConfigControl = new ConfigControl(Node.this, characteristic,mConnection);
                    }//for
                }else {//otherwise will contains feature characteristics

                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        UUID uuid = characteristic.getUuid();
                        if (BLENodeDefines.FeatureCharacteristics.isFeatureCharacteristics(uuid))
                            buildFeature(characteristic);
                        else if (BLENodeDefines.FeatureCharacteristics
                                .isGeneralPurposeCharacteristics(uuid)) {
                            buildGenericFeature(characteristic);
                        }else if(mExternalCharFeatures!=null &&
                                mExternalCharFeatures.containsKey(uuid))
                            buildKnowUuid(characteristic,mExternalCharFeatures.get(uuid));
                    }//for

                }//if-else-if-else
            }//for each service

           //move on the connected state only if all the discover services are finished
            if(mNScanRequest.decrementAndGet()==0)
                Node.this.updateNodeStatus(State.Connected);

        }//onServicesDiscovered


        /**
         * we have to do a linear search into the hash map since the BluetoothGattCharacteristic
         * keep in account the char values, and this can be different if are different
         * characteristics with the same uuid, this happen when the node is bounded and not
         * recreated afer a disconnection
         * @param c characteristic to search
         * @return list of feature send with this characteristics or null if it doesn't exist
         */
        List<Feature> getCorrespondingFeatures(BluetoothGattCharacteristic c) {
            for (Map.Entry<BluetoothGattCharacteristic, List<Feature>> e : mCharFeatureMap.entrySet()) {
                if(e.getKey().getUuid().equals(c.getUuid())){
                    return e.getValue();
                }//if
            }//for
            return null;
        }//getCorrespondingFeatures

        /**
         * update all the feature inside this characteristic
         * @param characteristic updated characteristic
         * @return true if the characteristic is associated to some feature
         */
        boolean updateFeature(BluetoothGattCharacteristic characteristic){
            List<Feature> features = getCorrespondingFeatures(characteristic);
            if(features!=null){
                byte data[] = characteristic.getValue();
                int timeStamp = NumberConversion.LittleEndian.bytesToUInt16(data);
                long timeStampLong = mUnwrapTimestamp.unwrap(timeStamp);

                int dataOffset =2;
                for(Feature f: features){
                    dataOffset += f.update(timeStampLong,data,dataOffset);
                }//for features
                return true;
            }else
                return false;
            //if feature list != null
        }

        /**
         * send back to the feature the response of its command
         * @param characteristic characteristics that contain the response data
         */
        private void dispatchCommandResponseData(BluetoothGattCharacteristic characteristic){
            byte data[] = characteristic.getValue();
            if(data.length<7) //if we miss some data
                return;
            int timeStamps = NumberConversion.LittleEndian.bytesToUInt16(data);
            int mask = NumberConversion.BigEndian.bytesToInt32(data,2);
            byte reqType= data[6];
            Feature f =mMaskToFeature.get(mask);
            if(f!=null)
                f.commandResponseReceived(timeStamps,reqType, Arrays.copyOfRange(data, 7,
                        data.length));
        }//dispatchCommandResponseData

        /**
         * receive the notification change
         * @param gatt connection with the device
         * @param characteristic updated characteristic
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //if it comes form the console service we send to it
            if(mDebugConsole!=null &&
                    BLENodeDefines.Services.Debug.isDebugCharacteristics(characteristic.getUuid()))
                mDebugConsole.receiveCharacteristicsUpdate(characteristic);
            else if(characteristic.equals(mFeatureCommand)) //if it is the commandCharacteristics
                dispatchCommandResponseData(characteristic);
            else if (mConfigControl != null && characteristic.getUuid().equals(BLENodeDefines.Services.Config.REGISTERS_ACCESS_UUID))
                mConfigControl.characteristicsUpdate(characteristic);
            else //otherwise is a feature characteristics
                updateFeature(characteristic);
        }//onCharacteristicChanged

        /**
         * receive the data after a reading
         * @param gatt connection with the device
         * @param characteristic characteristics that contain the response data
         * @param status command status
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            Log.d(TAG,"Read Char: "+characteristic.getUuid().toString());
            if(status == BluetoothGatt.GATT_SUCCESS) {
                if (mDebugConsole != null &&
                        BLENodeDefines.Services.Debug.isDebugCharacteristics(characteristic.getUuid()))
                    mDebugConsole.receiveCharacteristicsUpdate(characteristic);
                else if (mConfigControl != null && characteristic.getUuid().equals(BLENodeDefines.Services.Config.REGISTERS_ACCESS_UUID))
                    mConfigControl.characteristicsUpdate(characteristic);
                else
                    updateFeature(characteristic);
            }else{
                if(!isPairing()) {
                    Log.e(TAG,"Error reading the characteristics: "+characteristic);
                    Node.this.updateNodeStatus(State.Dead);
                }//if
            }//if-else
        }


        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            //descriptor write -> remove from the queue
            if(status == BluetoothGatt.GATT_SUCCESS)
                dequeueWriteDesc(new WriteDescCommand(descriptor,descriptor.getValue()));
            else{
                if(!isPairing()) {
                    Log.e(TAG,"onDescriptorWrite Error writing the descriptor: "+descriptor
                            .getCharacteristic());
                    Node.this.updateNodeStatus(State.Dead);
                }//if
            }//if-else
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            //Log.d(TAG,"Characteristics Write "+status);
            if (mDebugConsole != null &&
                    BLENodeDefines.Services.Debug.isDebugCharacteristics(characteristic.getUuid()))
                mDebugConsole.receiveCharacteristicsWriteUpdate(characteristic, status == BluetoothGatt.GATT_SUCCESS);
            else if (mConfigControl != null && characteristic.getUuid().equals(BLENodeDefines.Services.Config.REGISTERS_ACCESS_UUID))
                mConfigControl.characteristicsWriteUpdate(characteristic, status == BluetoothGatt.GATT_SUCCESS);

            WriteCharCommand writeOp = new WriteCharCommand(characteristic,characteristic.getValue());
            dequeueCharacteristicsWrite(writeOp);
        }
    }//GattNodeConnection

    /** device associated with this node */
    private BluetoothDevice mDevice;
    /** gatt connection with the device */
    private BluetoothGatt mConnection;
    /** characteristics used for send command to a feature */
    private BluetoothGattCharacteristic mFeatureCommand =null;
    /** thread where we do all the ble commands */
    private Handler mBleThread;

    /**
     * clear the data that are created during the connection
     */
    private void cleanConnectionData(){
        mConnection=null;
        mWriteDescQueue.clear();
        mCharacteristicWriteQueue.clear();
        mCharFeatureMap.clear();

        //remove all the task queued for avoid to run an old task with a new
        //connection object
        mBleThread.removeCallbacks(mScanServicesTask);
        mBleThread.removeCallbacks(mUpdateRssiTask);
        mBleThread.removeCallbacks(mWriteFeatureCommandTask);
        mBleThread.removeCallbacks(mWriteDescriptorTask);
        mBleThread.removeCallbacks(mConnectionTask);
        mBleThread.removeCallbacks(mDisconnectTask);

        //we stop the connection -> we have not notification enabled
        mNotifyFeature.clear();
        mFeatureCommand=null;
        mConfigControl=null;
        mDebugConsole=null;
        mBleThread=null;
    }

    /**
     * task that will disconnect the device
     */
    private Runnable mDisconnectTask = new Runnable() {
        @Override
        public void run() {
            if(mState==State.Disconnecting) {
                mConnection.disconnect();
                //the data will be free in the onConnectionStateChange
            }// if
        } //run
    };

    /**
     * task for ask an update rssi
     */
    private Runnable mUpdateRssiTask = new Runnable() {
        @Override
        public void run() {
            if(mConnection!=null)
                mConnection.readRemoteRssi();
        }
    };


    /**
     * count the number of discover service call, we move in the connected state only if we
     * finish all the call
     */
    //TODO now the scanning is not request if the device is doing the pairing -> this variable
    //should be used
    private AtomicInteger mNScanRequest = new AtomicInteger(0);

    /**
     * task for ask to discover the device service, if the call fail this command will auto
     * submit itself after {@code RETRY_COMMAND_DELAY_MS}
     * if the device is doing a pairing the function will not run, reschedule it when the bonding
     * is complete
     */
    private Runnable mScanServicesTask = new Runnable() {
        @Override
        public void run() {
            if (mConnection != null && !isPairing()){
                if (mConnection.discoverServices()) {
                    mNScanRequest.incrementAndGet();
                }else
                    mBleThread.postDelayed(this, RETRY_COMMAND_DELAY_MS);
                //if-else
            }//if connection
        }//run
    };

    /**
     * tell us if is the first time that we call connect, if true we refresh the device cache if ask
     */
    private boolean mIsFirstConnection=true;

    /**
     * true if the user ask to reset the device cache of this device
     */
    private boolean mResetCache=false;

    /**
     * context used for open the connection
     */
    private Context mContext;


    private static final int MAX_REFRESH_DEVICE_CACHE_TRY=10;
    /**
     * invoke an hide method for clear the device cache, in this way we can have device with same
     * name and mac that export different service/char in different connection (maybe because we
     * are developing on it)
     * @param gatt connection with the device
     * @return tue il the call is invoke correctly
     */
    private static boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            Method localMethod = gatt.getClass().getMethod("refresh");
            if (localMethod != null) {
                boolean done =false;
                int nTry =0;
                while (!done && nTry<MAX_REFRESH_DEVICE_CACHE_TRY) {
                    done=((Boolean) localMethod.invoke(gatt));
                    nTry++;
                }//while
                Log.d(TAG, "Refreshing Device Cache: "+done);
                return done;
            }//if
        } catch (Exception localException) {
            Log.e(TAG, "An exception occurred while refreshing device cache.");
        }//try-catch
        return false;
    }//refreshDeviceCache

    /**
     * task that open a connection with the remote device
     */
    private Runnable mConnectionTask = new Runnable() {
        @Override
        public void run() {
            mConnection = mDevice.connectGatt(mContext,false,new GattNodeConnection());
            if(mConnection==null){
                mBleThread.postDelayed(this, RETRY_COMMAND_DELAY_MS);
            } else if(mIsFirstConnection && mResetCache) {
                refreshDeviceCache(mConnection);
                mIsFirstConnection=false;
            }//
        }//run
    };


    /**
     * listener that receive the status update, it will subscribe the node to the command
     * characteristics for received the notification..
     */
    private NodeStateListener mNotifyCommandChange = new NodeStateListener() {

        @Override
        public void onStateChange(Node node, State newState, State prevState) {
            if(newState == State.Connected && mFeatureCommand !=null) {
                changeNotificationStatus(mFeatureCommand,true);
            }//if
            //error during connection (connecting ->dead or connected -> dead )
            if((prevState == State.Connecting || prevState== State.Connected ) && newState==State
                    .Dead){
                Log.e(TAG,"Error connecting to the node:"+node.getName());
                //we disconnect -> free the gatt resource and connect again
                if(mConnection!=null){ mConnection.close();cleanConnectionData();}
                //we stop the connection -> we have not notification enabled
                mNotifyFeature.clear();
                //remove the pending write request
                synchronized (mWriteDescQueue) {
                    mWriteDescQueue.clear();
                }
                synchronized (mCharacteristicWriteQueue) {
                    mCharacteristicWriteQueue.clear();
                }
            }
            if(newState==State.Dead  || newState==State.Disconnecting){
                if(mBoundStateChange !=null) {
                    //clean the broadcast receiver
                    mContext.getApplicationContext().unregisterReceiver(mBoundStateChange);
                    mBoundStateChange = null;
                }
            }
        }//onStateChange
    };

    /**
     * Helper class that contains the characteristic and the data that we have write on it
     */
    private class WriteCharCommand {
        public BluetoothGattCharacteristic characteristic;
        public byte data[];

        WriteCharCommand(BluetoothGattCharacteristic c, byte d[]){
            characteristic=c;
            data=d;
        }

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof WriteCharCommand))
                return false;
            WriteCharCommand other =(WriteCharCommand)o;
            return characteristic.getUuid().equals(other.characteristic.getUuid()) &&
                    Arrays.equals(data,other.data);
        }//equals
    }//WriteCharCommand

    /**
     * helper class that contains the descriptor and the data that we have to write on it
     */
    private class WriteDescCommand{
        public BluetoothGattDescriptor desc;
        public byte data[];

        WriteDescCommand(BluetoothGattDescriptor desc ,byte data[]){
            this.desc=desc;
            this.data=data;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof WriteDescCommand))
                return false;
            WriteDescCommand other = (WriteDescCommand) o;
            boolean dataEqual;
            //if the other data is null we keep it as equal
            /**
             * in the samsung device sometime the onDescriptorWrite callback as a descriptor with
             * null data.. so the descriptor is not dequeue
             */
            dataEqual = other.data == null || data==null || Arrays.equals(data, other.data);

            //compare the desc uuid and the char uuid
            return desc.getUuid().equals(other.desc.getUuid()) &&
                    desc.getCharacteristic().getUuid().equals(
                            other.desc.getCharacteristic().getUuid()) &&
                    dataEqual;
        }//equals
    }

    /**
     * we have to serialize the descriptor write, -> create a queue
     * + use the enqueueWriteDesc/dequeueWriteDesc for insert element on it
     */
    final private Queue<WriteDescCommand> mWriteDescQueue = new LinkedList<>();

    /** we have to serialize also the write in the characteristic for avoid to lost
     * some message you have to use enqueueWriteChar for add element to this queue
     */
    final private Queue<WriteCharCommand> mCharacteristicWriteQueue = new LinkedList<>();

    /** node state */
    private State mState = State.Init;
    /** last node rssi */
    private int mRssi;
    /** last time that we update the rssi */
    private Date mLastRssiUpdate;
    /** class where notify a node status change, we use a CopyOnWriteArrayList for permit to have
    a one shot listener that remove itself after that it did its work */
    private final CopyOnWriteArrayList<NodeStateListener> mStatusListener = new CopyOnWriteArrayList<>();
    /** class where notify change on the ble connection state  we use a CopyOnWriteArrayList for permit to have
     a one shot listener that remove itself after that it did its work */
    private final CopyOnWriteArrayList<BleConnectionParamUpdateListener> mBleConnectionListeners = new
            CopyOnWriteArrayList<>();
    /** true when the user ask to disconnect form the device */
    private boolean mUserAskToDisconnect;
    /** class that contains the advertise information */
    private BleAdvertiseParser mAdvertise;

    /** list of all the feature that are available in the advertise */
    private ArrayList<Feature> mAvailableFeature;
    /** map that join the build feature with the bitmask that tell us that the feature is present*/
    private Map<Integer,Feature> mMaskToFeature;
    private Map<UUID,List<Class< ? extends Feature>>> mExternalCharFeatures;
    /**
     * map that tell us whit feature we can update when we receive an update from a characteristics
     */
    private Map<BluetoothGattCharacteristic,List<Feature>> mCharFeatureMap= new HashMap<>();

    /** set that contains the features that are in notify*/
    final private Set<Feature> mNotifyFeature=new HashSet<>();

    /**
     * object used write/read debug message to/from the ble device,
     * null if the device doesn't export the debug service   */
    private Debug mDebugConsole;

    /**
     * object used write/read Registers  to/from the ble device,
     * null if the device doesn't export the debug service   */
    private ConfigControl mConfigControl;

    private String mFriendlyName = null;

    /** ms to wait before declare a node as lost */
    private static long NODE_LOST_TIMEOUT_MS=4000;
    /** thread where wait the timeout */
    private Handler mHandler;  //NOTE: since the handler is crate in a thread,
    // is better check that the class is not null in the case we try to use it before the thread
    // got exec
    /** task to run when the timeout expire, it will set the node status as lost */
    private Runnable mSetNodeLost = new Runnable() {
        @Override
        public void run() {
            if(mState==State.Idle){
                updateNodeStatus(State.Lost);
            }//if
        }//run
    };

    /**
     * store the new rssi, and call the call back if needed
     * @param rssi new rssi to store
     */
    protected void updateRssi(int rssi){
        mRssi =rssi;
        mLastRssiUpdate = new Date(); // now
        if(mState==State.Lost)
            updateNodeStatus(State.Idle);
        for(BleConnectionParamUpdateListener listener : mBleConnectionListeners){
            listener.onRSSIChanged(this, rssi);
        }//for
    }//updateRssi

    /**
     * store the new node status and call the call back if needed
     * @param newStatus new node status
     */
    protected void updateNodeStatus(State newStatus){
        State old =mState;
        mState = newStatus;
        for (NodeStateListener listener : mStatusListener) {
            listener.onStateChange(this, newStatus, old);
        }//if
    }//updateNodeStatus

    /**
     * crate a feature from its class instance
     * @param featureClass class object that represent the feature to build
     * @param <T> type of feature to build
     * @return the feature or null if the class doesn't has a method that request a node as a
     * parameter
     */
    protected @Nullable <T extends Feature> T buildFeatureFromClass(Class<T> featureClass){
        try {
            Constructor<T> constructor = featureClass.getConstructor(Node.class);
            return constructor.newInstance(this);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        } catch (InstantiationException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        }//try-catch
    }//buildFeatureFromClass

    /**
     * using the advertise data, we build a list a possible feature that this node can export
     */
    private void buildAvailableFeatures(){
        int featureMask = mAdvertise.getFeatureMap();
        SparseArray<Class<? extends Feature>> decoder = Manager.sFeatureMapDecoder.get(
                mAdvertise.getDeviceId());
        mMaskToFeature = new HashMap<>(32);
        mAvailableFeature = new ArrayList<>(32);
        if(decoder==null){ // unknown board type -> no feature
            return;
        }

        long mask=1;
        //we test all the 32bit of the feature mask
        for(int i=0; i<32; i++ ){
            if((featureMask & mask)!=0) { //if the bit is up
                Class<? extends Feature> featureClass = decoder.get((int)mask);
                if (featureClass != null) { //and the decoder has a name for that bit
                    Feature f = buildFeatureFromClass(featureClass);
                    if(f!=null){
                        mAvailableFeature.add(f);
                        mMaskToFeature.put((int)mask, f);
                    }else {
                        Log.e(TAG,"Impossible build the feature: "+featureClass.getSimpleName());
                    }//if-else
                }//if !=null
            }//if !=0
            mask = mask << 1;
        }//for
    }//buildAvailableFeatures

    /**
     * create a thread where run the handler that will contain the timeout for understand if the
     * node go out of scope
     */
    private void initHandler(){
        new Thread( new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                mHandler = new Handler(); //create the handler
                //put the first item
                mHandler.postDelayed(mSetNodeLost,NODE_LOST_TIMEOUT_MS);
                Looper.loop(); //start exec the item in the queue
            }//run
        }).start(); //run the thread
    }//initHandler

    /**
     * create a new node
     * @param device android ble device
     * @param rssi rssi of the advertise message
     * @param advertise advertise message for this node
     * @throws  InvalidBleAdvertiseFormat if the advertise is not well formed
     */
    public Node(BluetoothDevice device,int rssi,byte advertise[]) throws InvalidBleAdvertiseFormat{
        mAdvertise = new BleAdvertiseParser(advertise);
        mDevice = device;
        mExternalCharFeatures= new HashMap<>();
        updateRssi(rssi);
        updateNodeStatus(State.Idle);
        initHandler();
        buildAvailableFeatures();
        addNodeStateListener(mNotifyCommandChange);
        Log.i(TAG, mAdvertise.toString());
    }

    public void upDateAdvertising(byte advertise[]){
        try {
            mAdvertise = new BleAdvertiseParser(advertise);
        }catch (Exception e){Log.e(TAG,"Error updating advertising for:"+ getName());}
    }

    /**
     * create a new node from the advertise, the advertise have to contain the mac address
     * @param advertise advertise message for this node
     * @throws  InvalidBleAdvertiseFormat if the advertise is not well formed, for this method is
     * mandatory that the advertise contains also the mac address
     */
    public Node(byte advertise[]) throws InvalidBleAdvertiseFormat{
        mAdvertise = new BleAdvertiseParser(advertise);
        String bleAddress = mAdvertise.getAddress();
        if(bleAddress==null){
            throw  new InvalidBleAdvertiseFormat("Device Address non present in the advertise");
        }
        mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(bleAddress);
        mExternalCharFeatures= new HashMap<>();
        updateNodeStatus(State.Idle);
        initHandler();

        buildAvailableFeatures();
        addNodeStateListener(mNotifyCommandChange);
        Log.i(TAG, mAdvertise.toString());
    }

    /**
     * implement for have an api equal to the ios one, not use it, use the version with the context
     */
    public void connect(){
        throw new UnsupportedOperationException("The method is not implemented, " +
                "use Connect(Context) instead.");
    }//connect

    /**
     * open a gatt connection
     * @param c context to use for open the connection
     */
    public void connect(Context c){
        connect(c, false,null);
    }//connect

    public void connect(Context c,boolean resetCache){
        connect(c, false,null);
    }//connect

    /**
     * open a gatt connection
     * @param c context to use for open the connection
     * @param resetCache if true the handle cache for this device will be clear,
     *                   the connection will be slower, it will done only the first time that you
     *                   call this function with the parameter true
     * @param userDefineFeature register the UUID in the map as know UUID that will be manage by
     *                          the node class as feature
     */
    public void connect(Context c,boolean resetCache,
                        @Nullable Map<UUID,List<Class< ? extends Feature>>> userDefineFeature){
        //we start the connection so we will stop to receive advertise, so we delete the timeout
        if(mHandler!=null) mHandler.removeCallbacks(mSetNodeLost);
        mUserAskToDisconnect=false;
        updateNodeStatus(State.Connecting);
        /*
        HandlerThread thread = new HandlerThread("NodeConnection");
        thread.start();
        mBleThread = new Handler(thread.getLooper());
        */
        mBleThread = new Handler(Looper.getMainLooper());
        mContext=c;
        setBoundListener(c.getApplicationContext());
        mResetCache=resetCache;
        addExternalCharacteristics(userDefineFeature);
        mBleThread.post(mConnectionTask);
    }//connect

    /**
     * describe as manage some specific UUID using a feature class, the uuid will be manage by
     * the node class only if is know before the connection
     * if a uuid is already know it will be overwrite with the new list of feature
     * @param userDefineFeature map that link the uuid with the features that contains.
     */
    public void addExternalCharacteristics(@Nullable Map<UUID,List<Class< ? extends Feature>>>
                                                   userDefineFeature){
        if(userDefineFeature==null)
            return;

        mExternalCharFeatures.putAll(userDefineFeature);
    }//addExternalCharacteristics

    private BroadcastReceiver mBoundStateChange = null;
    private void setBoundListener(Context c){
        if(mBoundStateChange !=null)
            return;
        mBoundStateChange = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG,intent.getAction());
                Log.d(TAG,"OnReceive change:"+intent.getIntExtra(BluetoothDevice
                        .EXTRA_BOND_STATE,-1));
                int boundSate =intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                        BluetoothDevice.BOND_NONE);
                if(boundSate == BluetoothDevice.BOND_BONDED){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice
                            .EXTRA_DEVICE);
                    //it is needed for check that the paired device is an our device
                    Node n = Manager.getSharedInstance().getNodeWithTag(device.getAddress());
                    if(n!=null) {
                        mBleThread.post(mScanServicesTask);
                    }//if n
                }//if
            }//onReceive
        };
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        c.registerReceiver(mBoundStateChange, filter);
    }

    /**
     * close the node connection
     */
    public void disconnect(){
        if(!isConnected())
            return;
        mUserAskToDisconnect=true;
        updateNodeStatus(State.Disconnecting);

        // run the waitCompleteAllWriteRequest in a different handler for avoid to block the
        // ble/main thread, we use the handle used for update the rssi, since during disconnection
        // is not used
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                waitCompleteAllDescriptorWriteRequest(mDisconnectTask);
                mHandler.postDelayed(mSetNodeLost, NODE_LOST_TIMEOUT_MS);
            }
        });

    }//disconnect

    /**
     * node type
     * @return node type
     * @see com.st.BlueSTSDK.Node.Type
     */
    public Type getType(){
        return mAdvertise.getBoardType();
    }//getType

    public short getTypeId(){return mAdvertise.getDeviceId();}//getTypeId

    /**
     * node sleeping state
     * @return false running, true device is sleeping
     */
    public boolean isSleeping(){
        return mAdvertise.getBoardSleeping();
    }//getType

    /**
     * node general purpose features available
     * @return true if general purpsoe available else false.
     */
    public boolean hasGeneralPurpose(){
        return mAdvertise.getBoardHasGP();
    }//getType

    /**
     * tell if the node is connected
     * @return true if the internal state is equal to connected
     */
    public boolean isConnected(){
        return mState == State.Connected;
    }

    /**
     * return a unique identification for the node
     * @return return the ble mac address
     */
    public String getTag(){
        return mDevice.getAddress();
    }//getTag

    /**
     * return the internal state of the node
     * @return internal state
     * @see com.st.BlueSTSDK.Node.State
     */
    public State getState(){return  mState; }

    /**
     * get the device name
     * @return the device name
     */
    public String getName(){
        return mAdvertise.getName();
    }//getName

    /**
     * get the device name with some product tag info (BLE Address)
     * @return the device friendly Name
     */
    public String getFriendlyName(){
        if (mFriendlyName == null) {
            String strTagClean = "NA";
            if (getTag() != null && getTag().length() > 0)
                strTagClean = getTag().replace(":", "");
            mFriendlyName =  getName() + " @" + strTagClean.substring(strTagClean.length() - Math.min(6, strTagClean.length()), strTagClean.length());
        }
        return mFriendlyName;
    }//getFriendlyName

    /**
     * get the device Protocol Version
     * @return the device Protocol Version
     */
    public short getProtocolVersion(){
        return mAdvertise.getProtocolVersion();
    } //getProtocolVersion

    /**
     * get a list of feature that the node export in the advertise, when the node is connected we
     * will know the feature exported thought the characteristics, so we enable some of them.
     * @return  list of feature that the node can export
     */
    public List<Feature> getFeatures(){
        return java.util.Collections.unmodifiableList(mAvailableFeature);
    }

    /**
     * return a list of feature of a specific type
     * @param type type of feature that we want search
     * @param <T> class that will want search
     * @return all the features of type {@code type} exported by this node, or an empty list
     */
    public @NonNull <T extends Feature> List<T> getFeatures(Class<T> type){
        List<T> temp = new ArrayList<>();
        for (Feature f : mAvailableFeature) {
            if (f.getClass().isAssignableFrom(type)) {
                @SuppressWarnings("unchecked") //we just  check that we can do the assign
                T feature = (T)f; //needed for suppress the warnings
                temp.add(feature);
            }//if
        }// for list
        return java.util.Collections.unmodifiableList(temp);
    }//getFeatures

    /**
     * search for a specific feature
     * @param type type of feature that we want search
     * @param <T> class that will want search
     * @return the feature of type {@code type} exported by this node, or null if not present
     */
    public @Nullable <T extends Feature> T getFeature(Class<T> type){
        List <T> allFeature = getFeatures(type);
        if(!allFeature.isEmpty())
            return allFeature.get(0);
        //else
        return null;
    }//getFeature

    /**
     * add the listener for the node status change
     * @param listener object where notify a node status change
     */
    public void addNodeStateListener(NodeStateListener listener){
        if (listener != null && !mStatusListener.contains(listener))
            mStatusListener.add(listener);
    }

    /**
     * remove the node state listener form the node
     * @param listener listener to remove
     */
    public void removeNodeStateListener(NodeStateListener listener){
        mStatusListener.remove(listener);
    }//removeNodeStateListener

    /**
     * find the the gattCharacteristics corresponding to a feature
     * @param feature feature to search
     * @return null if the feature is not handle by the node, the characteristics otherwise
     */
    private BluetoothGattCharacteristic getCorrespondingChar(Feature feature){
        ArrayList<BluetoothGattCharacteristic> candidateChar = new ArrayList<>();
        for (Map.Entry<BluetoothGattCharacteristic,List<Feature>> e: mCharFeatureMap.entrySet()){
            List<Feature> featureList = e.getValue();
            if(featureList.contains(feature)){
                candidateChar.add(e.getKey());
            }
        }//for entry
        if(candidateChar.isEmpty())
            return null;
        else if(candidateChar.size()==1){
            return candidateChar.get(0);
        }else{ //we have to select the feature that permit us to have more data
            int maxNFeature=0;
            BluetoothGattCharacteristic bestChar=null;
            for(BluetoothGattCharacteristic characteristic: candidateChar){
                int nFeature = mCharFeatureMap.get(characteristic).size();
                if(nFeature>maxNFeature){
                    maxNFeature=nFeature;
                    bestChar=characteristic;
                }//if
            }//for
            return bestChar;
        }//if-else
    }

    /**
     * async request for read a feature, the new value will be notify thought the feature listener
     * @see Feature.FeatureListener
     * @param feature to read
     * @return false if the feature is not handle by this node or disabled
     */
    public boolean readFeature(Feature feature){
        if(!feature.isEnabled())
            return false;
        final BluetoothGattCharacteristic characteristic = getCorrespondingChar(feature);
        if(!charCanBeRead(characteristic))
            return false;
        //since we have to wait that the write description are done, we have to wait a thread -> we
        //can not run directly in the bleThread, so we use the handler for the rssi, since the
        // load on that thread will be low
        mHandler.post(new Runnable() {
            @Override
            public void run() {
            final Runnable readChar = this;
            if (mConnection != null && isConnected() && !isPairing()) {
                //wait that the queue is empty and write the characteristics, if an error
                // happen we enqueue again the command
                waitCompleteAllDescriptorWriteRequest(new Runnable() {
                    @Override
                    public void run() {
                        if (!mConnection.readCharacteristic(characteristic))
                            mHandler.postDelayed(readChar, RETRY_COMMAND_DELAY_MS);
                    }//run
                });
            }else{
                mHandler.postDelayed(readChar, RETRY_COMMAND_DELAY_MS);
            }
        }//run
    });

        return true;
    }//readFeature

    /* standard descriptor id used for enable/disable the notification */
    private final static UUID NOTIFY_CHAR_DESC_UUID = UUID.fromString
            ("00002902-0000-1000-8000-00805f9b34fb");

    /**
     * wait until all the task inside the @{code mWriteDescQueue} aren't completed
     * don't call this method from the bleThread otherwise it will freeze the app!
     * @param runWhenEmpty code that this function will post on the bleThread when the queue is
     *                     empty
     */
    private void waitCompleteAllDescriptorWriteRequest(Runnable runWhenEmpty){
        synchronized (mWriteDescQueue){
            while(!mWriteDescQueue.isEmpty()) {
                try {
                    mWriteDescQueue.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }//try-catch
            }//while
            if(runWhenEmpty!=null)
                mBleThread.post(runWhenEmpty);
        }//synchronized
    }//waitCompleteAllDescriptorWriteRequest


    /**
     * take the fist element in the mWriteDescQueue and call the writeDescriptor, if it fail it
     * will retry after {@code RETRY_COMMAND_DELAY_MS} ms.
     */
    private Runnable mWriteDescriptorTask = new Runnable() {
        @Override
        public void run() {
        if(mConnection!=null &&  (isConnected() || mState==State.Disconnecting) && !isPairing()) {
            synchronized (mWriteDescQueue) {
                if(mConnection!=null && !mWriteDescQueue.isEmpty()) {
                    WriteDescCommand command = mWriteDescQueue.peek();
                    BluetoothGattDescriptor desc = command.desc;
                    desc.setValue(command.data);
                    if (Arrays.equals(desc.getValue(), BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE))
                        mConnection.setCharacteristicNotification(desc.getCharacteristic(), true);
                    if (Arrays.equals(desc.getValue(), BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE))
                        mConnection.setCharacteristicNotification(desc.getCharacteristic(), false);
                    if (!mConnection.writeDescriptor(desc))
                        mBleThread.postDelayed(this, RETRY_COMMAND_DELAY_MS);
                }//if size
            }//synchronized
        }else{ //we can execute the task, postpone it
            mBleThread.postDelayed(this, RETRY_COMMAND_DELAY_MS);
        }
    }//run
};


    /**
     * add a command to the queue of writes, this function automatically start to write the
     * descriptor
     * @param command descriptor and data to write, we have to keep it separate for avoid to
     *                lost message writing multiple time in the same descriptor
     */
    private void enqueueWriteDesc(final WriteDescCommand command){
        synchronized (mWriteDescQueue){
            mWriteDescQueue.add(command);
            //if the queue contains only the element that we just add
            if(mWriteDescQueue.size()==1) {
                mBleThread.post(mWriteDescriptorTask);
            }//if
        }//synchronized
    }//enqueueWriteDesc

    /**
     * remove the descriptor from the queue and if present start a new write
     * @param command that is completed
     */
    private void dequeueWriteDesc(WriteDescCommand command){
        synchronized (mWriteDescQueue){
            if(!mWriteDescQueue.remove(command))
                Log.e(TAG,"No WriteDescCommand removed");
            //if there still element in the queue, start write the head
            if(!mWriteDescQueue.isEmpty()) {
                mBleThread.post(mWriteDescriptorTask);
            }else
                mWriteDescQueue.notifyAll();
        }//synchronized
    }//dequeueWriteDesc


    /**
     * take the fist element in the mCharacteristicWriteQueue and call the writeCharacteristics
     * note that the characteristic are write only when we write all the descriptor
     */
    private Runnable mWriteFeatureCommandTask = new Runnable() {
        @Override
        public void run() {
            if(mConnection!=null &&  isConnected() && !isPairing()) {
                synchronized (mCharacteristicWriteQueue) {
                    if(!mCharacteristicWriteQueue.isEmpty()) {
                        WriteCharCommand command = mCharacteristicWriteQueue.peek();
                        command.characteristic.setValue(command.data);
                        writeCharacteristics(command.characteristic);
                    }//if size
                }//synchronized
            }else{ //we can execute the task, postpone it
                mBleThread.postDelayed(this, RETRY_COMMAND_DELAY_MS);
            }
        }//run
    };


    /**
     * ad a command to the queue of work to do
     * @param command characteristic and data to write
     */
    private void enqueueCharacteristicsWrite(final WriteCharCommand command){
        synchronized (mCharacteristicWriteQueue){
            mCharacteristicWriteQueue.add(command);
            //if the queue contains only the element that we just add
            if(mCharacteristicWriteQueue.size()==1) {
                mBleThread.post(mWriteFeatureCommandTask);
            }//if
        }//synchronized
    }//enqueueWriteDesc

    void enqueueCharacteristicsWrite(BluetoothGattCharacteristic characteristic, byte
            data[]){
        enqueueCharacteristicsWrite(new WriteCharCommand(characteristic,data));
    }

    /**
     * remove a command when complited
     * @param writeOp command completed
     */
    private void dequeueCharacteristicsWrite(final WriteCharCommand writeOp){
        synchronized (mCharacteristicWriteQueue){
            if(!mCharacteristicWriteQueue.contains(writeOp))
                return;
            mCharacteristicWriteQueue.remove(writeOp);
            //if there still element in the queue, start write the head
            if(!mCharacteristicWriteQueue.isEmpty()) {
                mBleThread.post(mWriteFeatureCommandTask);
            }
        }//synchronized
    }//dequeueWriteDesc



    /**
     * send a request for enable/disable the notification update on a specific characteristics
     * @param characteristic characteristics to notify
     * @param enable true if you want enable the notification, false if you want disable it
     * @return true if the request is correctly send, false otherwise
     */
    boolean changeNotificationStatus(BluetoothGattCharacteristic characteristic,
                                          boolean enable){

        if(charCanBeNotify(characteristic) && mConnection!=null && isConnected()){
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(NOTIFY_CHAR_DESC_UUID);

            if(descriptor==null)
                return false;

            WriteDescCommand command= new WriteDescCommand(descriptor,
                    enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);

            enqueueWriteDesc(command);

            return true;
        }else
            return false;
    }

    /**
     * unsubscribe the notification for the node update of the feature
     * @param feature feature that you want stop to be notify
     * @return false if the feature is not handle by this node or disabled
     */
    public boolean disableNotification(Feature feature){
        if(!feature.isEnabled() && feature.getParentNode()!=this)
            return false;
        BluetoothGattCharacteristic featureChar =getCorrespondingChar(feature);
        if(charCanBeNotify(featureChar)) {
            mNotifyFeature.remove(feature);
            return changeNotificationStatus(getCorrespondingChar(feature),false);
        }//
        return false;
    }//disableNotification

    /**
     * ask to the node to notify when the feature change its value
     * @param feature feature to look
     * @return false if the feature is not handle by this node or disabled
     */
    public boolean enableNotification(Feature feature){
        if(!feature.isEnabled() && feature.getParentNode()!=this)
            return false;
        BluetoothGattCharacteristic featureChar =getCorrespondingChar(feature);
        if(charCanBeNotify(featureChar)) {
            mNotifyFeature.add(feature);
            return changeNotificationStatus(featureChar, true);
        }
        return false;
    }//enableNotification

    /**
     * tell if the user ask for receive notification update about this feature
     * @param feature feature that you want know if is in notification mode
     * @return true if the use call enableNotification on this feature
     */
    public boolean isEnableNotification(Feature feature){
        return mNotifyFeature.contains(feature);
    }


    /**
     * enqueue the write command for be execute in the bleThread and only if we already do all the
     * writeDescription request
     * @param writeMe characteristics that we have to write
     */
    private void writeCharacteristics(final BluetoothGattCharacteristic writeMe){
        //since we have to wait that the write description are done, we have to wait a thread -> we
        //can not run directly in the bleThread, so we use the handler for the rssi, since the load
        // on that thread will be low
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                final Runnable writeChar = this;
                if(mConnection!=null &&  isConnected()&& ! isPairing()) {
                    //wait that the queue is empty and write the characteristics, if an error
                    // happen we enqueue again the command
                    waitCompleteAllDescriptorWriteRequest(new Runnable() {
                        @Override
                        public void run() {
                            if (!mConnection.writeCharacteristic(writeMe)) {
                                mHandler.postDelayed(writeChar, RETRY_COMMAND_DELAY_MS);
                            }//if
                        }//run
                    });
                }else{
                    mHandler.postDelayed(writeChar, RETRY_COMMAND_DELAY_MS);
                }//if-else
            }//run
        });
    }

    /**
     * write some data to a feature characteristics, this method is accessible only by the
     * feature that know how the data interpreted in the node.
     * @param feature feature that will receive the data
     * @param data data that we have to send to the feature
     * @return true if the message is send without problem, false otherwise
     */
    boolean writeFeatureData(Feature feature,byte data[]){
        final BluetoothGattCharacteristic characteristic = getCorrespondingChar(feature);
        //not enable or not exist or not in write mode -> return false
        if(!charCanBeWrite(characteristic) || !feature.isEnabled())
            return false;

        enqueueCharacteristicsWrite(new WriteCharCommand(characteristic, data));

        return true;
    }//writeFeatureData

    /**
     * compare two nodes. two nodes are equals if the tag function return the same value
     * @param node object to compare
     * @return true both object have the same tag value
     */
    @Override
    public boolean equals(Object node) {
        return (node instanceof Node) && (node == this || getTag().equals(((Node) node).getTag()));
    }//equals

    /**
     * the node transmission power, in mdb
     * @return transmission power in mdb
     */
    public int getTxPowerLevel(){
        return mAdvertise.getTxPower();
    }//getTxPowerLevel

    /**
     * return the most recent value of rssi, for have feash data use readRssi and wait the answer
     * from the onRSSIChanged callback
     * @return last know rssi value
     * @see com.st.BlueSTSDK.Node.BleConnectionParamUpdateListener
     */
    public int getLastRssi(){
        return mRssi;
    }

    /**
     * request for an async read of the rssi, the value will be returned using the
     * {@link com.st.BlueSTSDK.Node.BleConnectionParamUpdateListener#onRSSIChanged(Node, int)} callback
     */
    public void readRssi(){
        if(mBleThread!=null)
            mBleThread.post(mUpdateRssiTask);
    }//readRssi


    /**
     * return the moment of the last rssi update that we received
     * @return date of the last rssi that we received
     */
    public Date getLastRssiUpdateDate(){
        return mLastRssiUpdate;
    }


    /**
     * set the class where do the callback in case the rssi or txpower will change.
     * <p> only one class at time can receive the notification</p>
     * @param listener class where do the callback
     */
    public void addBleConnectionParamListener(BleConnectionParamUpdateListener listener){
        if(listener!=null && !mBleConnectionListeners.contains(listener))
            mBleConnectionListeners.add(listener);
    }

    /**
     * remove the ble connection listener from this node
     * @param listener listener to remove
     */
    public void removeBleConnectionParamListener(BleConnectionParamUpdateListener listener){
        mBleConnectionListeners.remove(listener);
    }

    /**
     * function call when the manager receive a new advertise by this node
     * @param rssi rssi of the last advertise
     */
    void isAlive(int rssi){
        //remove the set lost task
        if(mHandler!=null) mHandler.removeCallbacks(mSetNodeLost);
        updateRssi(rssi);
        //start a new set lost task
        if(mHandler!=null) mHandler.postDelayed(mSetNodeLost,NODE_LOST_TIMEOUT_MS);

    }//isAlive

    /**
     * crate the package to send to the command characteristics
     * @param mask destination feature
     * @param type command type
     * @param data command parameters
     * @return data to send to the characteristics
     */
    private byte[] packageCommandData(int mask,byte type,byte data[]){
        byte calibPackage[] = new byte[data.length+4+1]; //4=sizeof(int) + 1 for the req type
        byte maskArray[] = NumberConversion.BigEndian.int32ToBytes(mask);
        System.arraycopy(maskArray, 0, calibPackage, 0, maskArray.length);
        calibPackage[4]=type;
        System.arraycopy(data, 0, calibPackage, 4 + 1, data.length);
        return calibPackage;
    }

    /**
     * Send a command to a feature, the command will be write into the
     * {@link com.st.BlueSTSDK.Utils.BLENodeDefines.Services.Config#FEATURE_COMMAND_UUID}
     * If not present the data will be write into the characteristic that export the feature data.
     * sending command to a general purpose feature is not supported
     * The command format is [feature mask (4byte) + type + data], if the command is send directly
     * to the feature the feature mask is omitted.
     *
     * @param feature destination feature
     * @param type command type
     * @param data command parameters
     * @return true if the message is correctly send, false otherwise
     */
    boolean sendCommandMessage(Feature feature,byte type,byte data[]) {
        if (feature instanceof FeatureGenPurpose)
            return false;

        final BluetoothGattCharacteristic characteristic = getCorrespondingChar(feature);
        final BluetoothGattCharacteristic writeTo = mFeatureCommand != null ? mFeatureCommand :
                characteristic;

        if (writeTo == null || characteristic == null || !charCanBeWrite(writeTo))
            return false;

        if(writeTo==mFeatureCommand) {
            int mask = BLENodeDefines.FeatureCharacteristics.extractFeatureMask(characteristic.getUuid());
            enqueueCharacteristicsWrite(
                    new WriteCharCommand(mFeatureCommand, packageCommandData(mask, type, data)));
        }else{ //fail back write directly to the feature characteristics
            byte dataToWrite[] = new byte[1+data.length];
            dataToWrite[0]=type;
            System.arraycopy(data,0,dataToWrite,1,data.length);
            enqueueCharacteristicsWrite(new WriteCharCommand(writeTo, dataToWrite));
        }//if-else

        return true;
    }//sendCommandMessage


    /***
     * tell if the node is pair with the device
     * @return true if the node is bonded with the device
     */
    boolean isBounded(){
        return mDevice.getBondState()==BluetoothDevice.BOND_BONDED;
    }

    /**
     * get the class that can be used for write to the node serial console
     * @return object used for send/receive message to/from serial console, null if the service
     * is not available
     */
    public @Nullable Debug getDebug(){return mDebugConsole; }//getDebug

    /**
     * get the class that permit to read/write the configuration register
     * @return null if the configuration service is not available or the class
     */
    public @Nullable ConfigControl getConfigRegister(){return mConfigControl; } //getConfigRegister

}//Node