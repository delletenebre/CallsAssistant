package kg.delletenebre.callsassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private IntentFilter mBroadcastReceiverFilter;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewPager viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);


        mBroadcastReceiverFilter = new IntentFilter();
        mBroadcastReceiverFilter.addAction(App.LOCAL_ACTION_SERVER_START);
        mBroadcastReceiverFilter.addAction(App.LOCAL_ACTION_SERVER_STOP);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case App.LOCAL_ACTION_SERVER_START:
                        case App.LOCAL_ACTION_SERVER_STOP:
                            updateIp();
                            break;
                    }
                }
            }
        };

        App.getInstance().startWebServer();
    }

    @Override
    protected void onStart() {
        super.onStart();

        App.getInstance().checkBluetoothEnabled(findViewById(R.id.root_layout));
        registerReceiver(mBroadcastReceiver, mBroadcastReceiverFilter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mBroadcastReceiver);
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new DebugFragment(), getString(R.string.tab_debug));
        adapter.addFragment(new PermissionsFragment(), getString(R.string.tab_permissions));
        viewPager.setAdapter(adapter);
    }

    public void updateIp() {
        TextView tvIpAddress = findViewById(R.id.ip_address);
        App app = App.getInstance();
        if (tvIpAddress != null) {
            WebServer webServer = app.getWebServer();
            if (app.getPrefs().getString("connection_type").equals(App.CONNECTION_TYPE_WIFI)
                    && webServer != null
                    && !webServer.isStopped()) {
                tvIpAddress.setVisibility(View.VISIBLE);
                tvIpAddress.setText(String.format(
                        getString(R.string.info_ip_address),
                        webServer.getHost(),
                        webServer.getPort()));
            } else {
                tvIpAddress.setVisibility(View.GONE);
            }
        }
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.report_crash:
                try {
                    throw new RuntimeException();
                } catch (Exception e) {
                    Crashlytics.setString("UserReport", "Failed sending reply to debugger: Broken pipe");
                    Crashlytics.logException(e);
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            PermissionsFragment pf = getCurrentPermissionsFragment();
            if (pf != null) {
                switch (requestCode) {
                    case PermissionsFragment.PERMISSIONS_REQUEST_READ_CONTACTS:
                        pf.setGrantedPermissionUi(this,
                                R.id.status_read_contacts, R.id.request_read_contacts);
                        break;
                    case PermissionsFragment.PERMISSIONS_REQUEST_CALL_PHONE:
                        pf.setGrantedPermissionUi(this,
                                R.id.status_call_phone, R.id.request_call_phone);
                        break;
                    case PermissionsFragment.PERMISSIONS_REQUEST_SEND_SMS:
                        pf.setGrantedPermissionUi(this,
                                R.id.status_send_sms, R.id.request_send_sms);
                        break;
                    case PermissionsFragment.PERMISSIONS_REQUEST_LOCATION:
                        pf.setGrantedPermissionUi(this,
                                R.id.status_gps, R.id.request_gps);
                        break;
                }
            }
        }
    }

    public PermissionsFragment getCurrentPermissionsFragment() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment instanceof PermissionsFragment) {
                    return (PermissionsFragment) fragment;
                }
            }
        }

        return null;
    }
}
