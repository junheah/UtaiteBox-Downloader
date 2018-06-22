package ml.melun.junhea.uboxdownloader;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.json.JSONObject;

public class SettingsActivity extends AppCompatActivity {
    //todo settings
    String settingsString;
    SharedPreferences sharedPref;
    JSONObject settings;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        settingsString = sharedPref.getString(getString(R.string.settings),"");
        if(settingsString.matches("")) {

        }else{
            try {
                settings = new JSONObject(settingsString);
            }catch (Exception e){
                //
            }
        }
    }
}
