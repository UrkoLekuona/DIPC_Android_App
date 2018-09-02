package es.ehu.dipc.dipc;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;


public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mUserView;
    private EditText mPasswordView;
    private LinearLayout mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set status bar color
        Utils.darkenStatusBar(this, R.color.colorPrimaryDark);

        // Hide keyboard.
        // (only necessary if an EditText has focus onCreate())
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // Set up the login form.
        mUserView = findViewById(R.id.username);
        mUserView.setText("");

        mPasswordView = findViewById(R.id.password);
        mPasswordView.setText("");
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mProgressBar = findViewById(R.id.linlaHeaderProgress);
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Hide keyboard.
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(findViewById(R.id.email_login_form).getWindowToken(), 0);

        // Reset errors.
        mUserView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String user = mUserView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_no_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(user)) {
            mUserView.setError(getString(R.string.error_field_required));
            focusView = mUserView;
            cancel = true;
        } else if (isUserRoot(user)) {
            mUserView.setError(getString(R.string.error_invalid_username));
            focusView = mUserView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mAuthTask = new UserLoginTask(user, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isUserRoot(String user) {
        return user.equals("root");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }


    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUser;
        private final String mPassword;
        private String mServers;
        private boolean auth = false;

        UserLoginTask(String user, String password) {
            mUser = user;
            mPassword = password;
        }

        @Override
        protected void onPreExecute() {
            // SHOW THE SPINNER WHILE LOADING FEEDS
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            String host = "ac.sw.ehu.es";
            int port=22;

            /**
             * SSH conection using JSch
             * Connects to a cluster and gets a list of available servers to the user
             * Tries to connect to a cluster first, and retries with another one until one connection succeeds
             * Local port forwarding is used to simulate nested SSH connections
             */
            try{

                JSch jsch = new JSch();

                for (int i = 0; i<3; i++) {
                    Session session = jsch.getSession(mUser, host, port);
                    session.setPassword(mPassword);

                    java.util.Properties config = new java.util.Properties();
                    config.put("StrictHostKeyChecking", "no");
                    session.setConfig(config);
                    switch (i){
                        case 0:
                            try{
                                session.delPortForwardingL(2233);
                            }catch (JSchException e){
                                System.out.println(e);
                            }
                            session.setPortForwardingL(2233, "atlas.sw.ehu.es", 22);
                            break;
                        case 1:
                            try{
                                session.delPortForwardingL(2233);
                            }catch (JSchException e){
                                System.out.println(e);
                            }
                            session.setPortForwardingL(2233, "hemera.sw.ehu.es", 22);
                            break;
                        case 2:
                            try{
                                session.delPortForwardingL(2233);
                            }catch (JSchException e){
                                System.out.println(e);
                            }
                            session.setPortForwardingL(2233, "ponto.sw.ehu.es", 22);
                            break;
                    }

                    session.setTimeout(5000);
                    try {
                        session.connect();

                        Session secondSession = jsch.getSession(mUser, "localhost", 2233);
                        secondSession.setPassword(mPassword);
                        secondSession.setConfig(config);
                        secondSession.setTimeout(10000);

                        try {
                            secondSession.connect();
                            // Get user's available server list from the cluster
                            mServers = Utils.execCommand(secondSession, "grep \"^" + mUser + ":\" /usr/local/administracion/cuentas_trabajo/usuarios.dat | cut -d':' -f6");
                            secondSession.disconnect();
                            session.delPortForwardingL(2233);
                            session.disconnect();
                            break;
                        } catch (JSchException e) {
                            System.out.println(e + "Can't connect to " + String.valueOf(i));
                            secondSession.disconnect();
                        }
                        session.delPortForwardingL(2233);
                        session.disconnect();
                    }
                    catch (JSchException e){
                        System.out.println(e);
                        try {
                            session.delPortForwardingL(2233);
                        }catch (JSchException ex){
                           System.out.println(ex);
                        }
                        session.disconnect();
                        if (e.toString().equals("com.jcraft.jsch.JSchException: Auth fail")){
                            auth = true;
                            return false;
                        }
                    }
                }


                if (mServers == null || mServers.equals("")){
                    return false;
                }

            }
            catch(JSchException e){

                System.out.println(e);
                return false;
            }


            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            mProgressBar.setVisibility(View.GONE);

            if (success) {
                Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                // Create new Intent and put the required values
                Intent intent = new Intent(LoginActivity.this, ResourcesActivity.class);
                // User and Pass to be able to connect to the servers again
                intent.putExtra("USER", mUser);
                intent.putExtra("PASS", mPassword);
                // Server list to know which servers the user has access to
                intent.putExtra("SERVERS", mServers);
                startActivity(intent);
                mPasswordView.setText("");

            } else if(auth){
                Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                mPasswordView.setError(getString(R.string.error_invalid_password));
                mPasswordView.requestFocus();
            }else{
                Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                mPasswordView.setError(getString(R.string.error_connection_failed));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
        }
    }
}

