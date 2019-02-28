package com.mad.wordly;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import org.json.JSONException;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

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

    public String getURLfromPixabay(String string){

        string = "https://pixabay.com/api/?key=11734484-6b3632485027241902e65c165&q=" + string + "&image_type=photo";
        String url = "";
        try {
            Intent serviceIntent = new Intent();
            serviceIntent.putExtra("download_url", string);
            URLPullService urlPullService = new URLPullService();
            urlPullService.onHandleIntent(serviceIntent);
            JSONObject jsonObject = urlPullService.result;
            JSONArray hits = (JSONArray) jsonObject.get("hits");
            jsonObject = hits.getJSONObject(0);
            url = (String) jsonObject.get("largeImageURL");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return url;
    }

    //Create a picassoImageTarget
    private Target picassoImageTarget(Context context, final String imageDir, final String imageName) {
        Log.d("picassoImageTarget", " picassoImageTarget");
        ContextWrapper cw = new ContextWrapper(context);
        final File directory = cw.getDir(imageDir, Context.MODE_PRIVATE); // path to /data/data/yourapp/app_imageDir
        return new Target() {
            @Override
            public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                 Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final File myImageFile = new File(directory, imageName); // Create image file
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(myImageFile);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.i("image", "image saved to >>>" + myImageFile.getAbsolutePath());

                    }
                });
                 thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onBitmapFailed(Exception ie, Drawable errorDrawable) {
            }
            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                if (placeHolderDrawable != null) {}
            }
        };
    }

    //Get image from Pixabay and paste into a new file, stored in /data/data/com.mad.wordly/app_imageDir/my_image.png
    public void getImage(ImageView image, String string) {
        String inputURL = getURLfromPixabay(string);
        Picasso.get().load(inputURL).into(picassoImageTarget(getApplicationContext(), "imageDir", "my_image.jpeg"));
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File myImageFile = new File(directory, "my_image.jpeg");
        Picasso.get().load(myImageFile).into(image);
    }


    public void deleteImage(String filename){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File myImageFile = new File(directory, filename);
        myImageFile.delete();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_INTERNET_ACCESS);

        ImageView image = findViewById(R.id.display);
        getImage(image, "child");

    }
}
