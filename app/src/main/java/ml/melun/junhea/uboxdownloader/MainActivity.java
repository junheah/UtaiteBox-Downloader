package ml.melun.junhea.uboxdownloader;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.bumptech.glide.Glide;

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

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static int version;
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
    CustomAdapter likesAdapter;
    CustomAdapter playlistAdapter;
    ArrayList<Long> dllist= new ArrayList<>();
    NotificationCompat.Builder stat;
    NotificationManagerCompat notificationManager;
    BroadcastReceiver onComplete;
    ViewFlipper contentHolder;
    Toolbar toolbar;
    int mode =0;
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
        notificationManager = NotificationManagerCompat.from(MainActivity.this);
        dlManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        setContentView(R.layout.activity_main);
        searchBox = findViewById(R.id.searchBox);
        resultList = findViewById(R.id.resultList);
        searchButton = findViewById(R.id.searchButton);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        contentHolder = findViewById(R.id.contentHolder);
        setSupportActionBar(toolbar);
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
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //get menuview
        menuNav = navigationView.getMenu();

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

            public void onReceive(Context ctxt, Intent intent) {
                long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                dllist.remove(referenceId);
                if (dllist.isEmpty())
                {
                    stat = new NotificationCompat.Builder(MainActivity.this, "UtaiteBox Downloader");
                    stat.setContentTitle("우타이테 박스 다운로더")
                            .setContentText("모든 다운로드가 완료되었습니다")
                            .setPriority(NotificationCompat.PRIORITY_LOW)
                            .setOngoing(false)
                            .setSmallIcon(R.drawable.ic_launcher_background);
                    notificationManager.notify(13155431, stat.build());
                    Toast.makeText(getApplicationContext(), "다운로드 완료", Toast.LENGTH_SHORT).show();
                }

            }
        };
        registerReceiver(onComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));


        reloadViews(0);
    }
    public void setListOnClick(){
        resultList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                CustomAdapter tAdapter = null;
                switch(mode){
                    case 0: tAdapter = searchAdapter; break;
                    case 1: tAdapter = likesAdapter; break;
                    case 2: tAdapter = playlistAdapter; break;
                }
                Item item = tAdapter.getItem(position);
                System.out.println(item.getName()+","+item.getType());
                switch(item.getType()){
                    case 0:
                        int sid = item.getId();
                        new selectSong().execute(sid);
                        break;
                    case 1:
                        int aid = item.getId();
                        new selectArtist().execute(aid);
                        break;
                    case 2:
                    case 3:
                        Toast.makeText(getApplicationContext(), "다운로드를 시작합니다.", Toast.LENGTH_SHORT).show();
                        try{
                            Item dlitem = tAdapter.getItem(position);
                            String dlkey = dlitem.getKey();
                            String dltitle = dlitem.getName();
                            Uri dlurl = Uri.parse("http://utaitebox.com/api/play/stream/"+dlkey);
                            DownloadManager.Request request = new DownloadManager.Request(dlurl);
                            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                            request.setAllowedOverRoaming(false);
                            request.setTitle(dltitle);
                            request.setDescription("우타이테 박스 다운로더");
                            request.setVisibleInDownloadsUi(true);
                            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "/UtaiteBoxDownloads/"+dltitle+".mp3");
                            dllist.add(dlManager.enqueue(request));
                            break;
                        }catch(Exception e){
                            e.printStackTrace();
                        }

                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onComplete);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.check_update) {
            new updateCheck().execute();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void reloadViews(int p){
        switch(p){
            case 0:
                searchBox = findViewById(R.id.searchBox);
                resultList = findViewById(R.id.resultList);
                resultList.setAdapter(searchAdapter);
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
                resultList.setAdapter(likesAdapter);
                try {
                    new fetchLikes().execute(sessionData.getInt("_mid"));
                }catch (Exception e){
                    //
                }
                setListOnClick();
                break;
            case 2:
                resultList = findViewById(R.id.likesList);
                resultList.setAdapter(playlistAdapter);
                try {
                    new fetchPlaylist().execute(sessionData.getInt("_mid"));
                }catch (Exception e){
                    //
                }
                setListOnClick();
                break;
            case 3:
                break;
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
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
            //playlist mode (basically liked mode
            contentHolder.setDisplayedChild(1);
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
                //Todo login function = parse user data and populate UI

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


                searchAdapter.addSectionHeaderItem(new Item(0,"노래",-1,null,null));
                for (int i=0; i<songs.length();i++){
                    JSONObject obj = songs.getJSONObject(i);
                    int id = obj.getInt("_source_id");
                    String name = obj.getString("song_original");
                    searchAdapter.addItem(new Item(id,name,0,"null",null));
                }
                searchAdapter.addSectionHeaderItem(new Item(0,"아티스트",-1,null,null));
                JSONArray artists = data.getJSONArray("artist");
                for (int i=0; i<artists.length();i++){
                    JSONObject obj = artists.getJSONObject(i);
                    int id = obj.getInt("_aid");
                    String name = obj.getString("artist_en");
                    String thumb = obj.getString("artist_cover");
                    searchAdapter.addItem(new Item(id,name,1,thumb,null));
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
            searchAdapter = new CustomAdapter(MainActivity.this);
            searchAdapter.addSectionHeaderItem(new Item(0,"검색결과",-1,null,null));
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
                        searchAdapter.addItem(new Item(id, artist + " - " + name, 2, thumb, key));
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
            resultList.setAdapter(searchAdapter);
        }
    }
    private class selectArtist extends AsyncTask<Integer, String, String> {
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.show();
            searchAdapter = new CustomAdapter(MainActivity.this);
            searchAdapter.addSectionHeaderItem(new Item(0,"검색결과",-1,null,null));
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
                        searchAdapter.addItem(new Item(id, artist + " - " + name, 3, thumb, key));
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
            resultList.setAdapter(searchAdapter);
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
            Glide.with(this).load("").into(hthumb);
            Glide.with(this).load("").into(hcover);
            menuNav.getItem(1).setEnabled(false);
            menuNav.getItem(2).setEnabled(false);
        }else {
            try {
                String cover = sessionData.getString("cover");
                if(!cover.matches("null")){
                    //cover = "http://utaitebox.com/res/profile/cover/" + cover;
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
            likesAdapter.addSectionHeaderItem(new Item(0,"좋아요",-1,null,null));
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
                    likesAdapter.addItem(new Item(0, artist + " - " + name, 2, thumb, key));
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
    private class fetchPlaylist extends AsyncTask<Integer, String, String> {
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.show();
            playlistAdapter = new CustomAdapter(MainActivity.this);
            playlistAdapter.addSectionHeaderItem(new Item(0,"플레이리스트",-1,null,null));
        }

        protected String doInBackground(Integer... params) {
            //String data = jsonGet("http://utaitebox.com/api/source/"+params[0]);
            //다국어로 제목 불러올 수 있음

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
                    playlistAdapter.addItem(new Item(0, artist + " - " + name, 2, thumb, key));
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
            resultList.setAdapter(playlistAdapter);
        }
    }
}
