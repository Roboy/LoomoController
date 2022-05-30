package roboy.loomo.loomosocketcontroller;


import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.io.Charsets;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.locomotion.sbv.Base;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

enum ServerMessage {
    CONNECTED("Connected"),
    GO("Go"),
    TURN("Turn"),
    MESSAGE_RECEIVED("MessageReceived");

    private final String value;
    ServerMessage( String value){
        this.value = value;
    }
    String getValue(){
        return this.value;
    }
}
public class MainActivity extends AppCompatActivity {
    public int SERVER_PORT = 5050;
    public String SERVER_IP = "172.20.10.2";
    private ClientThread clientThread;
    private Thread thread;
    private LinearLayout msgList;
    private Handler handler;
    private int clientTextColor;
    private EditText edMessage;
    private Base loomoBase;
    private boolean base_ready = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Client");
        clientTextColor = ContextCompat.getColor(this, R.color.green);
        handler = new Handler();
        msgList = findViewById(R.id.msgList);
        edMessage = findViewById(R.id.edMessage);
        loomoBase = Base.getInstance();
        loomoBase.bindService(getApplicationContext(), new ServiceBinder.BindStateListener() {
            @Override
            public void onBind() {
                base_ready = true;
                showMessage("Base is ready", clientTextColor, true);
            }

            @Override
            public void onUnbind(String reason) {
                base_ready = false;
            }
        });
    }

    public TextView textView(String message, int color, Boolean value) {
        if (null == message || message.trim().isEmpty()) {
            message = "<Empty Message>";
        }
        TextView tv = new TextView(this);
        tv.setTextColor(color);
        tv.setText(message + " [" + getTime() + "]");
        tv.setTextSize(20);
        tv.setPadding(0, 5, 0, 0);
        tv.setLayoutParams(new LinearLayout.LayoutParams
                (LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 0));
        if (value) {
            tv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        }
        return tv;
    }

    public void showMessage(final String message, final int color, final Boolean value) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                msgList.addView(textView(message, color, value));
            }
        });
        Log.i("ServerMessage", message);
    }

    public void onClick(View view) {

        if (view.getId() == R.id.connect_server) {
            msgList.removeAllViews();
            clientThread = new ClientThread();
            thread = new Thread(clientThread);
            thread.start();
            return;
        }

        if (view.getId() == R.id.send_data) {
            String clientMessage = edMessage.getText().toString().trim();
            showMessage(clientMessage, Color.BLUE, false);
            if (null != clientThread) {
                if (clientMessage.length() > 0){
                    clientThread.sendMessage(clientMessage);
                }
                edMessage.setText("");
            }
        }
    }

    /* clientThread class defined to run the client connection to the socket network using the server ip and port
     * and send message */
    class ClientThread implements Runnable {

        private Socket socket;
        private BufferedReader input;

        @Override
        public void run() {

            try {

                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                showMessage("Connecting to Server...", clientTextColor, true);

                socket = new Socket(serverAddr, SERVER_PORT);

                PrintWriter out;
                if (socket.isBound()){
                    out = new PrintWriter(new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream())),
                            true);
                    out.println(ServerMessage.CONNECTED.getValue());
                    showMessage("Connected to Server...", clientTextColor, true);
                }

                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8));
                char[] buffer = new char[24];
                while (true) {
                    if (reader.ready()) {
                        int charsRead = reader.read(buffer);
                        if (charsRead == -1) {
                            break;
                        }
                        String message_read = new String(buffer);
                        process_message(message_read);
                        //showMessage("Server: " + message_read, clientTextColor, true);
                    }
                }


            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                showMessage("Problem Connecting to server... Check your server IP and Port and try again", Color.RED, false);
                Thread.interrupted();
                e1.printStackTrace();
            } catch (NullPointerException e3) {
                showMessage(e3.getMessage(), Color.RED,true);
            }

        }

        void sendMessage(final String message) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (null != socket) {
                            PrintWriter out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(socket.getOutputStream())),
                                    true);
                            out.println(message);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    }

    private void process_message(String message_read) {
        String[] devided_string = message_read.split(",");
        switch (devided_string[0]){
            case "GO":
                if(base_ready){
                    loomoBase.setControlMode(Base.CONTROL_MODE_RAW);
                    float linear_velocity = Float.parseFloat(devided_string[1]);
                    float angular_velocity = Float.parseFloat(devided_string[2]);
                    showMessage("Loomo Go: " + linear_velocity + "," + angular_velocity, clientTextColor, true);
                    loomoBase.setLinearVelocity(linear_velocity);
                    loomoBase.setAngularVelocity(angular_velocity);
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + devided_string[0]);
        }
    }

    String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != clientThread) {
            clientThread.sendMessage("Disconnect");
            clientThread = null;
        }
    }
}