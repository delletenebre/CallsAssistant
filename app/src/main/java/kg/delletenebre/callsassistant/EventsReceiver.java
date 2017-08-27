package kg.delletenebre.callsassistant;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
        final App app = App.getInstance();

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

                if (infoType.equals("incoming")) {
                    final JSONObject info = app.createJsonData(infoEvent, infoType, infoState,
                            infoPhoneNumber, infoMessage);
                    app.connectAndSend(info.toString());
                    Debug.info("JSON data (call): " + info.toString());
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
                JSONObject info = app.createJsonData(infoEvent, infoType, infoState,
                        infoPhoneNumber, infoMessage);
                app.connectAndSend(info.toString());

                Debug.info("JSON SMS: " + info.toString());

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
        }
    }
}
