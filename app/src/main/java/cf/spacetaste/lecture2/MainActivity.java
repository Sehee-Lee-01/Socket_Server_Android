package cf.spacetaste.lecture2;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.ContentInfo;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import cf.spacetaste.lecture2.R;

public class MainActivity extends AppCompatActivity {
    private ServerThread mServerThread;
    private TextView mTextOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextOutput = (TextView) findViewById(R.id.txtOutput);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public void mOnClick(View v) {
        switch (v.getId()) {
            case R.id.btnStart:
                if (mServerThread == null) {
                    mServerThread = new ServerThread(this, mMainHanddler);
                    mServerThread.start();
                }
                break;
            case R.id.btnQuit:
                finish();
                break;
        }
    }

    private Handler mMainHanddler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    mTextOutput.append((String) msg.obj);
                    break;
            }
        }
    };
}


class ServerThread extends Thread {
    private Context mContext;
    private Handler mMainHandler;

    public ServerThread(Context context, Handler mainHandler) {
        mContext = context;
        mMainHandler = mainHandler;
    }

    @Override
    public void run() {
        ServerSocket servSock = null;
        try {
            servSock = new ServerSocket(9000);
            WifiManager wifiMgr = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
            int serverip = wifiMgr.isWifiEnabled() ? wifiInfo.getIpAddress() : 0x0100007F;
            doPrintln(">> 서버 시작!" + ipv4ToString(serverip) + "/" + servSock.getLocalPort());
            while (true) {
                Socket sock = servSock.accept();
                String ip = sock.getInetAddress().getHostAddress();
                int port = sock.getPort();
                doPrintln(">> 클라이언트 접속: " + ip + "/" + port);
                InputStream in = sock.getInputStream();
                OutputStream out = sock.getOutputStream();
                byte[] buf = new byte[1024];
                while (true) {
                    try {
                        int nbytes = in.read(buf);
                        if (nbytes > 0) {
                            String s = new String(buf, 0, nbytes);
                            doPrintln("[" + ip + "/" + port + "]" + s);
                            out.write(buf, 0, nbytes);
                        } else {
                            doPrintln(">> 클라이언트 종료: " + ip + "/" + port);
                            break;
                        }
                    } catch (IOException e) {
                        doPrintln(">> 클라이언트 종료: " + ip + "/" + port);
                        break;
                    }
                }
                sock.close();
            }
        } catch (IOException e) {
            doPrintln(e.getMessage());
        } finally {
            try {
                if (servSock != null) {
                    servSock.close();
                }
                doPrintln(">> 서버 종료!");
            } catch (IOException e) {
                doPrintln(e.getMessage());
            }
        }
    }

    public void doPrintln(String str) {
        Message msg = Message.obtain();
        msg.what = 1;
        msg.obj = str + "\n";
        mMainHandler.sendMessage(msg);
    }

    private String ipv4ToString(int ip) {
        int a = (ip) & 0xFF, b = (ip >> 8) & 0xFF;
        int c = (ip >> 16) & 0xFF, d = (ip >> 24) & 0xFF;
        return Integer.toString(a) + "." + Integer.toString(b) + "." + Integer.toString(c) + "." + Integer.toString(d);
    }
}