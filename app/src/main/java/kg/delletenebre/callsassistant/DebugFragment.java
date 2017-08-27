package kg.delletenebre.callsassistant;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
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
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Map;

public class DebugFragment extends Fragment {

    public DebugFragment() {}

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();

        final Spinner spinnerEvent = (Spinner) activity.findViewById(R.id.event);
        final Spinner spinnerState = (Spinner) activity.findViewById(R.id.state);
        final EditText textPhoneNumber = (EditText) activity.findViewById(R.id.phone_number);
        final EditText textMessageText = (EditText) activity.findViewById(R.id.message_text);
        final RelativeLayout contactCard = (RelativeLayout) activity.findViewById(R.id.contact_card);
        final ImageView contactPhoto = (ImageView) activity.findViewById(R.id.contact_photo);
        final TextView contactName = (TextView) activity.findViewById(R.id.contact_name);
        final Button btnDebugSend = (Button) activity.findViewById(R.id.button_debug_send);
        final Button btnDebugShow = (Button) activity.findViewById(R.id.button_debug_show);

        final App app = App.getInstance();


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
                String phoneNumber = textPhoneNumber.getText().toString();
                if (!phoneNumber.isEmpty()) {
                    Map<String,String> contact = app.getContactInfo(phoneNumber);

                    String base64Photo = contact.get("photo");
                    if (!base64Photo.isEmpty()) {
                        byte[] decodedString = Base64.decode(contact.get("photo"), Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                        BitmapDrawable drawablePhoto = new BitmapDrawable(getResources(), decodedByte);
                        contactPhoto.setImageDrawable(drawablePhoto);
                    } else {
                        contactPhoto.setImageResource(R.drawable.ic_person_black);
                    }

                    contactName.setText(contact.get("name"));

                } else {
                    Toast.makeText(activity.getApplicationContext(),
                            getString(R.string.warning_enter_phone_number),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });


        btnDebugSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String event = spinnerEvent.getSelectedItem().toString();
                JSONObject data = app.createJsonData(
                        event, "incoming",
                        event.equals("call") ? spinnerState.getSelectedItem().toString() : "",
                        textPhoneNumber.getText().toString(),
                        event.equals("call") ? "" : textMessageText.getText().toString()
                );

                App.getInstance().connectAndSend(data.toString());
            }
        });

        btnDebugShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String event = spinnerEvent.getSelectedItem().toString();
                JSONObject data = app.createJsonData(
                        event, "incoming",
                        event.equals("call") ? spinnerState.getSelectedItem().toString() : "",
                        textPhoneNumber.getText().toString(),
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
}
