package ml.melun.junhea.uboxdownloader;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.NotificationTarget;

import org.json.JSONArray;
import org.json.JSONObject;

public class PlayerService extends Service implements MediaPlayer.OnPreparedListener {
    public static final String ACTION_PLAY = "ml.melun.junhea.uboxdownloader.action.PLAY";
    public static final String ACTION_PAUSE = "ml.melun.junhea.uboxdownloader.action.PAUSE";
    public static final String ACTION_STOP = "ml.melun.junhea.uboxdownloader.action.STOP";
    public static final String ACTION_PREV = "ml.melun.junhea.uboxdownloader.action.PREV";
    public static final String ACTION_NEXT = "ml.melun.junhea.uboxdownloader.action.NEXT";
    public static final String ACTION_SET = "ml.melun.junhea.uboxdownloader.action.SET";
    public static final String ACTION_PLAYLIST = "ml.melun.junhea.uboxdownloader.action.PLAYLIST";
    public static final String ACTION_GETINFO = "ml.melun.junhea.uboxdownloader.action.GET";
    public static final String BROADCAST_ACTION = "ml.melun.junhea.uboxdownloader.action.BROADCAST";
    public static final String BROADCAST_TIME = "ml.melun.junhea.uboxdownloader.action.TIME";
    public static final String BROADCAST_STOP = "ml.melun.junhea.uboxdownloader.action.STOP";
    MediaPlayer mediaPlayer=null;
    IBinder mBinder = new LocalBinder();
    Intent intent, timerIntent, stopintent;
    JSONObject songData = null;
    JSONObject nullSongData;
    WifiManager.WifiLock wifiLock;
    private boolean started = false;
    private Handler handler = new Handler();
    private Runnable runnable;
    RemoteViews playerView;
    NotificationCompat.Builder notification;
    JSONArray playlist, nullPlayList;
    int position=0;
    Boolean single = true;
    Boolean busy =false;


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
        try{
            songData = new JSONObject()
                    .put("id", 0)
                    .put("name", "")
                    .put("artist", "")
                    .put("thumb", "null")
                    .put("key", "");
            nullSongData=songData;
            playlist = new JSONArray();
            nullPlayList=playlist;
        }catch(Exception e){
            e.printStackTrace();
        }
        mediaPlayer = new MediaPlayer();
        intent = new Intent();
        timerIntent = new Intent();
        stopintent = new Intent();


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
                if(position==playlist.length()-1){
                    System.out.println("complete!");
                    stopT();
                    broadcast(false);
                }else{
                    position++;
                    stopT();
                    play();
                }
                //todo add loop / shuffle mode

                // finish current activity

            }
        });
        //foreground bullshit
        playerView = new RemoteViews(getPackageName(), R.layout.notification_layout);
        //android O bullshit
        showNotification();
    }

    public void onPrepared(MediaPlayer player) {
        System.out.println("prepared!");
        mediaPlayer.start();
        broadcast(false);
        stopT();
        startT();
        updateNotification();
        busy=false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();
        //if(!busy) {
            switch (action) {
                case ACTION_PLAY:
                    try {
                        busy = true;
                        playlist = new JSONArray(intent.getStringExtra("playlist"));
                        position = intent.getIntExtra("position", 0);
                        single = intent.getBooleanExtra("single", false);
                        play();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case ACTION_PAUSE:
                    if (!mediaPlayer.isPlaying()) {
                        mediaPlayer.start();
                        startT();
                    } else {
                        mediaPlayer.pause();
                        stopT();
                    }
                    broadcast(false);
                    updateNotification();
                    break;

                case ACTION_STOP:
                    position=0;
                    songData=nullSongData;
                    playlist=nullPlayList;
                    stopSelf();
                    break;
                case ACTION_NEXT:
                    if (position + 1 < playlist.length()) {
                        position++;
                        play();
                    }else broadcast(false);
                    break;
                case ACTION_PREV:
                    if (mediaPlayer.getCurrentPosition() > 3000) {
                        mediaPlayer.seekTo(0);
                        stopT();
                        startT();
                    } else {
                        if (position > 0) {
                            position--;
                            stopT();
                            play();
                        }else broadcast(false);
                    }

                    break;
                case ACTION_SET:
                    int tarT = Integer.parseInt(intent.getStringExtra("time"));
                    mediaPlayer.seekTo(tarT);
                    broadcast(false);
                    break;

                case ACTION_GETINFO:
                    broadcast(true);
                    break;

                case ACTION_PLAYLIST:
                    if(!single) {
                        try {
                            playlist = new JSONArray(intent.getStringExtra("playlist"));
                            position = intent.getIntExtra("position", 0);
                            System.out.println(position+ "    " + playlist.toString());
                            broadcast(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
        //}
        return START_STICKY;
    }


    public void broadcast(boolean pl){
        intent.setAction(BROADCAST_ACTION);
        if(pl){
            intent.putExtra("playlist", playlist.toString());
        }else{
            intent.putExtra("playlist","");
        }
        intent.putExtra("position", position);
        intent.putExtra("playing",mediaPlayer.isPlaying());
        intent.putExtra("single",single);
        sendBroadcast(intent);
    }

    public void broadcasttime(int length, int current){
        timerIntent.setAction(BROADCAST_TIME);
        timerIntent.putExtra("length", length);
        timerIntent.putExtra("current", current);
        sendBroadcast(timerIntent);
    }

    public void broadcastEnd(){
        stopintent.setAction(BROADCAST_STOP);
        sendBroadcast(stopintent);
    }
    public void play(){
        try {
            stopT();
            mediaPlayer.reset();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            songData = playlist.getJSONObject(position);
            mediaPlayer.setDataSource("http://utaitebox.com/api/play/stream/"
                    + songData.get("key"));
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.prepareAsync();
            //showNotification();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void onDestroy() {
        System.out.println("DESTROYED!");
        stopT();
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        broadcastEnd();
        mediaPlayer.release();
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
    private void showNotification() {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(ACTION_PLAY);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Intent previousIntent = new Intent(this, PlayerService.class);
        previousIntent.setAction(ACTION_PREV);
        PendingIntent ppreviousIntent = PendingIntent.getService(this, 0,
                previousIntent, 0);

        Intent playIntent = new Intent(this, PlayerService.class);
        playIntent.setAction(ACTION_PAUSE);
        PendingIntent pplayIntent = PendingIntent.getService(this, 0,
                playIntent, 0);

        Intent nextIntent = new Intent(this, PlayerService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent pnextIntent = PendingIntent.getService(this, 0,
                nextIntent, 0);

        Intent stopIntent = new Intent(this, PlayerService.class);
        previousIntent.setAction(ACTION_STOP);
        PendingIntent pstopIntent = PendingIntent.getService(this, 0,
                previousIntent, 0);

        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.default_artist);

        //android O
        if (Build.VERSION.SDK_INT >= 26) {
            //NotificationChannel mchannel = new NotificationChannel("UtaiteBox Player","UtaiteBox Player",NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel mchannel = new NotificationChannel("UtaiteBpx Player", "UtaiteBox Player", NotificationManager.IMPORTANCE_DEFAULT);
            mchannel.setDescription("channel description");
            mchannel.enableLights(true);
            mchannel.setLightColor(Color.GREEN);
            mchannel.enableVibration(true);
            mchannel.setVibrationPattern(new long[]{100, 200, 100, 200});
            mchannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        }
        notification = new NotificationCompat.Builder(this, "UtaiteBox Player")
                .setContent(playerView)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setOngoing(true);
        playerView.setOnClickPendingIntent(R.id.notificationPlaybtn, pplayIntent);
        playerView.setOnClickPendingIntent(R.id.notificationNext,pnextIntent);
        playerView.setOnClickPendingIntent(R.id.notificationPrev,ppreviousIntent);
        playerView.setOnClickPendingIntent(R.id.notificationStop,pstopIntent);
        startForeground(123123123, notification.build());

    }

    public void updateNotification(){
        //playerView.
        try {

            playerView.setTextViewText(R.id.notificationSongName, songData.getString("name"));
            playerView.setTextViewText(R.id.notificationArtistName, songData.getString("artist"));
            NotificationTarget notificationCover = new NotificationTarget(
                    this,
                    playerView,
                    R.id.notificationCover,
                    notification.build(),
                    123123123);
            String thumb = songData.getString("thumb");
            if(thumb.matches("null")) playerView.setImageViewResource(R.id.notificationCover,R.drawable.default_cover);
            else{
                Glide.with(this).load("http://utaitebox.com/res/cover/" + thumb).asBitmap().into(notificationCover);
            }
            if (mediaPlayer.isPlaying()) {
                playerView.setImageViewResource(R.id.notificationPlaybtn,android.R.drawable.ic_media_play);
            }else{
                playerView.setImageViewResource(R.id.notificationPlaybtn,android.R.drawable.ic_media_pause);
            }
            showNotification();
        }catch(Exception e){
            //
        }
    }



}
