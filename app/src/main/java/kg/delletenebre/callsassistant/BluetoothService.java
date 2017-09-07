package kg.delletenebre.callsassistant;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import kg.delletenebre.callsassistant.utils.Debug;

public class BluetoothService {

    // Current connection state
    private enum BluetoothState {
        NONE,       // we're doing nothing
        LISTEN,     // now listening for incoming connections
        CONNECTING, // now initiating an outgoing connection
        CONNECTED   // now connected to a remote device
    }

    protected final String CONNECTION_NAME = "ABCDABCD-ABCD-ABCD-ABCD-6924A0EC8C25"; //"2563EB62-2E00-4760-AA7C-76BE833F4ADF";
    protected final UUID CONNECTION_UUID = UUID.fromString(CONNECTION_NAME);

    private final BluetoothAdapter mBluetoothAdapter;
    private BluetoothState mState;

    private ServerThread mServerThread;
    private ClientThread mClientThread;
    private ConnectedThread mConnectedThread;


    // Listener for Bluetooth Status & Connection
    private StateListener mStateListener = null;
    private OnDataReceivedListener mDataReceivedListener = null;
    private ConnectionListener mConnectionListener = null;
    //private AutoConnectionListener autoConnectionListener = null;




    BluetoothService() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = BluetoothState.NONE;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(BluetoothState state) {
        Debug.log("setState() " + mState + " -> " + state);

        if (mState == BluetoothState.CONNECTED && state != BluetoothState.CONNECTED) {
            if (mConnectionListener != null) {
                mConnectionListener.onDeviceDisconnected();
            }
        }

        if (mState == BluetoothState.CONNECTING && state != BluetoothState.CONNECTED) {
            if (mConnectionListener != null) {
                mConnectionListener.onDeviceConnectionFailed();
            }
        }

        mState = state;

        if (mStateListener != null) {
            mStateListener.onStateChanged(state);
        }
    }

    /**
     * Return the current connection state.
     */
    public synchronized BluetoothState getState() {
        return mState;
    }

    synchronized void startWaitingConnections() {
        Debug.info("start");

        // Cancel any thread attempting to make a connection
        if (mClientThread != null) {
            mClientThread.cancel();
            mClientThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (isEnabled()) {
            // Start the thread to listen on a BluetoothServerSocket
            if (mServerThread == null) {
                mServerThread = new ServerThread();
                mServerThread.start();
            }
        } else {
            Debug.log("Bluetooth not available or not enabled");
        }
    }

    synchronized void connectAndSend(BluetoothDevice device, String data) {
        // Cancel any thread attempting to make a connection
        if (mState == BluetoothState.CONNECTING) {
            if (mClientThread != null) {
                mClientThread.cancel();
                mClientThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (isEnabled()) {
            // Start the thread to connect with the given device
            mClientThread = new ClientThread(device, data);
            mClientThread.start();
        }
    }
    synchronized void connectAndSend(String deviceAddress, String data) {
        if (isEnabled() && !deviceAddress.isEmpty()
                && BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
            if (device != null) {
                connectAndSend(device, data);
            }
        }
    }

    private synchronized void connected(BluetoothSocket socket, String data) {
        Debug.log("connected");

        // Cancel the thread that completed the connection
        if (mClientThread != null) {
            mClientThread.cancel();
            mClientThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mServerThread != null) {
            mServerThread.cancel();
            mServerThread = null;
        }

        if (isEnabled()) {
            // Start the thread to manage the connection and perform transmissions
            mConnectedThread = new ConnectedThread(socket, data);
            mConnectedThread.start();
        }
    }

    /**
     * Stop all threads
     */
    synchronized void stop() {
        Debug.info("stop");

        if (mClientThread != null) {
            mClientThread.cancel();
            mClientThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mServerThread != null) {
            mServerThread.cancel();
            mServerThread = null;
        }

        setState(BluetoothState.NONE);
    }

    private void connectionFailed() {
        startWaitingConnections();
    }

    private void connectionLost() {
        startWaitingConnections();
    }

    private class ServerThread extends Thread {
        private final BluetoothServerSocket bluetoothServerSocket;

        ServerThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
                        CONNECTION_NAME, CONNECTION_UUID);
            } catch (IOException e) {
                Debug.error("listenUsingRfcommWithServiceRecord() failed");
            }
            bluetoothServerSocket = tmp;
        }

        public void run() {
            setState(BluetoothState.LISTEN);

            BluetoothSocket socket = null;
            while (!isInterrupted()) {
                try {
                    socket = bluetoothServerSocket.accept();
                } catch (IOException e) {
                    interrupt();
                }
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                            case LISTEN:
                            case CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, "");
                                break;
                            case NONE:
                            case CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Debug.error("Could not close unwanted socket");
                                }
                                break;
                        }
                    }

                }
            }
            Debug.info("Server stopped");
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        void cancel() {
            try {
                bluetoothServerSocket.close();
                interrupt();
            } catch (IOException e) {
                Debug.error("Failed to stop ServerThread");
            }
        }
    }

    private class ClientThread extends Thread {
        private BluetoothSocket mBluetoothSocket;
        private final BluetoothDevice mBluetoothDevice;
        private final String mData;

        ClientThread(BluetoothDevice device, String data) {
            mBluetoothDevice = device;
//            BluetoothSocket tmp = null;
//            try {
//                tmp = mBluetoothDevice.createRfcommSocketToServiceRecord(CONNECTION_UUID);
//            } catch (IOException e) {
//                Debug.error("createRfcommSocketToServiceRecord() failed");
//            }
//            mBluetoothSocket = tmp;
            mData = data;
        }

        public void run() {
            Debug.info("BEGIN ClientThread");

            setState(BluetoothState.CONNECTING);
            mBluetoothAdapter.cancelDiscovery();

            boolean connected = false;
            int attempt = 1;
            while (!connected) {
                if (isInterrupted() || attempt > 3) {
                    break;
                }
                try {
                    mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(CONNECTION_UUID);
                    mBluetoothSocket.connect();
                    connected = mBluetoothSocket.isConnected();
                } catch (Exception e) {
                    try {
                        if (mBluetoothSocket != null) {
                            mBluetoothSocket.close();
                        }
                        synchronized (this) {
                            sleep(500);
                        }
                    } catch (Exception e1) {
                        // nothing
                    }
                }
                attempt++;
            }

//            try {
//                mBluetoothSocket.connect();
//            } catch (IOException e) {
//                try {
//                    mBluetoothSocket.close();
//                } catch (IOException e2) {
//                    Debug.error("unable to close() socket during connection failure");
//                }
//                connectionFailed();
//                return;
//            }

            synchronized (BluetoothService.this) {
                mClientThread = null;
            }

            if (connected) {
                connected(mBluetoothSocket, mData);
            } else {
                connectionFailed();
            }
        }

        void cancel() {
            try {
                mBluetoothSocket.close();
                interrupt();
            } catch (IOException e) {
                Debug.error("close() of connect socket failed");
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mBluetoothSocket;
        private final InputStream mInputStream;
        private final OutputStream mOutputStream;
        private final String mDeviceAddress;
        private String mData;


        ConnectedThread(BluetoothSocket socket, String data) {
            Debug.log("create ConnectedThread");
            mBluetoothSocket = socket;
            InputStream tempInput = null;
            OutputStream tempOutput = null;
            String tempDeviceAddress = null;
            try {
                tempInput = socket.getInputStream();
                tempOutput = socket.getOutputStream();
                tempDeviceAddress = socket.getRemoteDevice().getAddress();
            } catch (IOException e) {
                Debug.error("temp sockets not created");
            }
            mInputStream = tempInput;
            mOutputStream = tempOutput;
            mDeviceAddress = tempDeviceAddress;

            mData = data;
        }

        public void run() {
            Debug.info("BEGIN connectedThread");

            setState(BluetoothState.CONNECTED);

            if (mConnectionListener != null) {
                mConnectionListener.onDeviceConnected(mBluetoothSocket.getRemoteDevice().getName(),
                        mBluetoothSocket.getRemoteDevice().getAddress());
            }

            if (!mData.isEmpty()) {
                mData += "\r\n";
                write(mData.getBytes());
            }

            StringBuilder sb = new StringBuilder();

            // Keep listening to the InputStream while connected
            while (!isInterrupted() && mState == BluetoothState.CONNECTED) {
                try {
                    int data = mInputStream.read();
                    if (data > -1) {
                        if (data == 0x0D || data == 0x0A) {
                            if (sb.length() > 0) {
                                if (mDataReceivedListener != null) {
                                    mDataReceivedListener.onDataReceived(sb.toString(), mDeviceAddress);
                                }

                                sb = new StringBuilder();
                            }
                        } else {
                            sb.append((char) data);
                        }
                    }
                } catch (IOException e) {
                    Debug.error("disconnected");

                    connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothService.this.startWaitingConnections();

                    interrupt();
                }
            }

            try {
                mInputStream.close();
                mOutputStream.close();
            } catch (IOException e) {
                Debug.error("inputStream.close() or outputStream.close() failed");
            }

            Debug.info("END connectedThread");
        }

        void write(byte[] buffer) {
            Debug.log("Trying to write");
            try {
                mOutputStream.write(buffer);
            } catch (IOException e) {
                Debug.error("Exception during write");
            }
        }

        void cancel() {
            try {
                mBluetoothSocket.close();
                interrupt();
            } catch (IOException e) {
                Debug.error("close() of connect socket failed");
            }
        }
    }



    interface OnDataReceivedListener {
        void onDataReceived(String message, String deviceAddress);
    }

    interface ConnectionListener {
        void onDeviceConnected(String name, String address);
        void onDeviceDisconnected();
        void onDeviceConnectionFailed();
    }

    interface StateListener {
        void onStateChanged(BluetoothState state);
    }

    void setOnDataReceivedListener(OnDataReceivedListener listener) {
        mDataReceivedListener = listener;
    }

    public void setConnectionListener (ConnectionListener listener) {
        mConnectionListener = listener;
    }

    private boolean isAvailable() {
        return mBluetoothAdapter != null;
    }

    private boolean isEnabled() {
        return isAvailable() && mBluetoothAdapter.isEnabled();
    }
}


//import android.app.Service;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothServerSocket;
//import android.bluetooth.BluetoothSocket;
//import android.content.Intent;
//import android.os.IBinder;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.util.UUID;
//
//public class BluetoothService {
//
//    private enum BluetoothState {
//        NONE,       // we're doing nothing
//        LISTEN,     // now listening for incoming connections
//        CONNECTING, // now initiating an outgoing connection
//        CONNECTED   // now connected to a remote device
//    }
//
//    protected static final String CONNECTION_NAME = "ABCDABCD-ABCD-ABCD-ABCD-6924A0EC8C25"; //"2563EB62-2E00-4760-AA7C-76BE833F4ADF";
//    protected static final UUID CONNECTION_UUID = UUID.fromString(CONNECTION_NAME);
//
//    private final BluetoothAdapter mBluetoothAdapter;
//    private BluetoothState mState;
//
//    private WaitForConnectionsThread mWaitForConnectionsThread;
//    private TryingToConnectThread mTryingToConnectThread;
//    private ConnectedThread mConnectedThread;
//
//    private StateListener mStateListener = null;
//    private OnDataReceivedListener mDataReceivedListener = null;
//
//
//    public BluetoothService() {
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        mState = BluetoothState.NONE;
//    }
//
//    private synchronized void setState(BluetoothState state) {
//        Debug.log("setState() " + mState + " -> " + state);
//
//        mState = state;
//        if (mStateListener != null) {
//            mStateListener.onStateChanged(state);
//        }
//    }
//
//    public synchronized BluetoothState getState() {
//        return mState;
//    }
//
//    public synchronized void startWaitingConnections() {
//        stop();
//        if (isEnabled()) {
//            mWaitForConnectionsThread = new WaitForConnectionsThread();
//            mWaitForConnectionsThread.start();
//        } else {
//            Debug.log("Bluetooth not available or not enabled");
//        }
//    }
//
//    synchronized void connectAndSend(BluetoothDevice device, String data) {
//        stop();
//        if (isEnabled()) {
//            mTryingToConnectThread = new TryingToConnectThread(device, data);
//            mTryingToConnectThread.start();
//        }
//    }
//    synchronized void connectAndSend(String deviceAddress, String data) {
//        if (isEnabled() && !deviceAddress.isEmpty()
//                && BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
//            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
//            if (device != null) {
//                connectAndSend(device, data);
//            }
//        }
//    }
//
//    public synchronized void connected(BluetoothSocket socket, String data) {
//        stop();
//        if (isEnabled()) {
//            mConnectedThread = new ConnectedThread(socket, data);
//            mConnectedThread.start();
//        }
//    }
//
//    public synchronized void stop() {
//        if (mTryingToConnectThread != null) {
//            mTryingToConnectThread.cancel();
//            mTryingToConnectThread = null;
//        }
//        if (mConnectedThread != null) {
//            mConnectedThread.cancel();
//            mConnectedThread = null;
//        }
//        if (mWaitForConnectionsThread != null) {
//            mWaitForConnectionsThread.cancel();
//            mWaitForConnectionsThread = null;
//        }
//        setState(BluetoothState.NONE);
//    }
//
//    private void connectionLost() {
//        startWaitingConnections();
//    }
//
//
//    private class WaitForConnectionsThread extends Thread {
//        private final BluetoothServerSocket bluetoothServerSocket;
//
//        WaitForConnectionsThread() {
//            BluetoothServerSocket tmp = null;
//            try {
//                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
//                        CONNECTION_NAME, CONNECTION_UUID);
//            } catch (IOException e) {
//                Debug.error("listenUsingRfcommWithServiceRecord() failed");
//            }
//            bluetoothServerSocket = tmp;
//        }
//
//        public void run() {
//            setState(BluetoothState.LISTEN);
//
//            BluetoothSocket socket = null;
//            while (!isInterrupted()) {
//                try {
//                    socket = bluetoothServerSocket.accept();
//                } catch (IOException e) {
//                    interrupt();
//                }
//                if (socket != null) {
//                    synchronized (BluetoothService.this) {
//                        switch (mState) {
//                            case LISTEN:
//                            case CONNECTING:
//                                connected(socket, "");
//                                break;
//                            case NONE:
//                            case CONNECTED:
//                                try {
//                                    socket.close();
//                                } catch (IOException e) {
//                                    Debug.error("Could not close unwanted socket");
//                                }
//                                break;
//                        }
//                    }
//                }
//            }
//            Debug.info("Server stopped");
//        }
//        void cancel() {
//            try {
//                bluetoothServerSocket.close();
//            } catch (IOException e) {
//                Debug.error("Failed to stop WaitForConnectionsThread");
//            }
//            interrupt();
//        }
//    }
//
//    private class TryingToConnectThread extends Thread {
//        private BluetoothSocket mBluetoothSocket;
//        private BluetoothDevice mBluetoothDevice;
//        private String mData;
//
//        TryingToConnectThread(BluetoothDevice device, String data) {
//            mBluetoothDevice = device;
//            mData = data;
//        }
//
//        public void run() {
//            Debug.info("BEGIN TryingToConnectThread");
//            setState(BluetoothState.CONNECTING);
//            mBluetoothAdapter.cancelDiscovery();
//
//            boolean connected = false;
//            int attempt = 1;
//            while (!connected) {
//                if (isInterrupted() || attempt > 3) {
//                    break;
//                }
//                try {
//                    mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(CONNECTION_UUID);
//                    mBluetoothSocket.connect();
//                    connected = mBluetoothSocket.isConnected();
//                } catch (Exception e) {
//                    try {
//                        if (mBluetoothSocket != null) {
//                            mBluetoothSocket.close();
//                        }
//                        synchronized (this) {
//                            sleep(500);
//                        }
//                    } catch (Exception e1) {
//                        // nothing
//                    }
//                }
//                attempt++;
//            }
//
//            if (connected) {
//                connected(mBluetoothSocket, mData);
//            } else {
//                startWaitingConnections();
//            }
//        }
//
//        void cancel() {
//            try {
//                if (mBluetoothSocket != null) {
//                    mBluetoothSocket.close();
//                    mBluetoothSocket = null;
//                }
//                interrupt();
//            } catch (IOException e) {
//                Debug.log("BluetoothService: Connect socket close FAIL");
//            }
//
//            setState(BluetoothState.NONE);
//            mBluetoothDevice = null;
//        }
//    }
//
//
//    /**
//     * This thread runs during a connection with a remote device.
//     * It handles all incoming and outgoing transmissions.
//     */
//    private class ConnectedThread extends Thread {
//        private BluetoothSocket mBluetoothSocket;
//        private InputStream mInputStream;
//        private OutputStream mOutputStream;
//        private String mData;
//
//        ConnectedThread(BluetoothSocket socket, String data) {
//            Debug.log("create ConnectedThread");
//            mBluetoothSocket = socket;
//
//            // Get the BluetoothSocket input and output streams
//            try {
//                mInputStream = socket.getInputStream();
//                mOutputStream = socket.getOutputStream();
//            } catch (IOException e) {
//                Debug.error("temp sockets not created");
//            }
//
//            mData = data;
//        }
//
//        public void run() {
//            Debug.info("BEGIN connectedThread");
//
//            setState(BluetoothState.CONNECTED);
//
//            if (!mData.isEmpty()) {
//                mData += "\r\n";
//                write(mData.getBytes());
//            }
//
//            StringBuilder sb = new StringBuilder();
//
//            while (!isInterrupted() && mState == BluetoothState.CONNECTED) {
//                try {
//                    int data = mInputStream.read();
//                    if (data > -1) {
//                        if (data == 0x0D || data == 0x0A) {
//                            if (sb.length() > 0) {
//                                if (mDataReceivedListener != null) {
//                                    mDataReceivedListener.onDataReceived(sb.toString());
//                                }
//
//                                sb = new StringBuilder();
//                            }
//                        } else {
//                            sb.append((char) data);
//                        }
//                    }
//                } catch (IOException e) {
//                    Debug.error("disconnected");
//                    e.printStackTrace();
//                    connectionLost();
//                    // Start the service over to restart listening mode
//                    BluetoothService.this.startWaitingConnections();
//
//                    interrupt();
//                }
//            }
//
//            try {
//                if (mInputStream != null) {
//                    mInputStream.close();
//                }
//                if (mOutputStream != null) {
//                    mOutputStream.close();
//                }
//            } catch (IOException e) {
//                Debug.error("inputStream.close() or outputStream.close() failed");
//            }
//
//            Debug.info("END connectedThread");
//        }
//
//        void write(byte[] buffer) {
//            Debug.log("Trying to write");
//            try {
//                mOutputStream.write(buffer);
//            } catch (IOException e) {
//                Debug.error("Exception during write");
//                e.printStackTrace();
//            }
//        }
//
//        void cancel() {
//            try {
//                if (mInputStream != null) {
//                    mInputStream.close();
//                    mInputStream = null;
//                }
//                if (mOutputStream != null) {
//                    mOutputStream.close();
//                    mOutputStream = null;
//                }
//            } catch (IOException e) {
//                Debug.log("inputStream.close() or outputStream.close() FAIL");
//            }
//
//            try {
//                if (mBluetoothSocket != null) {
//                    mBluetoothSocket.close();
//                    mBluetoothSocket = null;
//                }
//            } catch (IOException e) {
//                Debug.log("BluetoothService: Connect socket close FAIL");
//            }
//
//            setState(BluetoothState.NONE);
//            interrupt();
//        }
//    }
//
//    interface OnDataReceivedListener {
//        void onDataReceived(String message);
//    }
//
//    interface StateListener {
//        void onStateChanged(BluetoothState state);
//    }
//
//    public void setOnDataReceivedListener (OnDataReceivedListener listener) {
//        mDataReceivedListener = listener;
//    }
//
//    private boolean isEnabled() {
//        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
//    }
//
//}
