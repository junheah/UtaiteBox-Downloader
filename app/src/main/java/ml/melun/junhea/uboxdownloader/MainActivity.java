package ml.melun.junhea.uboxdownloader;

import android.Manifest;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static int version = 1;
    DownloadManager dlManager;
    String sessionKey,userName;
    int searchMode = 0;
    SharedPreferences sharedPref;
    Button searchButton;
    ListView resultList;
    EditText searchBox;
    ProgressDialog pd;
    JSONObject data;
    CustomAdapter mAdapter;
    ArrayList<Long> dllist= new ArrayList<>();
    NotificationCompat.Builder stat;
    NotificationManagerCompat notificationManager;
    BroadcastReceiver onComplete;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationManager = NotificationManagerCompat.from(MainActivity.this);
        dlManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        setContentView(R.layout.activity_main);
        searchBox = findViewById(R.id.searchBox);
        resultList = findViewById(R.id.resultList);
        searchButton = findViewById(R.id.searchButton);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
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

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);



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
        sessionKey = sharedPref.getString(getString(R.string.session_file_key),"");
        if(sessionKey.matches("")) {
            //key is empty, login
            /*
            sessionKey = "the key that you get";
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getString(R.string.session_file_key), sessionKey);
            editor.commit();
            */
        }else{

        }

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
        resultList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Item item = mAdapter.getItem(position);
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
                        Item dlitem = mAdapter.getItem(position);
                        String dlkey = dlitem.getKey();
                        String dltitle = dlitem.getName();
                        Uri dlurl = Uri.parse("http://utaitebox.com/api/play/stream/"+dlkey);
                        DownloadManager.Request request = new DownloadManager.Request(dlurl);
                        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                        request.setAllowedOverRoaming(false);
                        request.setTitle("우타이테 박스 다운로더");
                        request.setDescription(dltitle);
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
                            .setSmallIcon(R.drawable.ic_launcher_foreground);
                    notificationManager.notify(13155431, stat.build());
                    Toast.makeText(getApplicationContext(), "다운로드 완료", Toast.LENGTH_SHORT).show();
                }

            }
        };
        registerReceiver(onComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));



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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.search) {
            //search mode
        } else if (id == R.id.playList) {
            //playlist mode
        } else if (id == R.id.logIn) {
            //login mode wip
            sessionKey = "";
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getString(R.string.session_file_key), "");
            editor.commit();
        } else if (id == R.id.checkUpdate) {
            new updateCheck().execute();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private class searchAll extends AsyncTask<String, String, String> {
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.show();
            mAdapter = new CustomAdapter(MainActivity.this);
        }

        protected String doInBackground(String... params) {
            try {
                JSONObject data = new JSONObject(httpGet("http://utaitebox.com/api/search/all/"+ URLEncoder.encode(params[0], "UTF-8")));
                JSONArray songs = data.getJSONArray("music");


                mAdapter.addSectionHeaderItem(new Item(0,"노래",-1,null,null));
                for (int i=0; i<songs.length();i++){
                    JSONObject obj = songs.getJSONObject(i);
                    int id = obj.getInt("_source_id");
                    String name = obj.getString("song_original");
                    mAdapter.addItem(new Item(id,name,0,"null",null));
                }
                mAdapter.addSectionHeaderItem(new Item(0,"아티스트",-1,null,null));
                JSONArray artists = data.getJSONArray("artist");
                for (int i=0; i<artists.length();i++){
                    JSONObject obj = artists.getJSONObject(i);
                    int id = obj.getInt("_aid");
                    String name = obj.getString("artist_en");
                    String thumb = obj.getString("artist_cover");
                    mAdapter.addItem(new Item(id,name,1,thumb,null));
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

            resultList.setAdapter(mAdapter);
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
                String rawdata = httpsGet("https://raw.githubusercontent.com/junheah/UtaiteBox-Downloader/master/README.md");
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
            mAdapter = new CustomAdapter(MainActivity.this);
            mAdapter.addSectionHeaderItem(new Item(0,"검색결과",-1,null,null));
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
                    //mAdapter.addSectionHeaderItem(new Item(0,"노래",-1,null));
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        int id = obj.getInt("_source_id");
                        String artist = obj.getString("artist_en");
                        String name = obj.getString("song_original");
                        String thumb = obj.getString("cover");
                        String key = obj.getString("key");
                        mAdapter.addItem(new Item(id, artist + " - " + name, 2, thumb, key));
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
            resultList.setAdapter(mAdapter);
        }
    }
    private class selectArtist extends AsyncTask<Integer, String, String> {
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("로드중");
            pd.setCancelable(false);
            pd.show();
            mAdapter = new CustomAdapter(MainActivity.this);
            mAdapter.addSectionHeaderItem(new Item(0,"검색결과",-1,null,null));
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
                    //mAdapter.addSectionHeaderItem(new Item(0,"노래",-1,null));
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject obj = data.getJSONObject(i);
                        int id = obj.getInt("_source_id");
                        String artist = obj.getString("artist_en");
                        String name = obj.getString("song_original");
                        String thumb = obj.getString("cover");
                        String key = obj.getString("key");
                        mAdapter.addItem(new Item(id, artist + " - " + name, 3, thumb, key));
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
            resultList.setAdapter(mAdapter);
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
}
