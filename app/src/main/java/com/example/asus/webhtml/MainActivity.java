package com.example.asus.webhtml;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, LoaderManager.LoaderCallbacks<String>, View.OnClickListener, TextView.OnEditorActionListener, View.OnTouchListener {
    private Spinner mSpinner;
    private Button mButton;
    private TextView mTextView;
    private EditText mEditText;
    private TextView mTextWeb;
    private ProgressBar mBar;
    private ScrollView mScrollView;
    private FrameLayout.LayoutParams mParams;

    private static final int HTTP = 0;
    private static final int HTTPS = 1;
    private static boolean mIndicator = false;
    private static final String _URL = "url";
    private static final String TEXT_URL = "text_url";
    private static final String TEXT_HTML = "text_html";
    private static final int ID = 0;
    private int mScheme = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initViewListener();
        if (savedInstanceState != null) {
            mTextWeb.setText(savedInstanceState.getString(TEXT_URL));
            mTextView.setText(savedInstanceState.getString(TEXT_HTML));
        }
        if (mIndicator) {
            hideShow(true);
            getSupportLoaderManager().initLoader(ID, null, this);
        } else {
            hideShow(false);
        }

    }

    private void initView() {
        mEditText = findViewById(R.id.edit_url);
        mTextView = findViewById(R.id.result);
        mTextWeb = findViewById(R.id.web);
        mBar = findViewById(R.id.prog);
        mScrollView = findViewById(R.id.scroll);
        FrameLayout layout = findViewById(R.id.frame);
        mParams = (FrameLayout.LayoutParams) layout.getLayoutParams();
        mSpinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.list, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mButton = findViewById(R.id.get);
        mEditText.requestFocus();
    }

    private void initViewListener() {
        mSpinner.setOnItemSelectedListener(this);
        mButton.setOnClickListener(this);
        mEditText.setOnEditorActionListener(this);
        mScrollView.setOnTouchListener(this);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(TEXT_URL, mTextWeb.getText().toString());
        outState.putString(TEXT_HTML, mTextView.getText().toString());
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == HTTP) {
            mScheme = HTTP;
        } else {
            mScheme = HTTPS;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private void hideShow(boolean valueBar) {
        if (valueBar) {
            mBar.setVisibility(View.VISIBLE);
            mTextView.setVisibility(View.GONE);
            mParams.gravity = Gravity.CENTER;
        } else {
            mBar.setVisibility(View.GONE);
            mTextView.setVisibility(View.VISIBLE);
            mParams.gravity = Gravity.START;
        }
    }

    @Override
    public Loader<String> onCreateLoader(int id, Bundle args) {
        mIndicator = true;
        hideShow(true);
        String url = args.getString(_URL);
        return new HtmlTaskLoader(this, url);
    }

    @Override
    public void onLoadFinished(Loader<String> loader, String data) {
        mIndicator = false;
        hideShow(false);
        if (data != null && !data.isEmpty()) {
            mTextView.setText(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {

    }

    @Override
    public void onClick(View view) {
        hideSoftKeyboard(view);
        String url = mEditText.getText().toString();
        if (mScheme == HTTP) {
            url = "http://" + url;
        } else {
            url = "https://" + url;
        }
        validateProcess(url);

    }

    private boolean checkConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();//check the connection
    }

    private void cancelLoadError(String error) {
        Loader loader = getSupportLoaderManager().getLoader(ID);
        if (loader != null) {
            loader.cancelLoad();
        }
        mTextView.setText(error);
        mIndicator = false;
        hideShow(false);
    }

    private void validateProcess(String url) {
        mTextWeb.setText("URL : " + url);
        boolean valid = Patterns.WEB_URL.matcher(url).matches();//check if the URL is valid
        if (!valid) {
            cancelLoadError("URL INVALID");
        } else {
            if (checkConnection()) {
                Bundle bundle = new Bundle();
                bundle.putString(_URL, url);
                getSupportLoaderManager().restartLoader(ID, bundle, this);
            } else {
                cancelLoadError("NO INTERNET CONNECTION");
            }
        }
    }

    @Override
    public boolean onEditorAction(TextView View, int actionId, KeyEvent keyEvent) {
        boolean handled = false;
        if (actionId == EditorInfo.IME_ACTION_GO) {
            handled = true;
            mButton.performClick();
        }
        return handled;
    }

    private void hideSoftKeyboard(View view) {
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        hideSoftKeyboard(view);
        return false;
    }
}

class HtmlTaskLoader extends AsyncTaskLoader<String> {
    private String mResult;
    private String mURL;
    private boolean mCancel = false;

    public HtmlTaskLoader(Context context, String url) {
        super(context);
        mURL = url;
    }

    @Override
    public String loadInBackground() {
        URL url;
        String result;
        try {
            url = createURL(mURL);
            result = openReadConnection(url);
        } catch (MalformedURLException ex) {
            return "URL INVALID";
        } catch (IOException ex) {
            ex.printStackTrace();
            return "UNKNOWN ERROR";
        }
        return result;
    }

    @Override
    protected void onStartLoading() {
        if (mResult == null && !mCancel) {
            forceLoad();
        } else {
            deliverResult(mResult);
        }
    }

    @Override
    public void onCanceled(String data) {
        super.onCanceled(data);
        mCancel = true;
    }

    @Override
    public void deliverResult(String data) {
        mResult = data;
        super.deliverResult(data);
    }

    private URL createURL(String url) throws MalformedURLException {
        URL url1 = new URL(url);
        return url1;
    }

    private String openReadConnection(URL url) throws IOException {
        InputStream inputStream = null;
        HttpURLConnection connection = null;
        String result = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(15000);
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                result = readByteToString(inputStream);
            } else {
                return "ERROR RESPONSE CODE " + connection.getResponseCode();
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            connection.disconnect();
        }
        return result;
    }

    private String readByteToString(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                builder.append(line);
                line = reader.readLine();
            }
            return builder.toString();
        }
        return null;
    }
}


