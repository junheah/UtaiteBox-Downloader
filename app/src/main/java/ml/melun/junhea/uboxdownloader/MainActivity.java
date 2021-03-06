package ml.melun.junhea.uboxdownloader;

//todo 서비스-액티비티 간 통신 딜레이 줄이기
//todo 리스트뷰 없애고 리사이클러로 변경
//todo UI 개선
//todo 오프라인 모드 개선 (라이브러리 기능 추가, 오프라인 플레이리스트)
//todo playlist 탭 들어갔다 likes 탭 들어가면 onclicklistener가 적용이 안됨

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.bumptech.glide.Glide;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import ml.melun.junhea.uboxdownloader.Adapter.CustomAdapter;
import ml.melun.junhea.uboxdownloader.Adapter.playlistAdapter;
import ml.melun.junhea.uboxdownloader.ItemTouchHelper.ItemTouchHelperCallback;

import static ml.melun.junhea.uboxdownloader.PlayerService.ACTION_NEXT;
import static ml.melun.junhea.uboxdownloader.PlayerService.ACTION_PREV;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private SlidingUpPanelLayout panel;
    private static int version;
    static String ACTION_PLAY = "ml.melun.junhea.uboxdownloader.action.PLAY";
    static String ACTION_PAUSE = "ml.melun.junhea.uboxdownloader.action.PAUSE";
    static String ACTION_STOP = "ml.melun.junhea.uboxdownloader.action.STOP";

    int panelOriginalHeight;
    Menu menuNav;
    DownloadManager dlManager;
    String sessionString;
    JSONObject sessionData;
    //String sessionToken,sessionUsername, sessionUserAvatar, sessionUserCover;
    //int sessionUserId;
    //int searchMode = 0;
    static int loginTaskRequestId = 1;
    SharedPreferences sharedPref;
    Button searchButton;
    ListView resultList;
    EditText searchBox;
    ProgressDialog pd;
    JSONObject data;
    NavigationView navigationView;
    CustomAdapter searchAdapter;
    CustomAdapter resultAdapter;
    int searchStage=0;
    //searched nothing = 0 , searched all = 1 , got to result page = 2
    CustomAdapter likesAdapter;
    ArrayList<Long> dllist= new ArrayList<>();
    NotificationCompat.Builder stat;
    BroadcastReceiver onComplete;
    ViewFlipper contentHolder;
    Toolbar toolbar;
    Boolean panelVisible = true;
    LinearLayout miniPlayer, miniPlayerInfoContainer;
    int mode =0;
    int playerOriginalHeight;
    ImageView miniPlayerCover;
    ImageButton miniPlayerPlaybtn, playerPlaybtn, playerNextbtn, playerPrevbtn;
    SeekBar playerSeekBar;
    ConstraintLayout playerControl;
    TextView playerSongName, playerArtistName, miniPlayerSongName, miniPlayerArtistName, playerTime;
    Intent player;
    BroadcastReceiver playBackReceiver;
    JSONObject playerCurrentSong = null;
    JSONObject nullSongData;
    Boolean seekbarPressed = false;
    ProgressBar miniPlayerProgress;
    RecyclerView playListView;
    ArrayList<Item> playlistItems;
    int songLength = 0;
    playlistAdapter listAdapter;
    int playlistPosition=0;
    JSONArray playlist;
    String settingsString;
    JSONObject settings;
    NotificationManagerCompat notificationManagerc;
    NotificationManager notificationManager = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //get version code
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        dlManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        setContentView(R.layout.activity_main);
        searchBox = findViewById(R.id.searchBox);
        resultList = findViewById(R.id.resultList);
        searchButton = findViewById(R.id.searchButton);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        contentHolder = findViewById(R.id.contentHolder);
        setSupportActionBar(toolbar);
        miniPlayer = findViewById(R.id.tinyPlayer);
        miniPlayerInfoContainer = findViewById(R.id.infoContainer);
        miniPlayerCover = findViewById(R.id.miniPlayerCover);
        miniPlayerPlaybtn = findViewById(R.id.miniPlayerPlaybtn);
        miniPlayerSongName = findViewById(R.id.miniPlayerSongName);
        miniPlayerArtistName = findViewById(R.id.miniPlayerArtistName);

        playerControl = findViewById(R.id.playerControl);

        playerPlaybtn = findViewById(R.id.PlayerPlaybtn);
        playerSeekBar = findViewById(R.id.playerSeekBar);
        playerArtistName = findViewById(R.id.playerArtistName);
        playerSongName = findViewById(R.id.playerSongName);
        playerTime = findViewById(R.id.playerTime);
        miniPlayerProgress = findViewById(R.id.miniPlayerProgress);
        playerPrevbtn = findViewById(R.id.playerPrevbtn);
        playerNextbtn = findViewById(R.id.playerNextbtn);


        playListView = findViewById(R.id.playList);
        playListView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        playListView.setHasFixedSize(true);
        playlistItems = new ArrayList<>();
        listAdapter = new playlistAdapter(playlistItems,MainActivity.this);
        new ItemTouchHelper(new ItemTouchHelperCallback(listAdapter)).attachToRecyclerView(playListView);
        playListView.setAdapter(listAdapter);
        playListView.addOnItemTouchListener(
                new RecyclerItemClickListener(this, playListView ,new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) {
                        // add playlist to service
                        //System.out.println(listAdapter.getPlayList());
                        try {
                            playlist = new JSONArray(listAdapter.getPlayList());
                            player.putExtra("playlist", playlist.toString());
                            player.putExtra("position", position);
                            player.putExtra("single", false);
                            playerDeinit();
                            player.setAction(PlayerService.ACTION_PLAY);
                            startplayer(player);

                        }catch(Exception e){
                            //
                        }
                    }
                })
        );


        try {
            nullSongData = new JSONObject()
                    .put("id", 0)
                    .put("name", "")
                    .put("artist", "")
                    .put("thumb", "null")
                    .put("key", "");
        }catch(Exception e){
            //
        }

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */
        panel = findViewById(R.id.sliding_layout);
        panelOriginalHeight = panel.getPanelHeight();
        panel.setParallaxOffset(500);
        panel.setDragView(miniPlayer);

        System.out.println(playerOriginalHeight);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);


        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
            //add drawer listeners here

        };

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //get menuview
        menuNav = navigationView.getMenu();


        //notification channel (for oreo)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = new NotificationChannel("UtaiteBox Player", "UtaiteBox Player", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("channel description"); notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.GREEN); notificationChannel.enableVibration(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(notificationChannel);
        }else{
            notificationManagerc = NotificationManagerCompat.from(MainActivity.this);
        }

        ///////CODE STARTS HERE
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        13132);
        }

        // session
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        sessionString = sharedPref.getString(getString(R.string.session_file),"");
        refreshSettings();
        if(sessionString.matches("")) {
            //data is empty
            menuNav.getItem(1).setEnabled(false);
            menuNav.getItem(2).setEnabled(false);
        }else{
            try{
                sessionData = new JSONObject(sessionString);
                refreshUserData();
                MenuItem loginbtn = menuNav.findItem(R.id.logIn);
                loginbtn.setTitle("로그아웃");
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        onComplete = new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {
                long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                dllist.remove(referenceId);
                if (dllist.isEmpty())
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Notification.Builder builder = new Notification.Builder(MainActivity.this, "UtaiteBox Player")
                                .setContentTitle("우타이테 박스 다운로더")
                                .setContentText("모든 다운로드가 완료되었습니다")
                                .setSmallIcon(R.drawable.ic_notification_icon)
                                .setAutoCancel(false);
                        notificationManager.notify(13155431, builder.build());
                    }else {

                        stat = new NotificationCompat.Builder(MainActivity.this, "UtaiteBox Downloader");
                        stat.setContentTitle("우타이테 박스 다운로더")
                                .setContentText("모든 다운로드가 완료되었습니다")
                                .setPriority(NotificationCompat.PRIORITY_LOW)
                                .setOngoing(false)
                                .setSmallIcon(R.drawable.ic_notification_icon);
                        notificationManagerc.notify(13155431, stat.build());
                    }
                    Toast.makeText(getApplicationContext(), "다운로드 완료", Toast.LENGTH_SHORT).show();
                }

            }
        };
        registerReceiver(onComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));


        reloadViews(0);

        addpanelListener();

        //player service intent
        player = new Intent(this,PlayerService.class);

        miniPlayerPlaybtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                player.setAction(ACTION_PAUSE);
                startplayer(player);
            }
        });
        playerPlaybtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                player.setAction(ACTION_PAUSE);
                startplayer(player);
            }
        });
        playerPrevbtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                player.setAction(ACTION_PREV);
                startplayer(player);
            }
        });
        playerNextbtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                player.setAction(ACTION_NEXT);
                startplayer(player);
            }
        });
        miniPlayerPlaybtn.setEnabled(false);
        playerPlaybtn.setEnabled(false);
        //broadcast reciever
        playBackReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action.matches(PlayerService.BROADCAST_ACTION)) {
                    playlistPosition = intent.getIntExtra("position",0);
                    String playliststr = intent.getStringExtra("playlist");
                    if(playliststr.matches("")){
                        //no playlist sync needed
                    }else{
                        try{
                            playlist = new JSONArray(playliststr);
                        }catch(Exception e){
                            //
                        }
                    }
                    boolean playing = intent.getBooleanExtra("playing", false);
                    if (playing) {
                        miniPlayerPlaybtn.setImageResource(android.R.drawable.ic_media_play);
                        playerPlaybtn.setImageResource(android.R.drawable.ic_media_play);
                    } else {
                        miniPlayerPlaybtn.setImageResource(android.R.drawable.ic_media_pause);
                        playerPlaybtn.setImageResource(android.R.drawable.ic_media_pause);
                    }
                    if(intent.getBooleanExtra("single",false)){
                        listAdapter.setPosition(-1);
                    }
                    else{
                        listAdapter.setPosition(playlistPosition);
                    }

                    try {
                        JSONObject nowPlaying = playlist.getJSONObject(playlistPosition);
                        playerCurrentSong = nowPlaying;
                        updatePlayer(playerCurrentSong);
                    } catch (Exception e) {
                        //
                    }
                    try {
                        if (playerCurrentSong.getString("name").matches("")) playerDeinit();
                        else playerInit();
                    }catch(Exception e){

                    }

                }else if(action.matches(PlayerService.BROADCAST_TIME)){
                    if(!seekbarPressed) {
                        songLength = intent.getIntExtra("length",0);
                        int current = intent.getIntExtra("current",0);
                        playerSeekBar.setMax(songLength);
                        playerSeekBar.setProgress(current);
                        miniPlayerProgress.setMax(songLength);
                        miniPlayerProgress.setProgress(current);
                        playerTime.setText(getTimeStamp(current) + " | " + getTimeStamp(songLength));
                    }
                    playerInit();
                }else if(action.matches(PlayerService.BROADCAST_STOP)){
                    playerCurrentSong=nullSongData;
                    playlistPosition=-1;
                    playlist=new JSONArray();
                    updatePlayer(playerCurrentSong);
                    playerDeinit();
                    listAdapter.setPosition(-1);
                }
            }
        };
        IntentFilter infil = new IntentFilter();
        infil.addAction(PlayerService.BROADCAST_ACTION);
        infil.addAction(PlayerService.BROADCAST_TIME);
        infil.addAction(PlayerService.BROADCAST_STOP);
        registerReceiver(playBackReceiver,infil);

        //start player intent to see if service is already running
        player.setAction(PlayerService.ACTION_GETINFO);
        startplayer(player);


        //seekbar listener
        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(b){
                    playerTime.setText(getTimeStamp(i)+" | " + getTimeStamp(songLength));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekbarPressed=true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekbarPressed=false;
                int tarT = seekBar.getProgress();
                player.setAction(PlayerService.ACTION_SET);
                player.putExtra("time",tarT+"");
                startplayer(player);
            }
        });

        playerSeekBar.setEnabled(false);

        //playerSeekBar.setOnSeekBarChangeListener();


    }

    public String getTimeStamp(int m){
        long second = (m / 1000) % 60;
        long minute = (m / (1000 * 60)) % 60;
        long hour = (m / (1000 * 60 * 60)) % 24;
        return(String.format("%02d:%02d:%02d", hour, minute, second));
    }

    public void updatePlayer(JSONObject song){
        try {
            System.out.println(song.toString());
            String thumb = song.getString("thumb");
            String name = song.getString("name");
            String artist = song.getString("artist");
            playerSongName.setText(name);
            miniPlayerSongName.setText(name);
            playerArtistName.setText(artist);
            miniPlayerArtistName.setText(artist);

            if (thumb.matches("null")) miniPlayerCover.setImageResource(R.drawable.default_cover);
            else {
                thumb = "http://utaitebox.com/res/cover/" + thumb;
                Glide.with(this)
                        .load(thumb)
                        .dontAnimate()
                        .into(miniPlayerCover);
            }
            if(name.length()>0){
                playerInit();
            }else{
                playerDeinit();
            }

        }catch(Exception e){
            //
        }
    }
    public void playerInit(){
        miniPlayerPlaybtn.setEnabled(true);
        playerPlaybtn.setEnabled(true);
        playerSeekBar.setEnabled(true);
        playerNextbtn.setEnabled(true);
        playerPrevbtn.setEnabled(true);
    }
    public void playerDeinit(){
        miniPlayerPlaybtn.setEnabled(false);
        playerPlaybtn.setEnabled(false);
        playerSeekBar.setEnabled(false);
        playerNextbtn.setEnabled(false);
        playerPrevbtn.setEnabled(false);
        playerSeekBar.setProgress(0);
        miniPlayerProgress.setProgress(0);
        playerTime.setText("");
        playerPlaybtn.setImageResource(android.R.drawable.ic_media_pause);
        miniPlayerPlaybtn.setImageResource(android.R.drawable.ic_media_pause);
    }
    public void playerBtnDeinit(){
        miniPlayerPlaybtn.setEnabled(false);
        playerPlaybtn.setEnabled(false);
        playerNextbtn.setEnabled(false);
        playerPrevbtn.setEnabled(false);
    }


    public void setListOnClick(){
       if(mode==2){
           //playlist onitemtouchhelper

       }else {
           resultList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

               public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                   CustomAdapter tAdapter = null;
                   switch (mode) {
                       case 0:
                           switch(searchStage) {
                               case 1:
                                   tAdapter = searchAdapter;
                                   break;
                               case 2:
                                   tAdapter = resultAdapter;
                                   break;
                           }
                           break;
                       case 1:
                           tAdapter = likesAdapter;
                           break;
                       case 2:
                           break;
                   }
                   Item item = tAdapter.getItem(position);
                   System.out.println(item.getName() + "," + item.getType());

                   switch (item.getType()) {
                       case 0:
                           int sid = item.getId();
                           new selectSong().execute(sid);
                           break;
                       case 1:
                           int aid = item.getId();
                           new selectArtist().execute(aid);
                           break;
                       case 2:
                           Item tarItem = tAdapter.getItem(position);
                           try{
                               playlist= new JSONArray("["+tarItem.getJSON()+"]");
                               playlistPosition=0;
                               player.putExtra("playlist", playlist.toString());
                               player.putExtra("position",playlistPosition);
                               player.putExtra("single",true);
                               playerDeinit();
                               player.setAction(ACTION_PLAY);
                               startplayer(player);
                           }catch(Exception e){
                               //
                           }

                           break;
                   }
               }
           });

           //longclick
           resultList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
               @Override
               public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                              int position, long id) {
                   // TODO Auto-generated method stub
                   CustomAdapter tAdapter = null;
                   switch (mode) {
                       case 0:
                           tAdapter = searchAdapter;
                           break;
                       case 1:
                           tAdapter = likesAdapter;
                           break;
                       case 2:
                           break;
                   }
                   Item item = tAdapter.getItem(position);
                   System.out.println(item.getName() + "," + item.getType());

                   switch (item.getType()) {
                       case 0:
                           break;
                       case 1:
                           break;
                       case 2:
                           //todo longclick popup menu
                           Toast.makeText(getApplicationContext(), "다운로드를 시작합니다.", Toast.LENGTH_SHORT).show();
                           try {
                               Item dlitem = tAdapter.getItem(position);
                               String dlkey = dlitem.getKey();
                               String dltitle = dlitem.getArtist() + " - " + dlitem.getName();
                               Uri dlurl = Uri.parse("http://utaitebox.com/api/play/stream/" + dlkey);
                               DownloadManager.Request request = new DownloadManager.Request(dlurl);
                               request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                               request.setAllowedOverRoaming(false);
                               request.setTitle(dltitle);
                               request.setDescription("우타이테 박스 다운로더");
                               request.setVisibleInDownloadsUi(true);
                               request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "/UtaiteBoxDownloads/" + dltitle + ".mp3");
                               dllist.add(dlManager.enqueue(request));
                               break;
                           } catch (Exception e) {
                               e.printStackTrace();
                           }

                   }
                   return true;
               }
           });
       }
    }

    public void startplayer(Intent service){
        playerBtnDeinit();
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(service);
        }else{
            startService(service);
        }
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onComplete);
        unregisterReceiver(playBackReceiver);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if(panel.getPanelState()== SlidingUpPanelLayout.PanelState.EXPANDED) {
                panel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }else{
                if(searchStage>0&&mode==0){
                    searchStage--;
                    reloadViews(mode);
                }else super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // actionbar clicks
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.check_update) {
            new updateCheck().execute();
            return true;
        }else if(id == R.id.settings) {
            Intent settingsTask = new Intent(this, SettingsActivity.class);
            startActivity(settingsTask);
        }

        return super.onOptionsItemSelected(item);
    }
    //additional variables for panel
    int miniPlayerCoverOriginalWidth;
    int miniPlayerCoverMaxWidth;
    int screenWidth;
    Boolean listnerFirst=true;
    //
    public void addpanelListener(){
        panel.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                if(listnerFirst){
                    //declare original sizes
                    //todo: make this not "hard-coded"
                    playerOriginalHeight = Math.round(68 * getResources().getDisplayMetrics().density);
                    miniPlayerCoverOriginalWidth = Math.round(50 * getResources().getDisplayMetrics().density);
                    miniPlayerCoverMaxWidth = Resources.getSystem().getDisplayMetrics().widthPixels - Math.round(20 * getResources().getDisplayMetrics().density);
                    screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
                    //
                    listnerFirst=false;
                }

                ViewGroup.LayoutParams params = miniPlayer.getLayoutParams();
                int height = playerOriginalHeight+Math.round((screenWidth-playerOriginalHeight)*slideOffset);
                miniPlayer.getLayoutParams().height = height;
                miniPlayer.setLayoutParams(params);

                //info container
                miniPlayerInfoContainer.setAlpha(1-slideOffset*5);

                //play button
                miniPlayerPlaybtn.setAlpha(1-slideOffset*5);


                //cover image
                int width = miniPlayerCoverOriginalWidth +
                        Math.round((miniPlayerCoverMaxWidth - miniPlayerCoverOriginalWidth)*slideOffset);
                ViewGroup.LayoutParams paramss = miniPlayerCover.getLayoutParams();
                paramss.height = width;
                paramss.width = width;
                miniPlayerCover.setLayoutParams(paramss);
                //player controls
                playerControl.setAlpha(slideOffset);


            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if(newState== SlidingUpPanelLayout.PanelState.EXPANDED)updatePlayer(playerCurrentSong);

            }
        });
    }

    public void reloadViews(int p){
        switch(p){
            case 0:
                searchBox = findViewById(R.id.searchBox);
                resultList = findViewById(R.id.resultList);
                switch(searchStage){
                    case 0:
                        searchAdapter = new CustomAdapter(this);
                        resultList.setAdapter(searchAdapter);
                        break;
                    case 1:
                        resultList.setAdapter(searchAdapter);
                        break;
                }

                searchButton = findViewById(R.id.searchButton);
                searchButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String query = searchBox.getText().toString();
                        if(query.length()>0) {
                            searchAll search = new searchAll();
                            search.execute(query);
                        }
                    }
                });
                setListOnClick();
                break;
            case 1:
                resultList = findViewById(R.id.likesList);
                likesAdapter = new CustomAdapter(this);
                resultList.setAdapter(likesAdapter);
                try {
                    new fetchLikes().execute(sessionData.getInt("_mid"));
                }catch (Exception e){
                    //
                }
                setListOnClick();
                break;
            case 2:
                //playListView = findViewById(R.id.playList);
                //playListView.setAdapter(playlistAdapter);
                try {
                    new fetchPlaylist().execute(sessionData.getInt("_mid"));
                }catch (Exception e){
                    //
                }
                break;
            case 3:
                break;
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        //if panel open : collapse
        if(panel.getPanelState()== SlidingUpPanelLayout.PanelState.EXPANDED)
            panel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);

        // Handle navigation view item clicks here.

        int id = item.getItemId();
        if (id == R.id.search) {
            //search mode
            contentHolder.setDisplayedChild(0);
            reloadViews(0);
            mode=0;
        } else if (id == R.id.liked) {
            //liked mode
            contentHolder.setDisplayedChild(1);
            reloadViews(1);
            mode=1;
        } else if (id == R.id.playList) {
            contentHolder.setDisplayedChild(2);
            reloadViews(2);
            mode=2;
        } else if (id == R.id.logIn) {
            //login mode wip
            /*
            sessionKey = "";
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getString(R.string.session_file_key), "");
            editor.commit();
            */
            if(item.getTitle().toString().matches("로그인")) {
                Intent loginTask = new Intent(this, LoginActivity.class);
                startActivityForResult(loginTask, loginTaskRequestId);
            }else{
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(getString(R.string.session_file), "");
                editor.commit();
                sessionData=null;
                refreshUserData();
                item.setTitle("로그인");
            }


        } else if (id == R.id.checkUpdate) {
            new updateCheck().execute();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == loginTaskRequestId) {
            if(resultCode == Activity.RESULT_OK){
                //write key to pref data
                String returnedjson = data.getStringExtra("userdata");
                try{
                    sessionData = new JSONObject(returnedjson);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getString(R.string.session_file), returnedjson);
                    editor.commit();
                    refreshUserData();
                    MenuItem loginbtn = menuNav.findItem(R.id.logIn);
                    loginbtn.setTitle("로그아웃");
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }

    private class searchAll extends AsyncTask<String, String, String> {
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.show();
            searchAdapter = new CustomAdapter(MainActivity.this);

        }

        protected String doInBackground(String... params) {
            try {
                JSONObject data = new JSONObject(httpGet("http://utaitebox.com/api/search/all/"+ URLEncoder.encode(params[0], "UTF-8")));
                JSONArray songs = data.getJSONArray("music");


                searchAdapter.addSectionHeaderItem(new Item(0,"노래",-1,null,null,null));
                for (int i=0; i<songs.length();i++){
                    JSONObject obj = songs.getJSONObject(i);
                    int id = obj.getInt("_source_id");
                    String name = obj.getString("song_original");
                    searchAdapter.addItem(new Item(id,name,0,"null",null,null));
                }
                searchAdapter.addSectionHeaderItem(new Item(0,"아티스트",-1,null,null, null));
                JSONArray artists = data.getJSONArray("artist");
                for (int i=0; i<artists.length();i++){
                    JSONObject obj = artists.getJSONObject(i);
                    int id = obj.getInt("_aid");
                    String name = obj.getString("artist_en");
                    String thumb = obj.getString("artist_cover");
                    searchAdapter.addItem(new Item(id,null,1,thumb,null,name));
                }

            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }

            resultList.setAdapter(searchAdapter);
            searchStage=1;
        }
    }





    private class updateCheck extends AsyncTask<Void, Integer, Integer> {
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("업데이트 확인중");
            pd.setCancelable(false);
            pd.show();
        }

        protected Integer doInBackground(Void... params) {
            try {
                String rawdata = httpsGet("https://raw.githubusercontent.com/junheah/UtaiteBox-Downloader/master/versioninfo.json");
                JSONObject data = new JSONObject(rawdata);
                int lver = data.getInt("version");
                String link = data.getString("link");
                if(version<lver){
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    startActivity(browserIntent);
                    return 1;
                }
            }catch(Exception e){
                return -1;
            }return 0;
        }
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            switch(result){
                case -1:
                    Toast.makeText(getApplicationContext(), "오류가 발생했습니다. 나중에 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                    break;
                case 0:
                    Toast.makeText(getApplicationContext(), "최신버전 입니다.", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(getApplicationContext(), "새로운 버전을 찾았습니다. 다운로드 페이지로 이동합니다.", Toast.LENGTH_SHORT).show();
                    break;
            }
            if (pd.isShowing()){
                pd.dismiss();
            }
        }
    }


    private class selectSong extends AsyncTask<Integer, String, String> {
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.show();
            resultAdapter = new CustomAdapter(MainActivity.this);
            resultAdapter.addSectionHeaderItem(new Item(0,"검색결과",-1,null,null, null));

        }

        protected String doInBackground(Integer... params) {
            //String data = jsonGet("http://utaitebox.com/api/source/"+params[0]);
            //다국어로 제목 불러올 수 있음
            int index=0;
            while (true) {
                index++;
                String rawdata = httpGet("http://utaitebox.com/api/source/" + params[0] + "/list/" + index);
                if(rawdata.matches("\\{\"status\":1\\}")) break;
                try {
                    JSONArray data = new JSONArray(rawdata);
                    //searchAdapter.addSectionHeaderItem(new Item(0,"노래",-1,null));
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        int id = obj.getInt("_source_id");
                        String artist = obj.getString("artist_en");
                        String name = obj.getString("song_original");
                        String thumb = obj.getString("cover");
                        String key = obj.getString("key");
                        resultAdapter.addItem(new Item(id, name, 2, thumb, key, artist));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }
            resultList.setAdapter(resultAdapter);
            searchStage=2;
        }
    }
    private class selectArtist extends AsyncTask<Integer, String, String> {
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.show();
            resultAdapter = new CustomAdapter(MainActivity.this);
            resultAdapter.addSectionHeaderItem(new Item(0,"검색결과",-1,null,null,null));

        }

        protected String doInBackground(Integer... params) {
            //String data = httpGet("http://utaitebox.com/api/source/"+params[0]);
            //다국어로 제목 불러올 수 있음
            int index=0;
            while (true) {
                index++;
                String rawdata = httpGet("http://utaitebox.com/api/artist/" + params[0] + "/list/" + index);
                if(rawdata.matches("\\{\"status\":1\\}")) break;
                try {
                    JSONArray data = new JSONArray(rawdata);
                    //searchAdapter.addSectionHeaderItem(new Item(0,"노래",-1,null));
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        int id = obj.getInt("_source_id");
                        String artist = obj.getString("artist_en");
                        String name = obj.getString("song_original");
                        String thumb = obj.getString("cover");
                        String key = obj.getString("key");
                        resultAdapter.addItem(new Item(id, name, 2, thumb, key,artist));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }
            resultList.setAdapter(resultAdapter);
            searchStage=2;
        }
    }


    public String httpGet(String urlin){
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(urlin);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept-Encoding", "*");
            connection.setRequestProperty("Accept", "*");
            try {
                if (sessionData != null) {
                    connection.setRequestProperty("Authorization", sessionData.getString("token"));
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            connection.connect();


            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            return buffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public String httpsGet(String urlin){
        HttpsURLConnection connection = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlin);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept-Encoding", "*");
            connection.setRequestProperty("Accept", "*");
            connection.connect();
            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            return buffer.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void refreshUserData(){
        View header = navigationView.getHeaderView(0);
        TextView huser = header.findViewById(R.id.headerUser);
        ImageView hthumb = header.findViewById(R.id.headerThumb);
        ImageView hcover = header.findViewById(R.id.headerCover);
        if(sessionData==null){
            huser.setText("Guest");
            hthumb.setImageResource(R.drawable.default_artist);
            Glide.with(this).load("").into(hcover);
            menuNav.getItem(0).setChecked(true);
            menuNav.getItem(1).setEnabled(false);
            menuNav.getItem(2).setEnabled(false);
            searchAdapter=null;
            likesAdapter=null;
            mode=0;
            contentHolder.setDisplayedChild(0);
            reloadViews(0);
        }else {
            try {
                String cover = sessionData.getString("cover");
                if(!cover.matches("null")){
                    cover = "http://utaitebox.com/res/profile/cover/" + cover;
                }
                String avatar = sessionData.getString("avatar");
                if (avatar.matches("null")) {
                    avatar = "http://utaitebox.com/images/profile.png";
                } else {
                    avatar = "http://utaitebox.com/res/profile/image/" + avatar;
                }
                Glide.with(this).load(cover).centerCrop().into(hcover);
                huser.setText(sessionData.getString("username"));
                Glide.with(this).load(avatar).into(hthumb);
                menuNav.getItem(1).setEnabled(true);
                menuNav.getItem(2).setEnabled(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private class fetchLikes extends AsyncTask<Integer, String, String> {
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.show();
            likesAdapter = new CustomAdapter(MainActivity.this);
            likesAdapter.addSectionHeaderItem(new Item(0,"좋아요",-1,null,null,null));
        }

        protected String doInBackground(Integer... params) {
            //String data = jsonGet("http://utaitebox.com/api/source/"+params[0]);
            //다국어로 제목 불러올 수 있음

            String rawdata = httpGet("http://utaitebox.com/api/member/" + params[0] + "/ribbon");

            try {
                JSONArray data = new JSONArray(rawdata);
                //searchAdapter.addSectionHeaderItem(new Item(0,"노래",-1,null));
                for (int i = 0; i < data.length(); i++) {
                    JSONObject obj = data.getJSONObject(i);
                    String artist = obj.getString("artist_en");
                    String name = obj.getString("song_original");
                    String thumb = obj.getString("cover");
                    String key = obj.getString("key");
                    likesAdapter.addItem(new Item(0, name, 2, thumb, key,artist));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }
            resultList.setAdapter(likesAdapter);
        }
    }
    private class fetchPlaylist extends AsyncTask<Integer, String, Void> {
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.show();
            //playlistAdapter.addSectionHeaderItem(new Item(0,"플레이리스트",-1,null,null,null));
        }

        protected Void doInBackground(Integer... params) {
            //String data = jsonGet("http://utaitebox.com/api/source/"+params[0]);
            //다국어로 제목 불러올 수 있음
            playlistItems = new ArrayList<>();
            String rawdata = httpGet("http://utaitebox.com/api/member/" + params[0] + "/playlist");

            try {
                JSONArray data = new JSONArray(rawdata);
                //searchAdapter.addSectionHeaderItem(new Item(0,"노래",-1,null));
                for (int i = 0; i < data.length(); i++) {
                    JSONObject obj = data.getJSONObject(i);
                    String artist = obj.getString("artist_en");
                    String name = obj.getString("song_original");
                    String thumb = obj.getString("cover");
                    String key = obj.getString("key");
                    playlistItems.add(new Item(0, name, 2, thumb, key,artist));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }
            listAdapter.swap(playlistItems);
            //listAdapter.notifyDataSetChanged();

        }
    }

    public void refreshSettings(){
        settingsString = sharedPref.getString(getString(R.string.settings),"");
        if(settingsString.matches("")) {
            //data is empty
        }else{
            try{
                settings = new JSONObject(settingsString);
                //todo handle settings
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

}
