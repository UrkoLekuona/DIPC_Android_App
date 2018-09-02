package es.ehu.dipc.dipc;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class QueueActivity extends AppCompatActivity {

    private DrawerLayout mDrawerLayout;
    private LinearLayout mProgressBar;
    private GetDataTask mDataTask;
    private TextView userView;
    private NavigationView nv;
    private Button mRefresh;
    private String mUser;
    private String mPassword;
    private String mServers;
    private String mCluster;
    private ListView listView;
    private ExpandableListView expandableListView;
    private ExpandableListViewAdapter expandableListViewAdapter;
    private List<String> listDataGroup;
    private HashMap<String, List<String>> listDataChild;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);
        Utils.darkenStatusBar(this, R.color.colorPrimaryDark);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        mProgressBar = findViewById(R.id.linlaHeaderProgress);
        // expandableListView will be the list containing the queue information
        expandableListView = findViewById(R.id.expandableList);

        Intent intent = getIntent();
        mUser = intent.getStringExtra("USER");
        mPassword = intent.getStringExtra("PASS");
        mServers = intent.getStringExtra("SERVERS");
        mCluster = intent.getStringExtra("CLUSTER");

        //listView = (ListView) findViewById(R.id.list);

        String[] values = mServers.split(" ");

        // Refresh button action listener
        mRefresh = findViewById(R.id.toolbar_btn);
        mRefresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mDataTask = new GetDataTask(mUser, mPassword);
                mDataTask.execute((Void) null);
            }
        });

        mDataTask = new GetDataTask(mUser, mPassword);
        mDataTask.execute((Void) null);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
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
                        intent = new Intent(QueueActivity.this, ResourcesActivity.class);
                        intent.putExtra("USER", mUser);
                        intent.putExtra("PASS", mPassword);
                        intent.putExtra("SERVERS", mServers);
                        finish();
                        startActivity(intent);
                        return true;
                    case 1:
                        if(!mCluster.equals(menu.findItem(id).getTitle().toString())) {
                            intent = new Intent(QueueActivity.this, QueueActivity.class);
                            intent.putExtra("USER", mUser);
                            intent.putExtra("PASS", mPassword);
                            intent.putExtra("SERVERS", mServers);
                            intent.putExtra("CLUSTER", menu.findItem(id).getTitle());
                            finish();
                            startActivity(intent);
                            return true;
                        }else{
                            mDrawerLayout.closeDrawer(GravityCompat.START);
                            return true;
                        }
                    case 2:
                        if(!mCluster.equals(menu.findItem(id).getTitle().toString())) {
                            intent = new Intent(QueueActivity.this, QueueActivity.class);
                            intent.putExtra("USER", mUser);
                            intent.putExtra("PASS", mPassword);
                            intent.putExtra("SERVERS", mServers);
                            intent.putExtra("CLUSTER", menu.findItem(id).getTitle());
                            finish();
                            startActivity(intent);
                            return true;
                        }else{
                            mDrawerLayout.closeDrawer(GravityCompat.START);
                            return true;
                        }
                    case 3:
                        if(!mCluster.equals(menu.findItem(id).getTitle().toString())) {
                            intent = new Intent(QueueActivity.this, QueueActivity.class);
                            intent.putExtra("USER", mUser);
                            intent.putExtra("PASS", mPassword);
                            intent.putExtra("SERVERS", mServers);
                            intent.putExtra("CLUSTER", menu.findItem(id).getTitle());
                            finish();
                            startActivity(intent);
                            return true;
                        }else{
                            mDrawerLayout.closeDrawer(GravityCompat.START);
                            return true;
                        }
                    case R.id.nav_dcrab:
                        intent = new Intent(QueueActivity.this, DcrabActivity.class);
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
    public class GetDataTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUser;
        private final String mPassword;
        private List<String> queue;

        GetDataTask(String user, String password) {
            mUser = user;
            mPassword = password;
        }

        @Override
        protected void onPreExecute() {
            // SHOW THE SPINNER WHILE LOADING FEEDS
            mProgressBar.setVisibility(View.VISIBLE);
            expandableListView.setVisibility(View.GONE);
            mRefresh.setEnabled(false);
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
                session.setPortForwardingL(2233, mCluster.toLowerCase() + ".sw.ehu.es", 22);


                session.setTimeout(10000);
                session.connect();

                Session secondSession = jsch.getSession(mUser, "localhost", 2233);
                secondSession.setPassword(mPassword);
                secondSession.setConfig(config);
                secondSession.setTimeout(10000);

                try {
                    secondSession.connect(); // now we're connected to the secondary system

                    queue = Utils.execQueueCommand(secondSession, mCluster);

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
            expandableListView.setVisibility(View.VISIBLE);

            /*ArrayAdapter<String> adapter = new ArrayAdapter<String>(QueueActivity.this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, queue);

            listView.setAdapter(adapter);*/

            // Expandable List View set up. Important to check list_row_child.xml and list_row_group.xml layouts and ExpandableListViewAdapter class
            // Initialize groups and group-child container
            listDataGroup = new ArrayList<>();
            listDataChild = new HashMap<>();

            // Set adapter to expandableListView
            expandableListViewAdapter = new ExpandableListViewAdapter(QueueActivity.this, listDataGroup, listDataChild);
                expandableListView.setAdapter(expandableListViewAdapter);

            // Fill groups
            listDataGroup.add("Active Jobs");
            listDataGroup.add("Idle Jobs");
            listDataGroup.add("Blocked Jobs");

            if (queue != null) {
                // Remove all empty list indexes
                queue.removeAll(Arrays.asList(null, ""));

                // Get index of each section
                int active_index = queue.indexOf("ACTIVE JOBS--------------------");
                int idle_index = queue.indexOf("IDLE JOBS----------------------");
                int blocked_index = queue.indexOf("BLOCKED JOBS----------------");

                // Split queue in 3 parts
                List<String> active_list = queue.subList(active_index, idle_index);
                List<String> idle_list = queue.subList(idle_index, blocked_index);
                List<String> blocked_list = queue.subList(blocked_index, queue.size());

                int as = active_list.size();
                int is = idle_list.size();
                int bs = blocked_list.size();

                //boolean a = Collections.replaceAll(queue, "  ", " ");
                // Loop as many times as the biggest queue and replaces multiple whitespaces with ' | ' to make fields distinguishable
                for (int i = 0; i < Math.max(Math.max(as, is), bs); i++) {
                    if (as > i && !active_list.get(i).contains("Active")) {
                        active_list.set(i, active_list.get(i).replaceAll("\\s\\s+", " | "));
                    }
                    if (is > i) {
                        idle_list.set(i, idle_list.get(i).replaceAll("\\s\\s+", " | "));
                    }
                    if (bs > i) {
                        blocked_list.set(i, blocked_list.get(i).replaceAll("\\s\\s+", " | "));
                    }
                }

                // Fill group-child container with data
                listDataChild.put(listDataGroup.get(0), active_list);
                listDataChild.put(listDataGroup.get(1), idle_list);
                listDataChild.put(listDataGroup.get(2), blocked_list);
            }else{
                // Notify user that data couldn't be read
                final AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(QueueActivity.this, android.R.style.Theme_Material_Dialog_Alert);
                } else {
                    builder = new AlertDialog.Builder(QueueActivity.this);
                }
                builder.setTitle("Error")
                        .setMessage("Unable to fetch data. Cluster may be offline.")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }

            // Refresh adapter data
            expandableListViewAdapter.notifyDataSetChanged();

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
            Toast.makeText(QueueActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
            //mDataTask = null;
            finish();
        }

    }
}
