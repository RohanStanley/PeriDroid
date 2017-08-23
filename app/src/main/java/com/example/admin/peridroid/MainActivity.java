package com.example.admin.peridroid;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorEventListener;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.hardware.SensorEvent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;
import android.os.Vibrator;

import org.w3c.dom.Text;


public class MainActivity extends AppCompatActivity implements SensorEventListener{


    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private float x , y,  z;
    private TextView testField;
    public BluetoothSocket bluetoothSocket;
    private BluetoothAdapter bluetoothAdapter;
    private Button connectButton , lClick, rClick;
    private ProgressBar connectProgress;
    private static final String TAG = "PeriDroid";
    final int RECIEVE_MESSAGE = 1;
    private ConnectedThread mConnectedThread;
    // SPP UUID service
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Bluetooth module (you must edit this line)
    public static String address = "98:D3:31:20:04:3E";
    public boolean connectedStatus = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectButton = (Button) findViewById(R.id.connectButton);
        testField = (TextView) findViewById(R.id.testField);
        lClick = (Button)findViewById(R.id.LC);
        rClick = (Button)findViewById(R.id.RC);
        findViewById(R.id.connectProgress).setVisibility(View.GONE);
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this ,senAccelerometer ,SensorManager.SENSOR_DELAY_NORMAL);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        testField.setVisibility(View.INVISIBLE);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (bluetoothAdapter.isEnabled()) {
                    testField.setVisibility(View.GONE);
                    connectButton.setVisibility(View.GONE);
                    findViewById(R.id.connectProgress).setVisibility(View.VISIBLE);
                    // Set up a pointer to the remote node using it's address.
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);

                    // Two things are needed to make a connection:
                    //   A MAC address, which we got above.
                    //   A Service ID or UUID.  In this case we are using the
                    //     UUID for SPP.

                    try {
                        bluetoothSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
                    }
    /*try {
      btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
    } catch (IOException e) {
      errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
    }*/
                    // Discovery is resource intensive.  Make sure it isn't going on
                    // when you attempt to connect and pass your message.
                    bluetoothAdapter.cancelDiscovery();

                    // Establish the connection.  This will block until it connects.
                    Log.d(TAG, "...Connecting...");
                    try {
                        bluetoothSocket.connect();
                        connectedStatus = true;
                        Log.d(TAG, "....Connection ok...");
                    } catch (IOException e) {
                        try {
                            bluetoothSocket.close();
                        } catch (IOException e2) {
                            errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
                        }
                    }

                    // Create a data stream so we can talk to server.
                    Log.d(TAG, "...Create Socket...");

                    mConnectedThread = new ConnectedThread(bluetoothSocket);
                    mConnectedThread.start();

                    findViewById(R.id.connectProgress).setVisibility(View.GONE);
                    testField.setVisibility(View.VISIBLE);
                    connectButton.setVisibility(View.VISIBLE);

                }


            }
        });



        lClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(connectedStatus){
                    mConnectedThread.write("lClick\n");
                }

            }
        });

        rClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (connectedStatus) {
                    mConnectedThread.write("rClick\n");
                }

            }
        });

        lClick.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                // Get instance of Vibrator from current Context
                Vibrator x = (Vibrator) getSystemService(getApplicationContext().VIBRATOR_SERVICE);

                // Vibrate for 400 milliseconds
                x.vibrate(400);

                if (connectedStatus) {
                    mConnectedThread.write("dClick\n");
                }

                return true;
            }
        });


    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
             x = sensorEvent.values[0];
             y = sensorEvent.values[1];
             z = sensorEvent.values[2];

            testField.setText(String.format("x = %.2f \ny = %.2f \nz = %.2f",x,y,z) );


            if(connectedStatus){
                mConnectedThread.write(String.format("%.2f>>>%.2f>>>%.2f\n", x, y, z));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent x = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(x);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);		// Get number of bytes and message in "buffer"
                    //h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();		// Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }


    public BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, uuid);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection",e);
            }
        }
        return  device.createRfcommSocketToServiceRecord(uuid);
    }


}
