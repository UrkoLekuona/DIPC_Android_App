package es.ehu.dipc.dipc;


import android.content.Intent;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.anychart.anychart.AnyChart;
import com.anychart.anychart.AnyChartView;
import com.anychart.anychart.Cartesian;
import com.anychart.anychart.CartesianSeriesColumn;
import com.anychart.anychart.DataEntry;
import com.anychart.anychart.EnumsAnchor;
import com.anychart.anychart.HoverMode;
import com.anychart.anychart.Position;
import com.anychart.anychart.TooltipPositionMode;
import com.anychart.anychart.ValueDataEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.util.ArrayList;
import java.util.List;


public class ResourcesActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    private TextView userView;
    private NavigationView nv;
    private LinearLayout mProgressBar, mProgressBar2;
    private GetDataTask mDataTask = null;
    private String mUser;
    private String mPassword;
    private String mServers;
    private TextView textConsumption, textScratch, textHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Utils.darkenStatusBar(this, R.color.colorPrimaryDark);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mProgressBar = findViewById(R.id.linlaHeaderProgress);
        mProgressBar2 = findViewById(R.id.linlaHeaderProgress2);
        textConsumption = findViewById(R.id.consumptionText);
        textScratch = findViewById(R.id.scratchText);
        textHome = findViewById(R.id.homeText);

        Intent intent = getIntent();
        mUser = intent.getStringExtra("USER");
        mPassword = intent.getStringExtra("PASS");
        mServers = intent.getStringExtra("SERVERS");

        String[] values = mServers.split(" ");

        // Configure toolbar to have menu button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        mDataTask = new GetDataTask(mUser, mPassword);
        mDataTask.execute((Void) null);

        // Add PONTO, HEMERA or ATLAS to navigation menu if user has access to those servers.
        nv = findViewById(R.id.nav_view);
        final Menu menu = nv.getMenu();
        int count = 1;
        for (String value : values) {
            if (value.equals("PONTO") || value.equals("HEMERA") || value.equals("ATLAS")) {
                menu.add(R.id.nav_menu, count, 1, value);
                count++;
            }
        }
        // Set specific listener for navigation menu, case 1, 2 and 3 are ids given to PONTO, HEMERA or ATLAS
        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                Intent intent;
                switch(id)
                {
                    case R.id.nav_resources:
                        mDrawerLayout.closeDrawer(GravityCompat.START);
                        return true;
                    case 1:
                        intent = new Intent(ResourcesActivity.this, QueueActivity.class);
                        intent.putExtra("USER", mUser);
                        intent.putExtra("PASS", mPassword);
                        intent.putExtra("SERVERS", mServers);
                        intent.putExtra("CLUSTER", menu.findItem(1).getTitle());
                        finish();
                        startActivity(intent);
                        return true;
                    case 2:
                        intent = new Intent(ResourcesActivity.this, QueueActivity.class);
                        intent.putExtra("USER", mUser);
                        intent.putExtra("PASS", mPassword);
                        intent.putExtra("SERVERS", mServers);
                        intent.putExtra("CLUSTER", menu.findItem(2).getTitle());
                        finish();
                        startActivity(intent);
                        return true;
                    case 3:
                        intent = new Intent(ResourcesActivity.this, QueueActivity.class);
                        intent.putExtra("USER", mUser);
                        intent.putExtra("PASS", mPassword);
                        intent.putExtra("SERVERS", mServers);
                        intent.putExtra("CLUSTER", menu.findItem(3).getTitle());
                        finish();
                        startActivity(intent);
                        return true;
                    case R.id.nav_dcrab:
                        intent = new Intent(ResourcesActivity.this, DcrabActivity.class);
                        intent.putExtra("USER", mUser);
                        intent.putExtra("PASS", mPassword);
                        intent.putExtra("SERVERS", mServers);
                        finish();
                        startActivity(intent);
                        return true;
                    default:
                        return true;
                }
            }
        });
    }

    // Navigation menu opener listener
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

    // Back key pressed listener. If menu is open, close it. If it's not open, log out
    @Override
    public void onBackPressed(){
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)){
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }else{
            Toast.makeText(ResourcesActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
            mDataTask = null;
            finish();
        }

    }

    public class GetDataTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUser;
        private final String mPassword;
        private String consumption;
        private String max_consumption;
        private String scratch;
        private String home;

        GetDataTask(String user, String password) {
            mUser = user;
            mPassword = password;
        }

        @Override
        protected void onPreExecute() {
            // SHOW THE SPINNER WHILE LOADING FEEDS
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar2.setVisibility(View.VISIBLE);
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

                secondSession.connect(); // now we're connected to the secondary system

                // Spent credits command
                consumption = Utils.execCommand(secondSession, "cu " + mUser + " " + "ATLAS");
                // Credit limit for current user
                max_consumption = Utils.execCommand(secondSession, "grep \"^" + mUser + ":\" /usr/local/administracion/cuentas_trabajo/usuarios.dat | cut -d':' -f7");
                // Scratch space in use
                scratch = Utils.execCommand(secondSession, "du -sh /scratch/" + mUser);
                // Home directory space in use
                home = Utils.execCommand(secondSession, "du -sh");

                secondSession.disconnect();
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
            mProgressBar2.setVisibility(View.GONE);
            if (consumption.split(" ")[1].matches("\\d+(?:\\.\\d+)?")) {
                consumption = consumption.split(" ")[1];
                // Round credit consumption
                textConsumption.setText("Consumption: " + String.valueOf(Math.round(Float.parseFloat(consumption) * 100.0) / 100.0) + " / " + max_consumption);

                // Set up chart. Some of the options are of little importance
                Cartesian cartesian = AnyChart.column();

                List<DataEntry> data = new ArrayList<>();
                data.add(new ValueDataEntry("", Math.round(Float.parseFloat(consumption) * 100.0) / 100.0));

                CartesianSeriesColumn column = cartesian.column(data);

                column.getTooltip()
                        .setTitleFormat("{%X}")
                        .setPosition(Position.CENTER_BOTTOM)
                        .setAnchor(EnumsAnchor.CENTER_BOTTOM)
                        .setOffsetX(0d)
                        .setOffsetY(5d)
                        .setFormat("{%Value}{groupsSeparator: }");

                cartesian.setAnimation(true);
                cartesian.setTitle("Consumption");

                cartesian.getYScale().setMinimum(0d);
                cartesian.getYScale().setMaximum(Integer.parseInt(max_consumption));

                cartesian.getYAxis().getLabels().setFormat("{%Value}{groupsSeparator: }");

                cartesian.getTooltip().setPositionMode(TooltipPositionMode.POINT);
                cartesian.getInteractivity().setHoverMode(HoverMode.BY_X);

                cartesian.getYAxis().setTitle("Credit");

                AnyChartView columnView = findViewById(R.id.barchart);
                columnView.setChart(cartesian);
                columnView.setVisibility(View.VISIBLE);
            }else{
                // Empty chart in case user doesn't have job submitting privileges
                AnyChartView columnView = (AnyChartView) findViewById(R.id.barchart);
                columnView.setVisibility(View.VISIBLE);
                textConsumption.setText(consumption);
            }

            textScratch.setText("Used space in " + scratch.split("\\s+")[1] + " : " + scratch.split("\\s+")[0]);
            textHome.setText("Used space in home directory: " + home.split("\\s+")[0]);

            mDataTask = null;

        }

        @Override
        protected void onCancelled() {
            mProgressBar.setVisibility(View.GONE);
            mProgressBar2.setVisibility(View.GONE);
            mDataTask = null;
        }
    }
}
