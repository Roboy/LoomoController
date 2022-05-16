package roboy.loomo.loomocontroller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity {
    private SeekBar mBrightnessBar;
    private TextView mDisplayText;
    private TextView mRefresh;
    private AccessoryEngine mEngine = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mDisplayText = (TextView) findViewById(R.id.displayedText);
        mRefresh = (TextView) findViewById(R.id.refreshButton);
        mBrightnessBar = (SeekBar) findViewById(R.id.sbBrightness);
        mBrightnessBar.setOnSeekBarChangeListener(mSeekBarListener);
        mRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshIntent(getIntent());
            }
        });
        onNewIntent(getIntent());

    }

    @Override
    protected void onNewIntent(Intent intent) {
        refreshIntent(intent);
        super.onNewIntent(intent);
    }

    private void refreshIntent(Intent intent) {
        displayText("handling intent action: " + intent.getAction());
        if (mEngine == null) {
            mEngine = new AccessoryEngine(getApplicationContext(), mCallback);
        }
        mEngine.onNewIntent(intent);
    }

    @Override
    protected void onDestroy() {
        mEngine.onDestroy();
        mEngine = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private final OnSeekBarChangeListener mSeekBarListener = new OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            displayText("value is %d");
            if (fromUser && mEngine != null) {
                mEngine.write(new byte[] { (byte) progress });
            }
        }
    };

    private final AccessoryEngine.IEngineCallback mCallback = new AccessoryEngine.IEngineCallback() {
        @Override
        public void onDeviceDisconnected() {
            displayText("device physically disconnected");
        }

        @Override
        public void onConnectionEstablished() {
            displayText("device connected! ready to go!");
        }

        @Override
        public void onConnectionClosed() {
            displayText("connection closed");
        }

        @Override
        public void onDataRecieved(byte[] data, int num) {
            displayText("received bytes " + data.toString() + " " + num);
        }
    };
    
    private void displayText(String text){
        mDisplayText.setText(mDisplayText.getText() + "\n" + text);
    }

}