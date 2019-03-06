package com.mad.wordly;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.transitionseverywhere.ChangeBounds;
import com.transitionseverywhere.ChangeImageTransform;
import com.transitionseverywhere.TransitionManager;
import com.transitionseverywhere.TransitionSet;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "Word-ly";
    private static final int REQUEST_INTERNET_ACCESS = 200;
    private boolean permissionInternetAccepted = false;
    private String [] permissions = {Manifest.permission.INTERNET};

    // Views
    protected EditText wordInput;
    protected View gradientView;
    protected TextView logoTW;
    protected TextView howTW;
    protected TextView tapToPlayTW;
    protected TextView hintTW;
    protected TextView startWordTW;
    protected TextView lastWordTW;

    protected boolean howPressed;
    protected Animation anim;


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
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_INTERNET_ACCESS);

        gradientView = (View) findViewById(R.id.gradientPreloaderView);
        logoTW = (TextView) findViewById(R.id.logo);
        howTW = (TextView) findViewById(R.id.how);
        tapToPlayTW = (TextView) findViewById(R.id.tap);
        hintTW = (TextView) findViewById(R.id.hint);
        startWordTW = (TextView) findViewById(R.id.begin);
        lastWordTW = (TextView) findViewById(R.id.finish);
        wordInput = (EditText) findViewById(R.id.input);

        // Starting gradient animation
        startAnimation();

        howPressed = false;
        howTW = (TextView) findViewById(R.id.how);
        howTW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                logoTW = (TextView) findViewById(R.id.logo);
                howTW = (TextView) findViewById(R.id.how);

                if (!howPressed) {
                    howPressed = true;
                    logoTW.setTextColor(Color.parseColor("#ffffff"));
                    howTW.setText("X");
                    howTW.setTextColor(Color.parseColor("#ffffff"));
                    fullScreen();
                } else {
                    howPressed = false;
                    logoTW.setTextColor(Color.parseColor("#000000"));
                    howTW.setText("?");
                    howTW.setTextColor(Color.parseColor("#000000"));
                    initialScreen();
                }
            }
        });

        anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(2500);
        anim.setStartOffset(20);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        tapToPlayTW.startAnimation(anim);

        gradientView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                pressedPlay();
                fullScreen();
            }
        });

        hintTW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pressedHint();
            }
        });

        wordInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if(actionId==EditorInfo.IME_ACTION_DONE){
                    wordSubmitted();
                    return true;
                }
                return false;
            }
        });
    }

    public void wordSubmitted() {
        final ViewGroup transitionsContainer = (ViewGroup) findViewById(R.id.transitions_container);
        String word = wordInput.getText().toString();

        if (word == word) { // fix this

            startAnimation();

            startWordTW.setText( word );
            startWordTW.setVisibility( View.VISIBLE );
        }
    }

    public void pressedHint() {
        TextView hintText = (TextView) findViewById(R.id.hintText);
        ImageView image = findViewById(R.id.display);

        final int semiTransparentGrey = Color.argb(215, 255, 255, 255);
        image.setColorFilter(semiTransparentGrey, PorterDuff.Mode.SRC_ATOP);

        TranslateAnimation animate = new TranslateAnimation(
                0,
                0,
                image.getHeight(),
                0);
        animate.setDuration(2500);
        animate.setFillAfter(true);
        getImage(image, "flower", 5);
        image.startAnimation(animate);
        hintText.startAnimation(animate);
        hintText.setVisibility(View.VISIBLE);
    }

    public void pressedPlay() {
        final ViewGroup transitionsContainer = (ViewGroup) findViewById(R.id.transitions_container);
        hintTW.setVisibility(View.VISIBLE);
        logoTW.setTextColor(Color.parseColor("#ffffff"));
        howTW.setVisibility(View.GONE);
        tapToPlayTW.clearAnimation();
        tapToPlayTW.setVisibility(View.GONE);

        TransitionManager.beginDelayedTransition(transitionsContainer);
        wordInput.setVisibility(View.VISIBLE);
        InputMethodManager imm = (InputMethodManager) gradientView.getContext().getSystemService( Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(gradientView.getWindowToken(), 0);
        startWordTW.setVisibility(View.VISIBLE);
        lastWordTW.setVisibility(View.VISIBLE);

        new CountDownTimer(65000, 1000) {

            public void onTick(long millisUntilFinished) {
                logoTW.setText(Integer.toString((int)(millisUntilFinished / 1000)));
            }

            public void onFinish() {

            }
        }.start();
    }


    // Gradient background methods
    protected String gradients[][] =
            {
                    {"#56A47B", "#E95871", "#FDEE87" },
                    {"#7949F8", "#80D1DE", "#DDB8FE"},
                    {"#933FA1", "#F52330", "#FA9B4A"}
            };

    public void startAnimation() {
        Random rand = new Random();
        int r = rand.nextInt(gradients.length);
        final int start = Color.parseColor(gradients[r][0]);
        final int mid = Color.parseColor(gradients[r][1]);
        final int end = Color.parseColor(gradients[r][2]);


        final ArgbEvaluator evaluator = new ArgbEvaluator();
        View preloader = (View) findViewById(R.id.gradientPreloaderView);
        preloader.setVisibility(View.VISIBLE);
        final GradientDrawable gradient = (GradientDrawable) preloader.getBackground();

        ValueAnimator animator = TimeAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(5000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                Float fraction = valueAnimator.getAnimatedFraction();
                int newStrat = (int) evaluator.evaluate(fraction, start, end);
                int newMid = (int) evaluator.evaluate(fraction, mid, start);
                int newEnd = (int) evaluator.evaluate(fraction, end, mid);
                int[] newArray = {newStrat, newMid, newEnd};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    gradient.setColors(newArray);
                }
            }
        });

        animator.start();
    }

    public void stopAnimation(){
        ObjectAnimator.ofFloat((View) findViewById(R.id.gradientPreloaderView), "alpha", 0f).setDuration(125).start();
    }

    public void fullScreen() {

        tapToPlayTW.clearAnimation();
        tapToPlayTW.setVisibility(View.GONE);
        gradientView.setClickable(false);

        final ViewGroup transitionsContainer = (ViewGroup) findViewById(R.id.transitions_container);
        gradientView = (View) findViewById(R.id.gradientPreloaderView);
        TransitionManager.beginDelayedTransition(transitionsContainer, new TransitionSet()
                .addTransition(new ChangeBounds())
                .addTransition(new ChangeImageTransform()));

        ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) gradientView.getLayoutParams();
        marginParams.setMargins(0, 0, 0, 0);
        ViewGroup.LayoutParams params = gradientView.getLayoutParams();
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        gradientView.setLayoutParams(params);
    }

    public void initialScreen() {

        tapToPlayTW.startAnimation(anim);
        tapToPlayTW.setVisibility(View.VISIBLE);
        gradientView.setClickable(true);

        final ViewGroup transitionsContainer = (ViewGroup) findViewById(R.id.transitions_container);
        final float scale = getResources().getDisplayMetrics().density;
        int height = (int) (520 * scale + 0.5f);
        int width = (int) (520 * scale + 0.5f);

        gradientView = (View) findViewById(R.id.gradientPreloaderView);
        TransitionManager.beginDelayedTransition(transitionsContainer, new TransitionSet()
                .addTransition(new ChangeBounds())
                .addTransition(new ChangeImageTransform()));

        ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) gradientView.getLayoutParams();
        marginParams.setMargins((int) (25 * scale + 0.5f), (int) (125 * scale + 0.5f), (int) (25 * scale + 0.5f), 0);
        ViewGroup.LayoutParams params = gradientView.getLayoutParams();
        params.height = height;
        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        gradientView.setLayoutParams(params);
        gradientView.setLayoutParams(marginParams);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }


}
