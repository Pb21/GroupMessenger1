package edu.buffalo.cse.cse486586.groupmessenger1;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String[] PORTS={"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;

    private static final int TEST_CNT = 50;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

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

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e("Error", "Can't create a ServerSocket");
            return;
        }

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final TextView editText = (TextView) findViewById(R.id.editText1);
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
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

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            int k=0;
            try {
                while(true) {
                    Socket sSocket = serverSocket.accept();
                    BufferedReader incoming = new BufferedReader(
                            new InputStreamReader(sSocket.getInputStream()));

                    String userInput = incoming.readLine();
                    publishProgress(userInput);
                    saveMessages(k++,userInput);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }



            protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
                String strReceived = strings[0].trim();
                Log.d("Inside ProgressUpdate","strReceived"+strReceived);
                TextView textView1 = (TextView) findViewById(R.id.textView1);
                textView1.append(strReceived + "\t\n");
                return;
            }
        }
        /***
         * ClientTask is an AsyncTask that should send a string over the network.
         * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
         * an enter key press event.
         *
         * @author stevko
         *
         */
        private class ClientTask extends AsyncTask<String, Void, Void> {
            @Override
            protected Void doInBackground(String... msgs) {
                try {
                    String msgToSend = msgs[0];
                    for (int i=0;i<5;i++){
                        Socket[] socket=new Socket[5];
                        socket[i] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(PORTS[i]));

                        PrintWriter out=new PrintWriter(socket[i].getOutputStream(), true);
                        out.print(msgToSend);
                        out.flush();
                        out.close();

                        socket[i].close();
                    }

                } catch (UnknownHostException e) {
                    Log.e("Error", "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e("Error", "ClientTask socket IOException"+e.getMessage());
                }

                return null;
            }
        }



    /**
     * buildUri() demonstrates how to build a URI for a ContentProvider.
     *
     * @param scheme
     * @param authority
     * @return the URI
     */
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }


    private boolean saveMessages(int k,String values) {
        final Uri mUri= buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");
        final ContentResolver smContentResolver =getContentResolver();
        ContentValues cv = new ContentValues();
        cv = new ContentValues();
        cv.put(KEY_FIELD, Integer.toString(k));
        cv.put(VALUE_FIELD, values);

        try {
            smContentResolver.insert(mUri, cv);

        } catch (Exception e) {
            Log.e("Error", e.toString());
            return false;
        }
        return true;
    }
  }

