package kg.delletenebre.callsassistant;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.Map;

import kg.delletenebre.callsassistant.utils.Debug;

import static android.app.Activity.RESULT_OK;

public class DebugFragment extends Fragment {
    private static final int CONTACT_PICK_RESULT = 1;

    private App mApp;
    private EditText mTextPhoneNumber;
    private ImageView mContactPhoto;
    private TextView mContactName;

    public DebugFragment() {}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mApp = App.getInstance();

        final Activity activity = getActivity();

        final Spinner spinnerEvent = activity.findViewById(R.id.event);
        final Spinner spinnerState = activity.findViewById(R.id.state);
        mTextPhoneNumber = activity.findViewById(R.id.phone_number);
        final EditText textMessageText = activity.findViewById(R.id.message_text);
        final RelativeLayout contactCard = activity.findViewById(R.id.contact_card);
        mContactPhoto = activity.findViewById(R.id.contact_photo);
        mContactName = activity.findViewById(R.id.contact_name);
        final Button btnDebugSend = activity.findViewById(R.id.button_debug_send);
        final Button btnDebugShow = activity.findViewById(R.id.button_debug_show);

        textMessageText.setEnabled(false);

        spinnerEvent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String item = adapterView.getItemAtPosition(i).toString();
                if (item.equals("call")) {
                    spinnerState.setEnabled(true);
                    textMessageText.setEnabled(false);
                } else {
                    spinnerState.setEnabled(false);
                    textMessageText.setEnabled(true);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        contactCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                        ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(contactPickerIntent, CONTACT_PICK_RESULT);
            }
        });


        btnDebugSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String event = spinnerEvent.getSelectedItem().toString();
                JSONObject data = mApp.createJsonData(
                        event, "incoming",
                        event.equals("call") ? spinnerState.getSelectedItem().toString() : "",
                        mTextPhoneNumber.getText().toString(),
                        event.equals("call") ? "" : textMessageText.getText().toString()
                );

                App.getInstance().connectAndSend(data.toString());
            }
        });

        btnDebugShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String event = spinnerEvent.getSelectedItem().toString();
                JSONObject data = mApp.createJsonData(
                        event, "incoming",
                        event.equals("call") ? spinnerState.getSelectedItem().toString() : "",
                        mTextPhoneNumber.getText().toString(),
                        event.equals("call") ? "" : textMessageText.getText().toString()
                );

                try {
                    event = data.getString("event");

                    String contactName = data.getString("name");
                    String message = data.getString("message");

                    NotyOverlay noty = NotyOverlay.create(
                            "00:00:00:00", data.getString("number"), event);
                    noty.show(data.getString("type"), data.getString("state"),
                            contactName, data.getString("photo"),
                            message, data.getString("buttons"));
                } catch (Exception e) {
                    Debug.error("btnDebugShow error");
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_debug, container, false);
    }

    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CONTACT_PICK_RESULT:
                    Uri contactData = data.getData();
                    Cursor cursor =  mApp.getContentResolver().query(contactData, null, null, null, null);
                    if (cursor != null) {
                        Map<String,String> contact = mApp.getContactInfo(cursor);

                        String base64Photo = contact.get("photo");
                        if (!base64Photo.isEmpty()) {
                            byte[] decodedString = Base64.decode(contact.get("photo"), Base64.DEFAULT);
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                            BitmapDrawable drawablePhoto = new BitmapDrawable(getResources(), decodedByte);
                            mContactPhoto.setImageDrawable(drawablePhoto);
                        } else {
                            mContactPhoto.setImageResource(R.drawable.ic_person_black);
                        }

                        mContactName.setText(contact.get("name"));
                        mTextPhoneNumber.setText(contact.get("number"));
                    }
                    break;
            }
        }
    }
}
