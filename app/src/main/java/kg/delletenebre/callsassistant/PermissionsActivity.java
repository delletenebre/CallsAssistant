package kg.delletenebre.callsassistant;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

public class PermissionsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String permission = extras.getString("permission");
            if (permission != null && !permission.isEmpty()) {
                checkPermissionFor(permission);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        finish();
    }


    private void checkPermissionFor(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
        }
    }

    public static boolean testPermission(String permission) {
        Context context = App.getInstance().getApplicationContext();

        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(context, PermissionsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

            intent.putExtra("permission", permission);

            context.startActivity(intent);

            return false;
        } else {
            return true;
        }
    }
}
