package kg.delletenebre.callsassistant;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.json.JSONObject;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import kg.delletenebre.callsassistant.utils.Debug;

class WebServer {

    private AsyncHttpServer mWebServer;
    private Context mContext;

    private int mLocalPort;
    private String mLocalHost;
    private String mServerAddress;


    WebServer(Context context) {
        mContext = context;
    }

    void start(int port) {
        stop();
        mLocalPort = port;
        mLocalHost = getIpAddress();

        mWebServer = new AsyncHttpServer();
        mWebServer.listen(mLocalPort);
        mWebServer.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                Debug.log(request.getQuery().getString("message"));
                String queryMessage = Uri.decode(request.getQuery().getString("message"));
                response.send(queryMessage);
                try {
                    JSONObject data = new JSONObject(queryMessage);
                    String event = data.getString("event");
                    String number = data.getString("number");

                    if (event.equals("response")) {
                        String action = data.getString("action");
                        switch (action) {
                            case "cd": // Call Dismiss
                                mContext.sendBroadcast(new Intent(App.ACTION_CALL_DISMISS));
                                break;
                            case "ca": // Call Answer
                                mContext.sendBroadcast(new Intent(App.ACTION_CALL_ANSWER));
                                break;
                            case "s1":
                            case "s2":
                            case "s3":
                                String smsButtonNumber = action.substring(1);
                                Intent smsIntent = new Intent(App.ACTION_SMS);
                                smsIntent.putExtra("phoneNumber", number);
                                smsIntent.putExtra("buttonNumber", smsButtonNumber);
                                mContext.sendBroadcast(smsIntent);
                                break;
                            case "gps":
                                Intent gpsIntent = new Intent(App.ACTION_GPS);
                                gpsIntent.putExtra("phoneNumber", number);
                                gpsIntent.putExtra("coordinates", data.getString("extra"));
                                mContext.sendBroadcast(gpsIntent);
                                break;
                        }
                    } else {
                        String contactName = data.getString("name");
                        String message = data.getString("message");
                        String type = data.getString("type");
                        String contactPhoto = data.getString("photo");
                        String state = data.getString("state");
                        String buttons = data.getString("buttons");
                        String deviceAddress = data.getString("deviceAddress");

                        Intent intent = new Intent(App.ACTION_EVENT);
                        intent.putExtra("event", event);
                        intent.putExtra("type", type);
                        intent.putExtra("number", number);
                        intent.putExtra("state", state);
                        intent.putExtra("name", contactName);
                        intent.putExtra("photo", contactPhoto);
                        intent.putExtra("message", message);
                        intent.putExtra("buttons", buttons);
                        intent.putExtra("deviceAddress", deviceAddress);

                        mContext.sendBroadcast(intent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void stop() {
        if (mWebServer != null) {
            mWebServer.stop();
            mWebServer = null;
        }
    }

    void setServerAddress(String host, int port) {
        mServerAddress = "http://" + host + ":" + port + "/";
    }

    public void send(String message) {
        AsyncHttpGet request = new AsyncHttpGet(mServerAddress + "?message=" + Uri.encode(message));
        AsyncHttpClient.getDefaultInstance().executeString(request,
                new AsyncHttpClient.StringCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
                Debug.log(result);
            }
        });
    }

    String getDeviceAddress() {
        return "http://" + mLocalHost + ":" + mLocalPort;
    }

    private static String getIpAddress() {
        String type = "wlan"; // wlan, eth, sit, lo
        try {
            for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface anInterface = (NetworkInterface) en.nextElement();

                if (anInterface.getName().startsWith(type)) {
                    for (Enumeration enumIpAddress = anInterface.getInetAddresses();
                         enumIpAddress.hasMoreElements();) {
                        InetAddress inetAddress = (InetAddress) enumIpAddress.nextElement();
                        if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Debug.error("Socket exception in GetIP Address of Utilities");
            ex.printStackTrace();
        }

        return "0.0.0.0";
    }
}

