package kg.delletenebre.callsassistant;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class EventsReceiver extends BroadcastReceiver {

    private static boolean sCallIsIncoming = false;
    private static String sCallLastState = TelephonyManager.EXTRA_STATE_IDLE;
    private static String sCallLastPhoneNumber = "";


    @Override
    public void onReceive(Context context, Intent intent) {
        final App fApp = App.getInstance();

        String action = intent.getAction();

        String infoEvent;
        String infoType = "";
        String infoState = "";
        String infoPhoneNumber = "";
        String infoMessage = "";

        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:

                break;
            case "android.intent.action.PHONE_STATE":
                Debug.info("**** ACTION_PHONE_STATE ****");

                infoEvent = "call";
                String callState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                if (sCallLastState.equals(callState)) {
                    return;
                } else if (callState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    // Трубка не поднята, телефон звонит
                    sCallIsIncoming = true;

                    infoType = "incoming";
                    infoState = "ringing";
                    infoPhoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    if (infoPhoneNumber == null || infoPhoneNumber.length() < 3) {
                        infoPhoneNumber = "";
                    }
                    sCallLastPhoneNumber = infoPhoneNumber;
                } else if (callState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    // Телефон находится в режиме звонка (набор номера при исходящем звонке / разговор)
                    infoState = "offhook";
                    infoType = "incoming";
                    infoPhoneNumber = sCallLastPhoneNumber;
                    if (!sCallLastState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        // исходящий
                        infoType = "outgoing";
                        sCallIsIncoming = false;
                    }
                } else if (callState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    // Телефон находится в ждущем режиме - это событие наступает по окончанию разговора
                    // или в ситуации "отказался поднимать трубку".
                    infoState = "idle";
                    infoType = "incoming";
                    infoPhoneNumber = sCallLastPhoneNumber;
                    sCallLastPhoneNumber = "";
                    if (sCallLastState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        // Ring but no pickup - a miss
                        infoState = "missed";
                    } else if (!sCallIsIncoming) {
                        infoType = "outgoing";
                    }
                }
                sCallLastState = callState;

                if (fApp.getPrefs().getBoolean("noty_show_calls", true)) {
                    JSONObject callData = fApp.createJsonData(infoEvent, infoType, infoState,
                            infoPhoneNumber, infoMessage);
                    fApp.connectAndSend(callData.toString());
                    Debug.info("JSON data (call): " + callData.toString());
                }

                break;
            case "android.provider.Telephony.SMS_RECEIVED":
                Debug.info("**** ACTION_SMS_RECEIVED ****");

                infoEvent = "sms";
                infoType = "incoming";
                Map<String, String> contact = new HashMap<>();
                if (Build.VERSION.SDK_INT >= 19) {
                    for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                        if (smsMessage == null) {
                            Debug.error("SMS is null");
                            break;
                        }
                        infoPhoneNumber = smsMessage.getDisplayOriginatingAddress();
                        infoMessage += smsMessage.getDisplayMessageBody();
                    }
                } else {
                    Bundle extras = intent.getExtras();
                    Object[] smsData = (Object[]) extras.get("pdus");
                    if (smsData != null) {
                        for (Object pdu : smsData) {
                            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                            if (smsMessage == null) {
                                Debug.error("SMS is null");
                                break;
                            }
                            infoPhoneNumber = smsMessage.getDisplayOriginatingAddress();
                            infoMessage += smsMessage.getDisplayMessageBody();
                        }
                    }
                }

                if (fApp.getPrefs().getBoolean("noty_show_sms", true)) {
                    JSONObject smsData = fApp.createJsonData(infoEvent, infoType, infoState,
                            infoPhoneNumber, infoMessage);
                    fApp.connectAndSend(smsData.toString());
                    Debug.info("JSON SMS: " + smsData.toString());
                }
                break;
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                Debug.log("**** BluetoothAdapter.ACTION_STATE_CHANGED ****");

                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        App.getInstance().startBluetoothCommunication();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        App.getInstance().stopBluetoothCommunication();
                        break;
                }
                break;
            case App.ACTION_CALL_DISMISS:
                Debug.log("**** ACTION_CALL_DISMISS ****");
                fApp.endCall();
                break;
            case App.ACTION_CALL_ANSWER:
                Debug.log("**** ACTION_CALL_ANSWER ****");
                Debug.log("No \"legal\" way to call answer programmatically");
                break;
            case App.ACTION_SMS:
                Debug.log("**** ACTION_SMS ****");
                fApp.endCall();
                String smsMessage = fApp.getPrefs().getString("message_sms_" + intent.getStringExtra("buttonNumber"),
                        context.getString(R.string.pref_default_message));
                fApp.sendSMS(intent.getStringExtra("phoneNumber"), smsMessage);
                break;
            case App.ACTION_GPS:
                Debug.log("**** ACTION_GPS ****");
                fApp.endCall();
                String gpsMessage = fApp.getLocationSMS(fApp.getPrefs().getString("message_gps",
                        fApp.getString(R.string.pref_default_message_gps)), intent.getStringExtra("coordinates"));
                Debug.log(intent.getStringExtra("phoneNumber"));
                Debug.log(gpsMessage);
                fApp.sendSMS(intent.getStringExtra("phoneNumber"), gpsMessage);
                break;
            case App.ACTION_EVENT:
                Debug.log("**** ACTION_EVENT ****");
                String event = intent.getStringExtra("event");
                String eventState = intent.getStringExtra("state");

                if ((event.equals("sms") && fApp.getPrefs().getBoolean("noty_show_sms", true))
                        || (event.equals("call") && fApp.getPrefs().getBoolean("noty_show_calls", true))) {
                    NotyOverlay noty = NotyOverlay.create(intent.getStringExtra("deviceAddress"),
                            intent.getStringExtra("number"),
                            event);
                    if (eventState.equals("idle") || eventState.equals("missed")) {
                        noty.close();
                    } else {
                        noty.show(intent.getStringExtra("type"),
                                eventState,
                                intent.getStringExtra("name"),
                                intent.getStringExtra("photo"),
                                intent.getStringExtra("message"),
                                intent.getStringExtra("buttons"));
                    }
                }
                break;
        }
    }
}
