package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.widget.Button;
import android.widget.TextView;
import android.telephony.TelephonyManager;
import android.os.AsyncTask;

import java.io.BufferedReader;

import java.io.DataOutputStream;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;

import java.net.Socket;
import java.net.UnknownHostException;
import java.io.FileOutputStream;

import android.content.Context;

import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.net.ServerSocket;
import java.util.concurrent.locks.ReentrantLock;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";

    static final int SERVER_PORT = 10000;
    static int messageCount = 0;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    static  Uri mUri;

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");


        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */




        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        Log.v("myPort"+myPort, "myPort number is ");
       try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
           Log.v("onCreate", "Checking server socket before ");
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Log.v("onCreate", "Checking server socket ");
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {

           /*  * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.v("OnCreate", "Can't create a ServerSocket "+e);
            return;
        }



        /*
         * Register an OnKeyListener for the input box. OnKeyListener is an event handler that
         * processes each key event. The purpose of the following code is to detect an enter key
         * press event, and create a client thread so that the client thread can send the string
         * in the input box over the network.
         */
        Log.v("OnCreate","Before on Click Listener");
        final Button button = (Button) findViewById(R.id.button4);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.v("OnCreate","Checking Click button");
                final EditText editText = (EditText) findViewById(R.id.editText1);
                Log.v("OnCreate","Checking edit text button");
                /*
                 * If the key is pressed (i.e., KeyEvent.ACTION_DOWN) and it is an enter key
                 * (i.e., KeyEvent.KEYCODE_ENTER), then we display the string. Then we create
                 * an AsyncTask that sends the string to the remote AVD.
                 */
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.local_text_display);
                localTextView.append("\t" + msg); // This is one way to display a string.
                TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
                remoteTextView.append("\n");

                /*
                 * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                 * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                 * the difference, please take a look at
                 * http://developer.android.com/reference/android/os/AsyncTask.html
                 */
                Log.v("OnCreate","Checking Client Task side");

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                return;

            }
        });





    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        private ReentrantLock pauseLock = new ReentrantLock();
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            try {
                while (true) {
                    pauseLock.lock();
                    Log.v("ServerSide", "Lock Applied");
                    String message;
                    Socket clientSocket = serverSocket.accept();
                    //DataInputStream msgFromClient = new DataInputStream(clientSocket.getInputStream());
                    BufferedReader msgFromClient
                            = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    message = msgFromClient.readLine();
                    if (message == null) {
                        Log.v("ServerSide", "Null message on Server Side");
                        pauseLock.unlock();
                        continue;
                    }

                    DataOutputStream responseMsg = new DataOutputStream(clientSocket.getOutputStream());

                    if (responseMsg != null) {
                        Log.v("ServerSide", "Response message sent");
                        responseMsg.writeBytes("OK");
                    }

                    Log.v("ServerSide", "Checking Server side 2"+message);


                    ContentValues cv = new ContentValues();
                    cv.put(KEY_FIELD,Integer.toString(messageCount));
                    cv.put(VALUE_FIELD,message);

                    getContentResolver().insert(mUri, cv);
                    publishProgress(message);
                    messageCount++;
                    Log.v("ServerSide", "Checking Server side");
                    clientSocket.close();
                    pauseLock.unlock();
                }
            } catch (Exception e) {
                Log.v("ServerSide", "File write failed");
            }


            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.remote_text_display);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.local_text_display);
            localTextView.append("\n");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            String filename = "GroupMessengerOutput123";
            String string = strReceived + "\n";
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                Log.v("PublishProgress", "File write failed");
            }

            return;
        }



    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                Log.v("ClientTask","Client Task side initiated");
                String[] remotePort = new String[5];
                int count = 5;
                remotePort[0] = REMOTE_PORT0;
                remotePort[1] = REMOTE_PORT1;
                remotePort[2] = REMOTE_PORT2;
                remotePort[3] = REMOTE_PORT3;
                remotePort[4] = REMOTE_PORT4;

                for(int k = 0 ; k < count ; k++)
                {
                    Log.v("ClientTask","Client Task debugging1");
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort[k]));
                    String msgToSend = msgs[0];
                    Log.v("ClientTask","Client Task debugging2");
                /*
                 * TODO: Fill in your client code that sends out a message.
                 */
                    int maxAttempts = 3;
                    int t = 0;
                    DataOutputStream outputMsg = null;
                    while(t<maxAttempts)
                    {
                        Log.v("ClientTask","Client Task debugging3");
                        outputMsg = new DataOutputStream(socket.getOutputStream());

                        if(outputMsg != null)
                        {
                            Log.v("ClientTask","Client Task output msg not null"+msgToSend);
                            outputMsg.writeBytes(msgToSend);
                        }
                        else
                        {
                            Log.v("ClientTask","Client Task output msg null");
                            continue;
                        }

                        boolean sendStatus = true;
                        int time_limit = 4;
                        int i = 0;
                        while(i<time_limit)
                        {
                            Log.v("ClientSide", "Waiting for response message from server");
                            BufferedReader msgFromServer
                                    = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            String responseMsg = msgFromServer.readLine();
                            if (responseMsg.equals("OK"))
                            {
                                sendStatus = true;
                                break;
                            }
                            //Thread.sleep(1000);
                            i+=1;
                        }

                        if(sendStatus == true)
                        {
                            Log.v("ClientSide","Successfully sent to Server");
                            break;
                        }
                        t+=1;
                    }
                    Log.v("ClientSide","Checking client side");

                    Log.v("ClientSide","Checking client side 2");
                    outputMsg.close();
                    socket.close();
                }

            } catch (UnknownHostException e) {
                Log.v("ClientSide", "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.v("ClientSide", "ClientTask socket IOException "+e);
            }


            return null;
        }
    }

}


