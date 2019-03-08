package com.mad.wordly;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TimeAnimator;
import android.animation.ValueAnimator;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.MotionEventCompat;

import com.transitionseverywhere.ChangeBounds;
import com.transitionseverywhere.ChangeImageTransform;
import com.transitionseverywhere.Rotate;
import com.transitionseverywhere.TransitionManager;
import com.transitionseverywhere.TransitionSet;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.TrustAnchor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import static androidx.core.view.MotionEventCompat.getActionMasked;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SixtySix";
    private static final int REQUEST_INTERNET_ACCESS = 200;
    private boolean permissionInternetAccepted = false;
    private String [] permissions = {Manifest.permission.INTERNET};

    // Views
    protected EditText wordInput;
    protected View gradientView;
    protected TextView logoTW;
    protected TextView howTW;
    protected TextView howLabelTW;
    protected TextView howBodyTW;
    protected TextView tapToPlayTW;
    protected TextView hintTW;
    protected TextView startWordTW;
    protected TextView lastWordTW;
    protected TextView loadLogoTW;
    protected TextView audioTW;
    protected TextView statsTW;
    protected TextView statsWinsTW;
    protected TextView statsLosesTW;
    protected TextView statsTimeTW;
    protected TextView statsWordsTW;
    protected TextView statsResetTW;
    protected TextView statsWinsNumTW;
    protected TextView statsLosesNumTW;
    protected TextView statsTimeNumTW;
    protected TextView statsWordsNumTW;
    protected ImageView hintImage;
    protected MediaPlayer mPlayer;
    protected CountDownTimer timer;
    protected SharedPreferences sharedPreferences;
    protected Vibrator v;
    protected String[] words;

    protected int numGuesses;
    protected long finalTime;
    protected ArrayList<String> allWords;

    protected boolean howPressed;
    protected boolean statsPressed;
    protected Animation anim;
    private GestureDetectorCompat mDetector;


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
        String inputURL = getURLfromPixabay( string, i );

        try {
            Picasso.get().load( inputURL ).into( image );
        } catch (IllegalArgumentException e) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        gradientView = (View) findViewById(R.id.gradientPreloaderView);
        logoTW = (TextView) findViewById(R.id.logo);
        howTW = (TextView) findViewById(R.id.how);
        howLabelTW = (TextView) findViewById(R.id.howLabel);
        howBodyTW = (TextView) findViewById(R.id.howBody);
        tapToPlayTW = (TextView) findViewById(R.id.tap);
        hintTW = (TextView) findViewById(R.id.hint);
        startWordTW = (TextView) findViewById(R.id.begin);
        lastWordTW = (TextView) findViewById(R.id.finish);
        wordInput = (EditText) findViewById(R.id.input);
        loadLogoTW = (TextView) findViewById(R.id.loadTW);
        statsTW = (TextView) findViewById(R.id.stats);
        statsWinsTW = (TextView) findViewById(R.id.wins);
        statsLosesTW = (TextView) findViewById(R.id.loses);
        statsTimeTW = (TextView) findViewById(R.id.time);
        statsWordsTW = (TextView) findViewById(R.id.words);
        statsWinsNumTW = (TextView) findViewById(R.id.winsNum);
        statsLosesNumTW = (TextView) findViewById(R.id.losesNum);
        statsTimeNumTW = (TextView) findViewById(R.id.timeNum);
        statsWordsNumTW = (TextView) findViewById(R.id.wordNum);
        statsResetTW = (TextView) findViewById(R.id.reset);
        hintImage = (ImageView) findViewById(R.id.display);
        audioTW = (TextView) findViewById(R.id.audio);
        timer = null;
        numGuesses = 1;
        sharedPreferences = getApplicationContext().getSharedPreferences("stats", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (sharedPreferences.getInt("Games", -1) == -1)
        {
            editor.putInt("Games", 0);
            editor.putInt("Wins", 0);
            editor.putInt("Loses", 0);
            editor.putInt("Guesses", 0);
            editor.putInt("Average Guesses per Game", 0);
            editor.putInt("Total time", 0);
            editor.apply();
        }
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        setGame();

        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
        mPlayer = MediaPlayer.create(this, R.raw.beat);
        mPlayer.setLooping(true);
        mPlayer.start();
        playAudio();

        getImage(hintImage, "belts", 5);

        anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(2000);
        anim.setStartOffset(20);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);

        // Starting gradient animation
        startAnimation();
        fullScreen();
        loadLogoTW.startAnimation(anim);

        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                loadScreen();
            }
        }, 3500);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_INTERNET_ACCESS);

        howPressed = false;
        howTW = (TextView) findViewById(R.id.how);
        howTW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!howPressed) {
                    howPressed = true;
                    howTW.setText("X");
                    howTW.setTextColor(Color.parseColor("#ffffff"));
                    statsTW.setVisibility(View.GONE);
                    howLabelTW.setVisibility(View.VISIBLE);
                    howBodyTW.setText("Text \nText \nText");

                    howBodyTW.setVisibility(View.VISIBLE);
                    fullScreen();
                } else {
                    howPressed = false;
                    howTW.setText("?");
                    howTW.setTextColor(Color.parseColor("#000000"));
                    statsTW.setVisibility(View.VISIBLE);
                    howLabelTW.setVisibility(View.GONE);
                    howBodyTW.setVisibility(View.GONE);
                    initialScreen();
                }
            }
        });

        statsPressed = false;
        statsTW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!statsPressed) {
                    showStats();
                } else {
                    hideStats();
                }
            }
        });

        statsResetTW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetStats();
            }
        });

        tapToPlayTW.startAnimation(anim);

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

    protected void onPause() {
        super.onPause();
        mPlayer.pause();
    }

    protected void onResume() {
        super.onResume();
        mPlayer.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }


class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = event2.getY() - event1.getY();
                float diffX = event2.getX() - event1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            timer.cancel();
                            logoTW.setText("66");
                            hidePlay();
                            initialScreen();
                        }
                        result = true;
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return result;
        }
    }

    public void showStats() {

        int wins = sharedPreferences.getInt("Wins", -1);
        int loses = sharedPreferences.getInt("Loses", -1);
        int games = sharedPreferences.getInt("Games", -1);
        int guesses = sharedPreferences.getInt("Average Guesses per Game", -1);
        statsPressed = true;
        statsTW.setText("X");
        statsTW.setTextColor(Color.parseColor("#ffffff"));
        howTW.setVisibility(View.GONE);

        statsWinsNumTW.setText(Integer.toString(wins));
        statsLosesNumTW.setText(Integer.toString(loses));
        if (games != 0) {
            statsWordsNumTW.setText(Float.toString((float)guesses/games));
            statsTimeNumTW.setText(Float.toString((float) guesses / games));
        } else {
            statsWordsNumTW.setText(Integer.toString(0));
            statsTimeNumTW.setText(Integer.toString(0));
        }
        statsWinsTW.setVisibility(View.VISIBLE);
        statsLosesTW.setVisibility(View.VISIBLE);
        statsTimeTW.setVisibility(View.VISIBLE);
        statsWordsTW.setVisibility(View.VISIBLE);
        statsWinsNumTW.setVisibility(View.VISIBLE);
        statsLosesNumTW.setVisibility(View.VISIBLE);
        statsTimeNumTW.setVisibility(View.VISIBLE);
        statsWordsNumTW.setVisibility(View.VISIBLE);
        fullScreen();
    }

    public void hideStats() {
        statsPressed = false;
        statsTW.setText("S");
        statsTW.setTextColor(Color.parseColor("#000000"));
        howTW.setVisibility(View.VISIBLE);
        statsTW.setVisibility(View.GONE);
        statsWinsTW.setVisibility(View.GONE);
        statsLosesTW.setVisibility(View.GONE);
        statsTimeTW.setVisibility(View.GONE);
        statsWordsTW.setVisibility(View.GONE);
        statsWinsNumTW.setVisibility(View.GONE);
        statsLosesNumTW.setVisibility(View.GONE);
        statsTimeNumTW.setVisibility(View.GONE);
        statsWordsNumTW.setVisibility(View.GONE);
        initialScreen();
    }

    public void resetStats() {
        // Resetting stats
    }

    public void playAudio() {

        audioTW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPlayer.isPlaying()) {
                    audioTW.setPaintFlags(audioTW.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    mPlayer.pause();
                } else {
                    mPlayer.start();
                    audioTW.setPaintFlags(audioTW.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                }
            }
        });
    }

    public void wordSubmitted() {
        final ViewGroup transitionsContainer = (ViewGroup) findViewById( R.id.transitions_container );
        String word = wordInput.getText().toString();
        numGuesses++;

        if (word.equals(lastWordTW.getText().toString())) {
            endGame(true, numGuesses, (int)finalTime/1000);
            timer.cancel();
            initialScreen();
            hidePlay();
        } else if (allWords.contains(word)) { // fix this
            startAnimation();
            startWordTW.setText( word );
            startWordTW.setVisibility( View.VISIBLE );
        } else {
            v.vibrate(500);
        }
    }

    protected boolean isRotated;

    public void pressedHint() {

        isRotated = !isRotated;
        hintTW.setRotation(isRotated ? 90 : 0);
        final ViewGroup transitionsContainer = (ViewGroup) findViewById( R.id.transitions_container );
        TransitionManager.beginDelayedTransition( transitionsContainer, new Rotate() );
        TextView hintText = (TextView) findViewById( R.id.hintText );

        if (!isRotated) {
            hintImage.animate()
                    .translationY(hintImage.getHeight())
                    .setListener(null);
            hintText.setVisibility(View.GONE);
        } else {
            final int semiTransparentGrey = Color.argb( 215, 255, 255, 255 );
            hintImage.setColorFilter( semiTransparentGrey, PorterDuff.Mode.SRC_ATOP );
            hintImage.setAlpha(0.0f);
            hintImage.setVisibility(View.VISIBLE);
            hintImage.animate()
                    .alpha(1.0f)
                    .translationY(0)
                    .setListener(null);
            hintText.setVisibility( View.VISIBLE );
        }
    }

    public void pressedPlay() {

        final ViewGroup transitionsContainer = (ViewGroup) findViewById(R.id.transitions_container);
        numGuesses = 0;
        hintTW.setVisibility(View.VISIBLE);
        logoTW.setTextColor(Color.parseColor("#ffffff"));
        howTW.setVisibility(View.GONE);
        statsTW.setVisibility(View.GONE);
        tapToPlayTW.clearAnimation();
        tapToPlayTW.setVisibility(View.GONE);

        TransitionManager.beginDelayedTransition(transitionsContainer);
        wordInput.setVisibility(View.VISIBLE);
        InputMethodManager imm = (InputMethodManager) gradientView.getContext().getSystemService( Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(gradientView.getWindowToken(), 0);
        startWordTW.setVisibility(View.VISIBLE);
        lastWordTW.setVisibility(View.VISIBLE);

        timer = new CountDownTimer(66000, 1000) {
            public void onTick(long millisUntilFinished) {
                finalTime = millisUntilFinished;
                logoTW.setText(Integer.toString((int)(millisUntilFinished / 1000)));
            }

            public void onFinish() {
                endGame(false, numGuesses, (int)finalTime/1000);
                v.vibrate(500);
                logoTW.setText("66");
                hidePlay();
                setGame();
                initialScreen();
            }
        };
        timer.start();
    }

    public void hidePlay() {
        setGame();
        wordInput.setText(null);
        logoTW.setText("66");
        hintTW.setVisibility(View.GONE);
        startWordTW.setVisibility(View.GONE);
        lastWordTW.setVisibility(View.GONE);
        wordInput.setVisibility(View.GONE);
        howTW.setVisibility(View.VISIBLE);
    }

    // Gradient background methods
    protected String gradients[][] =
            {
                    {"#56A47B", "#E95871", "#FDEE87" },
                    {"#7949F8", "#80D1DE", "#DDB8FE"},
                    {"#933FA1", "#F52330", "#FA9B4A"},
                    {"#F2B7E0", "#BF1F77", "#40139E" },
                    {"#3FD0DB", "#CE147E", "#FA1226"},
                    {"#87F276", "#CF476B", "#8D39DA"},
                    {"#FAB063", "#CE9F82", "#41A0F2" },
                    {"#F6142C", "#C5137A", "#12020A"},
                    {"#7BF2F4", "#E88A54", "#6611EC"}
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

    public void stopAnimation() {
        ObjectAnimator.ofFloat((View) findViewById(R.id.gradientPreloaderView), "alpha", 0f).setDuration(125).start();
    }

    public void loadScreen() {

        logoTW.setVisibility(View.VISIBLE);
        statsTW.setVisibility(View.VISIBLE);
        howTW.setVisibility(View.VISIBLE);
        audioTW.setVisibility(View.VISIBLE);
        loadLogoTW.clearAnimation();
        loadLogoTW.setVisibility(View.GONE);
        gradientView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                pressedPlay();
                fullScreen();
            }
        });
        initialScreen();
    }

    public void fullScreen() {

        audioTW.setVisibility(View.GONE);
        logoTW.setTextColor(Color.parseColor("#ffffff"));
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

        audioTW.setVisibility(View.VISIBLE);
        statsTW.setVisibility(View.VISIBLE);
        logoTW.setTextColor(Color.parseColor("#000000"));
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


    //Use SharedPreferences to store stats. Call SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("stats",MODE_PRIVATE); and sharedPreference.getInt("Wins", -1) to get the stored number of wins, default value being -1
    //When pass value into endGame, pass whether win/lose as a boolean and add the number of guesses made in the latest game
    public void endGame(boolean win, int numGuess, int time)
    {
        sharedPreferences = getApplicationContext().getSharedPreferences("stats", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int numGame = sharedPreferences.getInt("Games", -1);
        numGame++;
        int numWins = sharedPreferences.getInt("Wins", -1);
        int numLoses = sharedPreferences.getInt("Loses", -1);
        int totalTime = sharedPreferences.getInt("Total time", -1);
        if (win == true)
        {
            numWins++;
        } else
        {
            numLoses++;
        }
        numGuess += sharedPreferences.getInt("Guesses", -1);
        totalTime += time;
        int average = numGuess/numGame;
        editor.remove("Average Guesses per Game");
        editor.remove("Games");
        editor.remove("Wins");
        editor.remove("Loses");
        editor.remove("Guesses");
        editor.remove("Total time");
        editor.apply();
        editor.putInt("Games", numGame);
        editor.putInt("Wins", numWins);
        editor.putInt("Loses", numLoses);
        editor.putInt("Guesses", numGuess);
        editor.putInt("Average Guesses per Game", average);
        editor.putInt("Total time", totalTime);
        editor.apply();
    }

    public String[] getWords() {

        InputStream inputStream = getResources().openRawResource(R.raw.words_gwicks);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        allWords = new ArrayList<String>();
        String line = null;

        try {
            line = reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(line != null){
            if (line.length() < 6 && line.length() > 3
            ) {
                allWords.add(line);
            }
            try {
                line = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        Random rand = new Random();

        int x = rand.nextInt(allWords.size());
        String word1 = allWords.get(x);
        String word2 = null;

        for (int i = 0; i < allWords.size(); i++) {
            if (allWords.get(i).length() == word1.length() && allWords.get(i).charAt(0) == word1.charAt(0) && allWords.get(i).charAt(allWords.get(i).length() - 1) == word1.charAt(word1.length() - 1) && i != x) {
                word2 = allWords.get(i);
                break;
            }
        }
        return new String[] {word1, word2} ;

    }

    public void setGame() {
        words = getWords();
        startWordTW.setText(words[0]);
        lastWordTW.setText(words[1]);
        Log.d(TAG, Integer.toString(ladderLength(words[0],words[1])));
    }

    public int ladderLength(String beginWord, String endWord) {
        // check edge case
        if (allWords == null || !allWords.contains(endWord)) {
            setGame();
            return 0;
        }

        // build queue, visited set
        Queue<String> queue = new LinkedList<>();
        queue.offer(beginWord);
        Set<String> words = new HashSet<>(allWords);

        // process one level of queue each time, count
        int count = 1;
        while (!queue.isEmpty() && count < 6) {
            int size = queue.size();
            count++;
            for (int i = 0; i < size; i++) {
                String word = queue.poll();
                ArrayList<String> candidates = transform(words, word);
                for (String candidate: candidates) {
                    if (endWord.equals(candidate)) {
                        return count;
                    }
                    queue.offer(candidate);
                }
            }
        }
        setGame();
        return 0;
    }

    private ArrayList<String> transform(Set<String> words, String word) {
        ArrayList<String> candidates = new ArrayList<>();
        StringBuffer sb = new StringBuffer(word);
        for (int i = 0; i < sb.length(); i++) {
            char temp = sb.charAt(i);
            for (char c = 'a'; c <= 'z'; c++) {
                if (temp == c) {
                    continue;
                }
                sb.setCharAt(i, c);
                String newWord = sb.toString();
                if (words.remove(newWord)) {
                    candidates.add(newWord);
                }
            }
            sb.setCharAt(i, temp);
        }
        return candidates;
    }

}
