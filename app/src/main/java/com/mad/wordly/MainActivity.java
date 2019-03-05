package com.mad.wordly;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "Word-ly";
    private static final int REQUEST_INTERNET_ACCESS = 200;
    private boolean permissionInternetAccepted = false;
    private String [] permissions = {Manifest.permission.INTERNET};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_INTERNET_ACCESS:
                permissionInternetAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionInternetAccepted ) finish();

    }

    public class URLPullService extends IntentService {

        public JSONObject result;
        public static final String inputUrl = "";
        public URLPullService(){
            super("URLPullService");
        }

        @Override
        protected  void onHandleIntent(final Intent workIntent){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    URL url;
                    JSONObject myresponse = null;
                    StringBuffer response = new StringBuffer();
                    try {
                        url = new URL(workIntent.getStringExtra("download_url"));
                    } catch (MalformedURLException e) {
                        throw new IllegalArgumentException("invalid url");
                    }

                    HttpURLConnection conn = null;
                    try {
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setDoOutput(false);
                        conn.setDoInput(true);
                        conn.setUseCaches(false);
                        conn.setRequestMethod("GET");

                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (conn != null) {
                            conn.disconnect();
                        }

                        //Here is your json in string format
                        String responseJSON = response.toString();
                        try {
                            myresponse = new JSONObject(responseJSON);
                        } catch (Exception e){
                        }
                    }
                    result = myresponse;
                }
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public String getURLfromPixabay(String string, int i){

        string = "https://pixabay.com/api/?key=11734484-6b3632485027241902e65c165&q=" + string + "&image_type=photo";
        String url = "";
        try {
            Intent serviceIntent = new Intent();
            serviceIntent.putExtra("download_url", string);
            URLPullService urlPullService = new URLPullService();
            urlPullService.onHandleIntent(serviceIntent);
            JSONObject jsonObject = urlPullService.result;
            JSONArray hits = (JSONArray) jsonObject.get("hits");
            jsonObject = hits.getJSONObject(i);
            url = (String) jsonObject.get("largeImageURL");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    public void getImage(ImageView image, String string, int i) {
        String inputURL = getURLfromPixabay(string, i);
        Picasso.get().load(inputURL).into(image);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_INTERNET_ACCESS);
        ImageView image = findViewById(R.id.display);

        getImage(image, "potato", 1);
    }
}
