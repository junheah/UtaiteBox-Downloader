package ml.melun.junhea.uboxdownloader;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.MediaController;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class PlayerService extends Service implements MediaPlayer.OnPreparedListener {
    public static final String ACTION_PLAY = "ml.melun.junhea.uboxdownloader.action.PLAY";
    public static final String ACTION_PAUSE = "ml.melun.junhea.uboxdownloader.action.PAUSE";
    public static final String ACTION_STOP = "ml.melun.junhea.uboxdownloader.action.STOP";
    public static final String ACTION_SET = "ml.melun.junhea.uboxdownloader.action.SET";
    public static final String BROADCAST_ACTION = "ml.melun.junhea.uboxdownloader.action.BROADCAST";
    public static final String BROADCAST_TIME = "ml.melun.junhea.uboxdownloader.action.TIME";
    public static final String BROADCAST_STOP = "ml.melun.junhea.uboxdownloader.action.STOP";
    MediaPlayer mediaPlayer=null;
    IBinder mBinder = new LocalBinder();
    Intent intent, timerIntent;
    JSONObject songData;
    WifiManager.WifiLock wifiLock;
    private boolean started = false;
    private Handler handler = new Handler();
    private Runnable runnable;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public PlayerService getServerInstance() {
            return PlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        System.out.println("oncreate!");
        mediaPlayer = new MediaPlayer();
        intent = new Intent();
        timerIntent = new Intent();
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        wifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "uboxdl");
        wifiLock.acquire();
        runnable = new Runnable() {
            @Override
            public void run() {
                int length = mediaPlayer.getDuration();
                int current = mediaPlayer.getCurrentPosition();
                if(current<length) broadcasttime(length, current);
                else stopT();
                if(started) {
                    startT();
                }
            }
        };
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                //if last track :
                System.out.println("complete!");
                stopT();
                // finish current activity
            }
        });
    }

    public void onPrepared(MediaPlayer player) {
        System.out.println("prepared!");
        mediaPlayer.start();
        broadcast();
        stopT();
        startT();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        switch(action) {
            case ACTION_PLAY:
                mediaPlayer.reset();
                try {
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    songData = new JSONObject(intent.getStringExtra("target"));
                    mediaPlayer.setDataSource("http://utaitebox.com/api/play/stream/"+songData.get("key"));
                    mediaPlayer.setOnPreparedListener(this);
                    mediaPlayer.prepareAsync();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mediaPlayer.setOnPreparedListener(this);
                broadcast();
                break;

            case ACTION_PAUSE:
                if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                    startT();
                }else{
                    mediaPlayer.pause();
                    stopT();
                }
                broadcast();
                break;

            case ACTION_STOP:
                //stop player
                stopT();
                break;

            case ACTION_SET:
                int tarT = Integer.parseInt(intent.getStringExtra("time"));
                mediaPlayer.seekTo(tarT);
                break;
        }

        return START_STICKY;
    }


    public void broadcast(){
        intent.setAction(BROADCAST_ACTION);
        intent.putExtra("target", songData.toString());
        intent.putExtra("playing",mediaPlayer.isPlaying());
        sendBroadcast(intent);
    }

    public void broadcasttime(int length, int current){
        timerIntent.setAction(BROADCAST_TIME);
        timerIntent.putExtra("length", length+"");
        timerIntent.putExtra("current", current+"");
        sendBroadcast(timerIntent);
    }

    public void broadcastEnd(){
        intent.setAction(BROADCAST_STOP);
        sendBroadcast(intent);
    }



    public void onDestroy() {
        System.out.println("DESTROYED!");
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.release();
        mediaPlayer=null;
        stopT();
        wifiLock.release();
    }

    public void stopT() {
        started = false;
        handler.removeCallbacks(runnable);
    }

    public void startT() {
        started = true;
        handler.postDelayed(runnable, 1000);
    }
}
