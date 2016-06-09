package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.Context;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.net.Uri;
import android.content.ContentValues;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {


    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static  String MyPort ="";

    static final int SERVER_PORT = 10000;
    static int SequenceCount=0;
    static int ProcessSeq = 0;
    static int FIFOCounter;
    static PriorityQueue<allMessageInformation> PriorityQ = new PriorityQueue<allMessageInformation>();
    static PriorityQueue<allMessageInformation> AgreedPriority = new PriorityQueue<allMessageInformation>();
    static HashMap changePriority = new HashMap();

    static int count=0;
    static boolean wasRemovedFromPriorityQueue = false;
    static int crashedProcess = 0;

    static allMessageInformation myAllMessage = new allMessageInformation();



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


        // added from PA1
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        MyPort = myPort;

        Log.e(TAG, "port is " + myPort);


        if(myPort.equals(REMOTE_PORT0)){
            FIFOCounter = 0;
        }
        else if(myPort.equals(REMOTE_PORT1)){
            FIFOCounter = 1;
        }
        else if(myPort.equals(REMOTE_PORT2)){
            FIFOCounter = 2;
        }
        else if(myPort.equals(REMOTE_PORT3)){
            FIFOCounter = 3;
        }
        else if(myPort.equals(REMOTE_PORT4)){
            FIFOCounter = 4;
        }


        //code Snipets from PA1
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT ,20 );
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }


        final EditText editText = (EditText) findViewById(R.id.editText1);

        Log.e(TAG, "click hoga abhi");

        final Button button = (Button) findViewById(R.id.button4);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Log.e(TAG, "send toh click hua ");

                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + msg); // This is one way to display a string.
                TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                remoteTextView.append("\n");

                // increment sequence number on message send

                //ProcessSeq++;
                // keep the mesure of allmessage data
                allMessageInformation alldata = new allMessageInformation();
                alldata.Message = msg;
                alldata.FIFOArray[FIFOCounter] = alldata.FIFOArray[FIFOCounter] +1;
                alldata.fromProcess = FIFOCounter;
                alldata.initialSeq = ProcessSeq;
                alldata.canBeDelivered = false;
                alldata.port = myPort;

                //PriorityQ.add(alldata);

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, alldata);
            }
        });
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Log.e(TAG, "call me atleast: ");

            while (true) {
                try {
                    Socket server = serverSocket.accept();
                    allMessageInformation temp = null;


                    Log.e(TAG, "Socket address" + server.getLocalPort());

                    //ObjectInputStream input = new ObjectInputStream(server.getInputStream());
                    String stringToSend = "";
                    Log.e(TAG, "just Before ");

                    //Read initial data
                    ObjectInputStream ois = new ObjectInputStream(server.getInputStream());
                    Object o = ois.readObject();

                    // Extract Information from the received message and send back proposed priority
                    if(o instanceof allMessageInformation) {
                        allMessageInformation received = (allMessageInformation) o;
                        temp = received;
                        stringToSend = received.Message;

                        if (!received.proposed) {

                            Log.e(TAG, "I received" + received.port + "  " + MyPort);

                            //if (!received.port.equals(MyPort)) {
                            ProcessSeq++;
                            PriorityQ.add(received);
                            crashedProcess = received.fromProcess;
                            received.proposed = true;
                            received.proposedSeq = ProcessSeq;
                            received.priorityNum = received.proposedSeq + (received.fromProcess * 0.1);
                            Log.e(TAG, "Priority Num " + received.priorityNum);


                            ObjectOutputStream sendData = new ObjectOutputStream(server.getOutputStream());
                            sendData.writeObject(received);
                          }
                      }


                        //Read initial data
                        ObjectInputStream read = new ObjectInputStream(server.getInputStream());
                        Object ready = read.readObject();

                        if(ready instanceof allMessageInformation) {
                            allMessageInformation received = (allMessageInformation) ready;


                            // Read the final agreed priority.
                            if (received.agreed) {
                                Iterator<allMessageInformation> iter = PriorityQ.iterator();
                                allMessageInformation current;
                                while (iter.hasNext()) {
                                    current = iter.next();
                                    if (current.fromProcess == received.fromProcess && current.initialSeq == received.initialSeq) {
                                        // do something with curret
                                        PriorityQ.remove(current);
                                        received.canBeDelivered = true;
                                        PriorityQ.add(received);
                                        count++;
                                        Log.e(TAG, "No of messages" + count);
                                        //current.priorityNum = received.priorityNum;
                                        //current.canBeDelivered = true;
                                    }
                                    Log.e(TAG, "Message " + received.Message + " agreed" + received.priorityNum);
                                }
                            }
                        }




                    //*************************** deliver the message ******************************/
                   if(!PriorityQ.isEmpty() && PriorityQ.peek().canBeDelivered) {
//                           count++;
//                           Log.e(TAG,"No of messages" + count);
                       publishProgress(PriorityQ.remove().Message);
                   }

                    server.close();
                    Log.e(TAG, "publish information: " );


                } catch (UnknownHostException e) {
                    Log.e(TAG, "ServerTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ServerTask socket IOException" + e.toString() + serverSocket.getInetAddress());
                    if (!wasRemovedFromPriorityQueue) {
                                removeCrashedPriority(crashedProcess);
                    }
                }
                    catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }



        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");
            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append("\n");
            Log.e(TAG, "text received" + strReceived);

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            ContentValues values = new ContentValues();
            values.put("key"  , Integer.toString(SequenceCount));
            values.put("value" , strReceived);
            SequenceCount++;


            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
            uriBuilder.scheme("content");

            Uri uri = uriBuilder.build();

            try {
                getContentResolver().insert(uri, values);


            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }

            return;
        }

    }





    //taken from PA1 and modified for 5 AVDS
    private class ClientTask extends AsyncTask<allMessageInformation, Void, Void> {

        protected Void doInBackground(allMessageInformation... msgs) {
                ArrayList<String> remotePortList = new ArrayList<String>();
                remotePortList.add(REMOTE_PORT0);
                remotePortList.add(REMOTE_PORT1);
                remotePortList.add(REMOTE_PORT2);
                remotePortList.add(REMOTE_PORT3);
                remotePortList.add(REMOTE_PORT4);

                allMessageInformation received = null;
                ArrayList<Double> allreceivedPriority = new ArrayList<Double>();

                 ArrayList<Socket> socketsList = new ArrayList<Socket>();

                for (String remote : remotePortList ) {

                    try {

                        //sending data
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remote));

                        //set timeout for the socket.
                        // socket.setSoTimeout(2000);

                        //save sockets in arraylist so that can be used later to send messages
                        socketsList.add(socket);
                        ObjectOutputStream sendData = new ObjectOutputStream(socket.getOutputStream());
                        allMessageInformation check = (allMessageInformation) msgs[0];
                        sendData.writeObject(msgs[0]);


                        //receiving data
                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                        Object o = ois.readObject();


                        // Extract Information from the received message
                        if (o instanceof allMessageInformation) {
                            received = (allMessageInformation) o;
                            allreceivedPriority.add(received.priorityNum);
                        }


                        } catch (UnknownHostException e) {
                            Log.e(TAG, "ClientTask UnknownHostException");
                        } catch (IOException e) {
                            Log.e(TAG, "ClientTask socket IOException" + e.toString() +"--"+remote);
//                            if (!wasRemovedFromPriorityQueue) {
//                                removeCrashedPriority(remote);
//                            }
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }

                    //socket.close();
                }


                // Sort the arraylist get the max priority num for agreed seq
                Collections.sort(allreceivedPriority);
                received.agreedSeq = allreceivedPriority.get(allreceivedPriority.size() - 1);
                received.agreed = true;

                Log.e(TAG, "highest priority num " + received.agreedSeq + " " + received.fromProcess + "message was " +received.Message );


                // Now send for agreed sequence.
                for (Socket socket  : socketsList ) {
                    try {
                     //sending data
                     Log.e(TAG, "sending from socket okay");
                      //socket.setSoTimeout(2000);

                    //sending data
                    ObjectOutputStream sendData = new ObjectOutputStream(socket.getOutputStream());
                    sendData.writeObject(received);
                    socket.close();


                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException");
                    } catch (IOException e) {
                        Log.e(TAG, "ClientTask socket IOException" + e.toString() + socket.getRemoteSocketAddress());
                    }
                }

            return null;
        }
    }



    public void removeCrashedPriority(String remotePort){
        wasRemovedFromPriorityQueue = true;
        Log.e(TAG, "entered removeCrashedPriority");

        Integer process = 0;

        if(remotePort.equals(REMOTE_PORT0)){
            process = 0;
        }
        else if(remotePort.equals(REMOTE_PORT1)){
            process = 1;
        }
        else if(remotePort.equals(REMOTE_PORT2)){
            process = 2;
        }
        else if(remotePort.equals(REMOTE_PORT3)){
            process = 3;
        }
        else if(remotePort.equals(REMOTE_PORT4)){
            process = 4;
        }



        Iterator<allMessageInformation> iter = PriorityQ.iterator();
        allMessageInformation current;
        while (iter.hasNext()) {
            current = iter.next();
            if (current.fromProcess == process) {
                // do something with curret
                Log.e(TAG, "removed element");

                PriorityQ.remove(current);
            }
        }
    }


    public void removeCrashedPriority(int process){
        wasRemovedFromPriorityQueue = true;
        Log.e(TAG, "entered removeCrashedPriority");

        Iterator<allMessageInformation> iter = PriorityQ.iterator();
        allMessageInformation current;
        while (iter.hasNext()) {
            current = iter.next();
            if (current.fromProcess == process) {
                // do something with curret
                Log.e(TAG, "removed element");

                PriorityQ.remove(current);
            }
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    // this class will contain all information that we need to maintain FIFO and

    public static class allMessageInformation implements Serializable, Comparable<Object> {
        int[] FIFOArray = {0,0,0,0,0};
        String Message;
        int fromProcess;
        int initialSeq;
        int proposedSeq;
        Double agreedSeq;
        boolean proposed;
        boolean agreed;

        double priorityNum;
        boolean canBeDelivered;
        String port;


        public allMessageInformation(){
            this.proposed = false;
            this.canBeDelivered = false;
        }


        public int compareTo(Object check)
        {
            allMessageInformation temp = (allMessageInformation)check;
            if(this.priorityNum < temp.priorityNum)
                return -1;
            else if(this.priorityNum == temp.priorityNum)
                return 0;
            else
                return 1;
        }
    }



    class SortQueueViaPriority implements Comparator<allMessageInformation> {
        @Override
        public int compare(allMessageInformation f1, allMessageInformation f2) {
            return Double.compare(f2.priorityNum, f1.priorityNum);
        }
    }


}
