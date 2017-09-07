package kg.delletenebre.callsassistant;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayout;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import kg.delletenebre.callsassistant.utils.Debug;

class NotyOverlay {

    private static Map<String, NotyOverlay> sOverlays = new HashMap<>();

    static NotyOverlay create(String deviceAddress, String number, String event) {
        String id = String.format(App.getInstance().getString(R.string.notification_id),
                deviceAddress, number, event);
        if (sOverlays.containsKey(id)) {
            sOverlays.get(id).close();
        }
        sOverlays.put(id, new NotyOverlay(deviceAddress, number, event));

        Debug.log("Overlay with key [ " + id + " ] created");

        return sOverlays.get(id);
    }

    private WindowManager mWindowManager;
    private View mNotificationLayout;
    private App mApp;
    private Context mContext;
    private String mDeviceAddress;
    private String mCallNumber;
    private String mEvent;
    private String mId;

    private NotyOverlay(String deviceAddress, String number, String event) {
        mApp = App.getInstance();
        mContext = mApp.getApplicationContext();
        mDeviceAddress = deviceAddress;
        mCallNumber = number;
        mEvent = event;
        mId = String.format(App.getInstance().getString(R.string.notification_id),
                deviceAddress, number, event);
    }

    void show(String type, String state, String name, String photoBase64, String message,
              String buttons) {

        if (Build.VERSION.SDK_INT < 23
                || (Build.VERSION.SDK_INT >= 23 && Settings.canDrawOverlays(mContext))) {

            mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

            DisplayMetrics displayMetrics = new DisplayMetrics();
            mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);

            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,

                    WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,

                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,

                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.TOP;

            LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            mNotificationLayout = layoutInflater.inflate(R.layout.noty_overlay, null);

            String headerTitle = "";
            String contactExtraText = "";
            int headerIcon = R.drawable.ic_phone_black;
            switch (mEvent) {
                case "call":
                    headerTitle = mContext.getString(R.string.incoming_call);
                    contactExtraText = name.equals(mCallNumber) ? "" : mCallNumber;
                    break;

                case "sms":
                    headerTitle = mContext.getString(R.string.incoming_sms);
                    contactExtraText = message;
                    headerIcon = R.drawable.ic_mail_outline_black;
                    break;

                case "message":
                    headerTitle = mContext.getString(R.string.incoming_message);
                    contactExtraText = message;
                    switch (type) {
                        case "whatsapp":
                            headerIcon = R.drawable.ic_whatsapp_black;
                            break;

                        default:
                            headerIcon = R.drawable.ic_mail_outline_black;
                    }
                    break;
            }

            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;

            int normalSize = getMagicPointSize();

            int smallSize = (int) (normalSize * 0.85f + 0.5f);
            int headerIconSize = calcDisplayPoints(displayMetrics, smallSize);
            int photoSize = calcDisplayPoints(displayMetrics, (int)(normalSize * 3.2f + 0.5f));
            int nameSize = (int) (normalSize * 1.15f + 0.5f);
            int buttonHeight = calcDisplayPoints(displayMetrics, normalSize * 3);

            View vwStatusBarHelper = mNotificationLayout.findViewById(R.id.status_bar_helper);
            RelativeLayout ltRoot = mNotificationLayout.findViewById(R.id.root_layout);
            //LinearLayout ltNotification = (LinearLayout) mNotificationLayout.findViewById(R.id.notification_layout);
            CardView ltNotification = mNotificationLayout.findViewById(R.id.notification_layout);
            RelativeLayout ltHeader = mNotificationLayout.findViewById(R.id.header_layout);
            TextView vwHeaderTitle = mNotificationLayout.findViewById(R.id.header_title);
            ImageView vwHeaderIcon = mNotificationLayout.findViewById(R.id.header_icon);
            LinearLayout ltContact = mNotificationLayout.findViewById(R.id.contact_layout);
            ImageView vwContactPhoto = mNotificationLayout.findViewById(R.id.contact_photo);
            TextView vwContactName = mNotificationLayout.findViewById(R.id.contact_name);
            TextView vwContactExtra = mNotificationLayout.findViewById(R.id.contact_extra);

            LinearLayout ltCallResponse = mNotificationLayout.findViewById(R.id.call_response_layout);
            AppCompatButton btnCallDismiss = mNotificationLayout.findViewById(R.id.call_dismiss);
            AppCompatButton btnCallAnswer = mNotificationLayout.findViewById(R.id.call_answer);

            final GridLayout ltResponseButtons = mNotificationLayout.findViewById(R.id.response_buttons_layout);

            vwStatusBarHelper.getLayoutParams().height = getStatusBarHeight(mContext.getResources());

            int notyWidth = Integer.parseInt(mApp.getPrefs().getString("noty_width", mContext.getString(R.string.pref_default_noty_width)));
            ltNotification.getLayoutParams().width = (int) (screenWidth / 100.0f * notyWidth);
            ViewCompat.setElevation(ltNotification, 10);

            /* **** HEADER **** */
            vwHeaderTitle.setTextSize(smallSize);
            vwHeaderTitle.setText(headerTitle);

            vwHeaderIcon.getLayoutParams().width = headerIconSize;
            vwHeaderIcon.getLayoutParams().height = headerIconSize;
            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                vwHeaderIcon.setImageDrawable(
                        VectorDrawableCompat.create(
                                mContext.getResources(), headerIcon, mContext.getTheme()));
            } else {
                vwHeaderIcon.setImageResource(headerIcon);
            }
            vwHeaderIcon.requestLayout();
            /* **** ------ **** */


            /* **** CONTACT **** */
            Drawable contactPhotoDrawable = getContactPhotoDrawable(mContext, photoBase64);
            if (contactPhotoDrawable != null) {
                vwContactPhoto.setImageDrawable(contactPhotoDrawable);
            } else {
                vwContactPhoto.setImageBitmap(getDefaultContactPhotoBitmap(mContext, photoSize));
            }

            vwContactPhoto.getLayoutParams().width = photoSize;
            vwContactPhoto.getLayoutParams().height = photoSize;
            vwContactPhoto.requestLayout();

            vwContactName.setTextSize(nameSize);
            vwContactName.setText(name);
            vwContactExtra.setTextSize(normalSize);
            vwContactExtra.setText(contactExtraText);

            ltContact.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    close();
                }
            });
            /* **** ------- **** */


            /* **** CALL BUTTONS **** */
            if (mEvent.equals("call")) {
                setButtonStyle(btnCallDismiss, normalSize, buttonHeight, R.drawable.ic_close_black);
                setButtonStyle(btnCallAnswer, normalSize, buttonHeight, R.drawable.ic_phone_black);
                if (state.equals("offhook")) {
                    btnCallAnswer.setVisibility(View.GONE);
                }
            } else {
                ltCallResponse.setVisibility(View.GONE);
            }

            btnCallDismiss.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mApp.connectAndSend(mDeviceAddress, mApp.createResponseData("cd"));
                    close();
                }
            });

            btnCallAnswer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mApp.connectAndSend(mDeviceAddress, mApp.createResponseData("ca"));
                    close();
                }
            });
            /* **** ---- ------- **** */


            /* **** OTHER BUTTONS **** */
            String[] enabledButtons = buttons.split(",");
            for (final String buttonName: enabledButtons) {
                AppCompatButton responseButton = new AppCompatButton(mContext);

                if (buttonName.charAt(0) == 's') {
                    int number = (int) buttonName.charAt(1) - '0';
                    responseButton.setText(String.format(mContext.getString(R.string.button_text_sms), number));
                } else if (buttonName.equals("gps")) {
                    responseButton.setText(mContext.getString(R.string.button_text_gps));
                }

                responseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String extra = "";
                        if (buttonName.equals("gps")) {
                            extra = mApp.getLocationString();
                        }
                        mApp.connectAndSend(mDeviceAddress,
                                mApp.createResponseData(buttonName, mCallNumber, "", extra));
                        close();
                    }
                });

                ltResponseButtons.addView(responseButton);

                int icon = R.drawable.ic_mail_outline_black;
                if (buttonName.equals("gps")) {
                    icon = R.drawable.ic_my_location_black_24dp;
                }
                setButtonStyle(responseButton, normalSize, buttonHeight, icon);
            }

            /* **** ----- ------- **** */

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mWindowManager.addView(mNotificationLayout, params);
                }
            });

            mNotificationLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int width = ltResponseButtons.getMeasuredWidth();
                    if (width > 0) {
                        mNotificationLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                        fillView(ltResponseButtons);
                    }
                }
            });


        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + mContext.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            mContext.startActivity(intent);
        }
    }

    void close() {
        if (mNotificationLayout != null){
            mWindowManager.removeView(mNotificationLayout);
            mNotificationLayout = null;
        }

        if (sOverlays.containsKey(mId)) {
            sOverlays.remove(mId);
        }
    }

    private int getStatusBarHeight(Resources resources) {
        int result = 0;
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId);
        }
        return result;
    }



    private Drawable getContactPhotoDrawable(Context context, String photoBase64) {
        Drawable contactPhotoDrawable = null;
        if (photoBase64 != null && !photoBase64.isEmpty()) {
            byte[] decodedString = Base64.decode(photoBase64, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

            contactPhotoDrawable = new BitmapDrawable(mContext.getResources(), decodedByte);
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            contactPhotoDrawable = context.getResources()
                    .getDrawable(R.drawable.ic_person_black, mContext.getTheme());
        }

        return contactPhotoDrawable;
    }

    private Bitmap getDefaultContactPhotoBitmap(Context context, int photoSize) {
        int defaultPhotoId = R.drawable.ic_person_black;

        Drawable contactPhotoDrawable =
                AppCompatDrawableManager.get().getDrawable(context, defaultPhotoId);
        Bitmap b = Bitmap.createBitmap(photoSize, photoSize, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        contactPhotoDrawable.setBounds(0, 0, c.getWidth(), c.getHeight());
        contactPhotoDrawable.draw(c);

        return b;
    }

    private int getMagicPointSize() {
        App app = App.getInstance();
        String defaultSize = app.getApplicationContext()
                .getString(R.string.pref_default_normal_size);
        return Integer.parseInt(app.getPrefs().getString("normal_size", defaultSize));
    }

    private int calcDisplayPoints(DisplayMetrics displayMetrics, int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, displayMetrics);
    }

    private void setButtonStyle(AppCompatButton btn, int textSize, int height, int icon) {
        btn.setTextSize(textSize);
        btn.getLayoutParams().height = height;
        btn.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
    }

    private void fillView(GridLayout gl) {
        //Stretch buttons
        int idealChildWidth = ((gl.getMeasuredWidth())/gl.getColumnCount());

        for (int i = 0; i < gl.getChildCount(); i++) {
            Button button = (Button) gl.getChildAt(i);
            button.setWidth(idealChildWidth);
        }
    }
}
