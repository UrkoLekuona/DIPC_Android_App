package es.ehu.dipc.dipc;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.util.ArrayList;
import java.util.List;

public class DcrabActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    private LinearLayout mProgressBar;
    private GetDataTask mDataTask;
    private TextView userView;
    private TextView desiredID;
    private WebView mWB;
    private TableLayout dcrab_layout;
    private ScrollView SV;
    private HorizontalScrollView SVH;
    private NavigationView nv;
    private Button mRefresh;
    private String mUser;
    private String mPassword;
    private String mServers;
    private Spinner mJobs;
    private TextView mNoJobs;
    private int clicked_times = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dcrab);
        Utils.darkenStatusBar(this, R.color.colorPrimaryDark);

        // Some of the views may not be used depending on the approach selected to display a DCRAB report
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mProgressBar = findViewById(R.id.linlaHeaderProgress);
        mJobs = findViewById(R.id.job_dropdown);
        mNoJobs = findViewById(R.id.no_job);
        desiredID = findViewById(R.id.desired_id);
        mWB = findViewById(R.id.dcrab_web_view);
        SV = findViewById(R.id.table_scroll);
        SVH = findViewById(R.id.table_scroll_h);
        dcrab_layout = findViewById(R.id.dcrab_layout);

        Intent intent = getIntent();
        mUser = intent.getStringExtra("USER");
        mPassword = intent.getStringExtra("PASS");
        mServers = intent.getStringExtra("SERVERS");

        String[] values = mServers.split(" ");

        mRefresh = findViewById(R.id.toolbar_btn);
        mRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Refresh clicked times of the dropdown spinner
                clicked_times = 0;
                mDataTask = new GetDataTask(mUser, mPassword, 0);
                mDataTask.execute((Void) null);
            }
        });

        mDataTask = new GetDataTask(mUser, mPassword, 0);
        mDataTask.execute((Void) null);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        //actionbar.setBackgroundDrawable(new ColorDrawable(Color.rgb(102,204,0)));
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);


        nv = findViewById(R.id.nav_view);
        final Menu menu = nv.getMenu();
        int count = 1;
        for (String value : values) {
            if (value.equals("PONTO") || value.equals("HEMERA") || value.equals("ATLAS")) {
                menu.add(R.id.nav_menu, count, 1, value);
                count++;
            }
        }
        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                Intent intent;
                switch(id)
                {
                    case R.id.nav_resources:
                        intent = new Intent(DcrabActivity.this, ResourcesActivity.class);
                        intent.putExtra("USER", mUser);
                        intent.putExtra("PASS", mPassword);
                        intent.putExtra("SERVERS", mServers);
                        finish();
                        startActivity(intent);
                        return true;
                    case 1:
                        intent = new Intent(DcrabActivity.this, QueueActivity.class);
                        intent.putExtra("USER", mUser);
                        intent.putExtra("PASS", mPassword);
                        intent.putExtra("SERVERS", mServers);
                        intent.putExtra("CLUSTER", menu.findItem(1).getTitle());
                        finish();
                        startActivity(intent);
                        return true;
                    case 2:
                        intent = new Intent(DcrabActivity.this, QueueActivity.class);
                        intent.putExtra("USER", mUser);
                        intent.putExtra("PASS", mPassword);
                        intent.putExtra("SERVERS", mServers);
                        intent.putExtra("CLUSTER", menu.findItem(2).getTitle());
                        finish();
                        startActivity(intent);
                        return true;
                    case 3:
                        intent = new Intent(DcrabActivity.this, QueueActivity.class);
                        intent.putExtra("USER", mUser);
                        intent.putExtra("PASS", mPassword);
                        intent.putExtra("SERVERS", mServers);
                        intent.putExtra("CLUSTER", menu.findItem(3).getTitle());
                        finish();
                        startActivity(intent);
                        return true;
                    case R.id.nav_dcrab:
                        mDrawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    default:
                        return true;
                }
            }
        });

    }
    public class GetDataTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUser;
        private final String mPassword;
        private String jobs;
        private final int mStep;
        private String[] dcrab_output;

        GetDataTask(String user, String password, int step) {
            mUser = user;
            mPassword = password;
            mStep = step;
        }

        @Override
        protected void onPreExecute() {
            // SHOW THE SPINNER WHILE LOADING FEEDS
            mProgressBar.setVisibility(View.VISIBLE);
            mJobs.setVisibility(View.GONE);
            mNoJobs.setVisibility(View.GONE);
            desiredID.setVisibility(View.GONE);
            mRefresh.setEnabled(false);
            mWB.setVisibility(View.GONE);
            dcrab_layout.setVisibility(View.GONE);
            SVH.setVisibility(View.GONE);
            SV.setVisibility(View.GONE);
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            String host = "ac.sw.ehu.es";
            int port=22;

            try{

                JSch jsch = new JSch();
                Session session = jsch.getSession(mUser, host, port);
                session.setPassword(mPassword);

                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.setPortForwardingL(2233, "atlas.sw.ehu.es", 22);


                session.setTimeout(10000);
                session.connect();

                Session secondSession = jsch.getSession(mUser, "localhost", 2233);
                secondSession.setPassword(mPassword);
                secondSession.setConfig(config);
                secondSession.setTimeout(10000);

                try {
                    secondSession.connect(); // now we're connected to the secondary system

                    // mStep represents the state of the user in the activity. 0 for dropdown spinner state and 1 for DCRAB report state
                    if (mStep == 0) {
                        // Get job ID belonging to current user
                        jobs = Utils.getJobIds(secondSession, "export LD_LIBRARY_PATH=/usr/local/lib ; /usr/local/maui/bin/showq -u " + mUser + " | cut -d ' ' -f1");
                        //jobs = Utils.getJobIds(secondSession, "export LD_LIBRARY_PATH=/usr/local/lib ; /usr/local/maui/bin/showq | cut -d ' ' -f1");
                    }else if (mStep == 1){
                        //Using HTML DCRAB report
                        /*if (!Utils.getReport(secondSession, mJobs.getSelectedItem().toString())){
                            secondSession.disconnect();
                            session.disconnect();
                            return false;
                        }*/

                        //Using Remora style DCRAB report
                        dcrab_output = Utils.getDcrabTextOutput(secondSession, mJobs.getSelectedItem().toString());
                    }
                    secondSession.disconnect();
                }catch (JSchException e){
                    secondSession.disconnect();
                    session.delPortForwardingL(2233);
                    session.disconnect();
                    System.out.println(e);
                    return false;
                }
                session.delPortForwardingL(2233);
                session.disconnect();

            }
            catch(JSchException e){

                System.out.println(e);
                return false;
            }

            return true;
        }


        @Override
        protected void onPostExecute(final Boolean success) {
            mProgressBar.setVisibility(View.GONE);
            mRefresh.setEnabled(true);

            // If job list could be fetched
            if (success && mStep == 0) {
                // Get active job ids only
                jobs = jobs.substring(0, jobs.indexOf("IDLE"));
                // Replace all non-numeric characters with whitespace
                jobs = jobs.replaceAll("[^?0-9]+", " ");

                List<String> joblist = new ArrayList<>();
                joblist.add("Job ID");
                // Make array from string
                String[] aux = jobs.trim().split(" ");
                for (String anAux : aux) {
                    // Get only job ids (we are assuming job ids won't be smaller than 1000, to avoid active job number)
                    if (!anAux.equals("") && Integer.parseInt(anAux) > 999) {
                        joblist.add(anAux);
                    }
                }
                if (joblist.isEmpty()) {
                    mNoJobs.setText(R.string.no_jobs);
                    mNoJobs.setVisibility(View.VISIBLE);
                } else {
                    desiredID.setVisibility(View.VISIBLE);
                    mJobs.setVisibility(View.VISIBLE);
                    // Create and fill adapter for dropdown spinner
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(DcrabActivity.this, android.R.layout.simple_spinner_dropdown_item, joblist);
                    mJobs.setAdapter(adapter);
                    // Set item selected listener for dropdown spinner
                    mJobs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            // Clicked_times is necessary to avoid first item calling the listener when spinner is filled with data, as first item always gets selected when data is introduced
                            if (clicked_times++ > 0) {
                                mDataTask = new GetDataTask(mUser, mPassword, 1);
                                mDataTask.execute((Void) null);
                            }
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) { }
                    });
                }
            // If dcrab report could be fetched
            }else if (success && mStep == 1 && (dcrab_output.length > 1 || (dcrab_output.length == 1 && !dcrab_output[0].equals("")))) {
                /**
                 * There are 3 approaches implemented to get a DCRAB report
                 * First one is opening the report in device's default browser (error when generating uri for file)
                 * Second one is showing the report in a WebViewer widget (functional but needs some changes)
                 * Third one is displaying Remora style DCRAB report in a table layout (recommended)
                 */
                //Open in default browser
                /*File newFile = new File("file:///android_asset/dcrab_report.html");
                Uri uri = GenericFileProvider.getUriForFile(DcrabActivity.this, "${applicationId}.provider", newFile);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                browserIntent.setClassName("com.android.browser", "com.android.browser.BrowserActivity");
                browserIntent.setData(uri);
                browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                browserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(browserIntent);*/

                //Open in WebViewer
                /*mWB.loadUrl("file:///android_asset/dcrab_report.html");
                mWB.setVisibility(View.VISIBLE);*/

                //Remora-DCRAB report output

                int dpValue = 20; // margin in dips
                float d = DcrabActivity.this.getResources().getDisplayMetrics().density;
                int margin = (int)(dpValue * d); // margin in pixels

                // Set up views and layouts
                TextView tx;
                EditText et;
                TableRow tr;
                TableRow.LayoutParams params;
                TableRow header_row = findViewById(R.id.dcrab_table_header_row);
                dcrab_layout.removeAllViews();
                dcrab_layout.addView(header_row);
                dcrab_layout.setGravity(Gravity.CENTER);
                dcrab_layout.setPadding(0,margin*2, 0, 0);

                // For each line in the report, create a new line in our table layout and fill it with data
                for (String aDcrab_output : dcrab_output) {
                    tr = new TableRow(DcrabActivity.this);
                    dcrab_layout.addView(tr);
                    params = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, margin, 0, 0);
                    tx = new TextView(DcrabActivity.this);
                    tx.setText(aDcrab_output.substring(0, aDcrab_output.indexOf("=")));
                    tx.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                    tx.setTextColor(Color.BLACK);
                    tr.addView(tx);
                    params = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.setMargins(margin, 0, 0, 0);
                    tx.setLayoutParams(params);
                    et = new EditText(DcrabActivity.this);
                    et.setText(aDcrab_output.substring(aDcrab_output.indexOf("=") + 1));
                    tr.addView(et);
                    params = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, 0, margin, 0);
                    et.setLayoutParams(params);
                    et.setClickable(false);
                    et.setCursorVisible(false);
                    et.setFocusable(false);
                    et.setFocusableInTouchMode(false);
                }
                SV.setVisibility(View.VISIBLE);
                SVH.setVisibility(View.VISIBLE);
                dcrab_layout.setVisibility(View.VISIBLE);
            // If DCRAB report couldn't be fetched
            }else if (mStep == 1){
                Toast.makeText(DcrabActivity.this, R.string.dcrab_report_fail, Toast.LENGTH_LONG).show();
                desiredID.setVisibility(View.VISIBLE);
                mJobs.setVisibility(View.VISIBLE);
            }
            // If job list couldn't be fetched
            else{
                mNoJobs.setText(R.string.error_connection_failed);
                mNoJobs.setVisibility(View.VISIBLE);
            }

            mDataTask = null;

        }

        @Override
        protected void onCancelled() {
            mProgressBar.setVisibility(View.GONE);
            mDataTask = null;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);

                Intent intent = getIntent();
                String mUser = intent.getStringExtra("USER");

                userView = findViewById(R.id.user_View);
                userView.setText(mUser);

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed(){
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)){
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }else{
            Toast.makeText(DcrabActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
            //mDataTask = null;
            finish();
        }

    }
}
