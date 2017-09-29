package com.example.asus.webhtml;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, LoaderManager.LoaderCallbacks<String>, View.OnClickListener {
    private Spinner mSpinner;
    private ArrayAdapter<CharSequence> mAdapter;
    private Button mButton;
    private TextView mTextView;
    private EditText mEditText;
    private TextView mTextWeb;
    private ProgressBar mBar;
    private FrameLayout.LayoutParams mParams;


    private static final int HTTP = 0;
    private static final int HTTPS = 1;
    private static boolean mIndicator = false;
    private static final String _URL = "url";
    private static final int ID = 0;
    private int mScheme = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEditText = findViewById(R.id.edit_url);
        mTextView = findViewById(R.id.result);
        mTextWeb = findViewById(R.id.web);
        mBar = findViewById(R.id.prog);
        FrameLayout layout = findViewById(R.id.frame);
        mParams = (FrameLayout.LayoutParams) layout.getLayoutParams();
        if (savedInstanceState != null) {
            mTextView.setText(savedInstanceState.getString("text1"));
            mTextWeb.setText(savedInstanceState.getString("text2"));

        }
        if (mIndicator) {
            hideShow(true);
            getSupportLoaderManager().initLoader(ID, null, this);
        } else {
            hideShow(false);
        }
        setUpSpinner();
        setUpButton();
    }

    private void setUpSpinner() {
        mSpinner = findViewById(R.id.spinner);
        mAdapter = ArrayAdapter.createFromResource(this, R.array.list, android.R.layout.simple_spinner_item);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setOnItemSelectedListener(this);
    }

    private void setUpButton() {
        mButton = findViewById(R.id.get);
        mButton.setOnClickListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("text1", mTextView.getText().toString());
        outState.putString("text2", mTextWeb.getText().toString());

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
            mParams.gravity = Gravity.LEFT;
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
        mTextView.setText("");
    }

    @Override
    public void onClick(View view) {
        String url = mEditText.getText().toString();
        if (mScheme == HTTP) {
            url = "http://" + url;
        } else {
            url = "https://" + url;
        }
        mTextWeb.setText("URL : " + url);
        boolean valid = Patterns.WEB_URL.matcher(url).matches();
        if (!valid) {
            Loader loader = getSupportLoaderManager().getLoader(ID);
            if (loader != null) {
                loader.cancelLoad();
                Log.v("Main", "Canceled");
            }
            mTextView.setText("URL INVALID");
            mIndicator = false;
            hideShow(false);
        } else {
            if (checkConnection()) {
                Bundle bundle = new Bundle();
                bundle.putString(_URL, url);
                getSupportLoaderManager().restartLoader(ID, bundle, this);
            } else {
                Loader loader = getSupportLoaderManager().getLoader(ID);
                if (loader != null) {
                    loader.cancelLoad();
                }
                mTextView.setText("NO INTERNET CONNECTION");
                mIndicator = false;
                hideShow(false);
            }
        }
    }

    private boolean checkConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();//check the connection
    }

}

class HtmlTaskLoader extends AsyncTaskLoader<String> {
    private String mResult;
    private String mURL;


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

        }catch (MalformedURLException ex){
            return "URL INVALID";
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return "Unknown Error";
        }
        return result;
    }

    @Override
    protected void onStartLoading() {
        if (mResult == null) {
            forceLoad();
        } else {
            deliverResult(mResult);
        }
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
                result = readByteString(inputStream);
            } else {
                return "Error Response Code " + connection.getResponseCode();
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            connection.disconnect();
            ;
        }
        return result;
    }

    private String readByteString(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                builder.append(line);
                line = reader.readLine();
            }
            return builder.toString();
        } else {
            return null;
        }
    }
}


