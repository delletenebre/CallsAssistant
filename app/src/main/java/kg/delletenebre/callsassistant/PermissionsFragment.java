package kg.delletenebre.callsassistant;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class PermissionsFragment extends Fragment {

    protected final static int PERMISSIONS_REQUEST_CODE = 0;
    protected final static int PERMISSIONS_REQUEST_READ_CONTACTS = 1;
    protected final static int PERMISSIONS_REQUEST_CALL_PHONE = 2;
    protected final static int PERMISSIONS_REQUEST_SEND_SMS = 3;
    protected final static int PERMISSIONS_REQUEST_LOCATION = 4;

    public PermissionsFragment() {}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();

        setPermissionUi(activity, "draw_overlays",
                R.id.status_draw_overlays, R.id.request_draw_overlays);

        setPermissionUi(activity, Manifest.permission.READ_CONTACTS,
                R.id.status_read_contacts, R.id.request_read_contacts);

        setPermissionUi(activity, Manifest.permission.CALL_PHONE,
                R.id.status_call_phone, R.id.request_call_phone);

        setPermissionUi(activity, Manifest.permission.SEND_SMS,
                R.id.status_send_sms, R.id.request_send_sms);

        setPermissionUi(activity, Manifest.permission.ACCESS_FINE_LOCATION,
                R.id.status_gps, R.id.request_gps);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_permissions, container, false);
    }

    private void requestPermission(Activity activity, String permission) {
        if (!checkPermission(activity, permission)) {
            if (permission.equals("draw_overlays")) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + activity.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                activity.startActivity(intent);
            } else {
                int requestCode = PERMISSIONS_REQUEST_CODE;
                switch (permission) {
                    case Manifest.permission.READ_CONTACTS:
                        requestCode = PERMISSIONS_REQUEST_READ_CONTACTS;
                        break;

                    case Manifest.permission.CALL_PHONE:
                        requestCode = PERMISSIONS_REQUEST_CALL_PHONE;
                        break;

                    case Manifest.permission.SEND_SMS:
                        requestCode = PERMISSIONS_REQUEST_SEND_SMS;
                        break;

                    case Manifest.permission.ACCESS_FINE_LOCATION:
                        requestCode = PERMISSIONS_REQUEST_LOCATION;
                        break;
                }

                String[] permissions = new String[]{permission};
                if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    permissions = new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION};
                }
                ActivityCompat.requestPermissions(activity, permissions, requestCode);
            }
        }
    }

    private boolean checkPermission(Activity activity, String permission) {
        if (permission.equals("draw_overlays")) {
            return Build.VERSION.SDK_INT < 23
                    || (Build.VERSION.SDK_INT >= 23 && Settings.canDrawOverlays(activity));
        } else {
            return ContextCompat.checkSelfPermission(activity, permission)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }


    public void setPermissionUi(final Activity activity, final String permission,
                                 int statusId, int requestId) {
        if (checkPermission(activity, permission)) {
            setGrantedPermissionUi(activity, statusId, requestId);
        } else {
            AppCompatButton btnRequest = (AppCompatButton) activity.findViewById(requestId);
            btnRequest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    requestPermission(activity, permission);
                }
            });
        }
    }

    public void setGrantedPermissionUi(Activity activity, int statusId, int requestId) {
        TextView txtStatus = (TextView) activity.findViewById(statusId);
        AppCompatButton btnRequest = (AppCompatButton) activity.findViewById(requestId);

        txtStatus.setTextColor(Color.parseColor("#4CAF50"));
        txtStatus.setText(R.string.perm_allow);

        btnRequest.setOnClickListener(null);
        btnRequest.setVisibility(View.GONE);
    }
}
