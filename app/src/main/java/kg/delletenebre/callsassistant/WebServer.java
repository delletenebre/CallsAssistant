package kg.delletenebre.callsassistant;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;

import kg.delletenebre.callsassistant.utils.Debug;

class WebServer {

    private AsyncHttpServer mWebServer;
    private Context mContext;

    private int mLocalPort;
    private String mLocalHost;
    private String mServerAddress;
    private WifiManager mWifiManager;


    WebServer(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    void start(int port) {
        stop();
        String[] wifiInfo = getWifiInfo();
        String currentSsid = wifiInfo[1].replace("\"", "");
        Debug.log("IP: " + wifiInfo[0]);
        Debug.log("SSID: " + currentSsid);
        if (!currentSsid.isEmpty()) {
            App app = App.getInstance();
            if (app.getPrefs().getBoolean("special_ssid_checkbox")) {
                String specialSsids = app.getPrefs().getString("special_ssid");
                specialSsids = specialSsids.replace(", ", ",");
                String[] specialSsidsArray = {};
                if (!specialSsids.isEmpty()) {
                    specialSsidsArray = specialSsids.split(",");
                }

                if (specialSsidsArray.length > 0) {
                    if (!ArrayUtils.contains(specialSsidsArray, currentSsid)) {
                        Debug.info("STOP");
                        return;
                    }
                }
            }

            mLocalPort = port;
            mLocalHost = getIpAddress();

            mWebServer = new AsyncHttpServer();
            mWebServer.listen(mLocalPort);

            Intent ipIntent = new Intent(App.LOCAL_ACTION_SERVER_START);
            ipIntent.putExtra("ip", getIpAddress());
            ipIntent.putExtra("port", mLocalPort);
            mContext.sendBroadcast(ipIntent);

            mWebServer.get("/", new HttpServerRequestCallback() {
                @Override
                public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                    AssetManager assetManager = mContext.getAssets();

                    String html = "<h1>200 OK</h1>";
                    InputStream input;
                    try {
                        input = assetManager.open("index.html");
                        int size = input.available();
                        byte[] buffer = new byte[size];
                        //noinspection ResultOfMethodCallIgnored
                        input.read(buffer);
                        input.close();

                        html = new String(buffer);
                        html = html.replace("{{title}}", mContext.getString(R.string.app_name));
                        html = html.replace("{{version_name}}", BuildConfig.VERSION_NAME);
                        html = html.replace("{{version_code}}", String.valueOf(BuildConfig.VERSION_CODE));
                    } catch (IOException ioe) {
                        Debug.error(ioe.getLocalizedMessage());
                    }
                    response.send(html);
                }
            });

            mWebServer.get("/api/", new HttpServerRequestCallback() {
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
                            intent.putExtra("disabled", data.getString("disabled"));

                            mContext.sendBroadcast(intent);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    void stop() {
        if (mWebServer != null) {
            mWebServer.stop();
            mWebServer = null;
            mContext.sendBroadcast(new Intent(App.LOCAL_ACTION_SERVER_STOP));
        }
    }

    public boolean isStopped() {
        return mWebServer == null;
    }



    public void send(String message) {
        String request = mServerAddress + "?message=" + Uri.encode(message);
        Debug.log(request);
        AsyncHttpClient.getDefaultInstance().executeString(new AsyncHttpGet(request),
                new AsyncHttpClient.StringCallback() {
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
                Debug.log("Server response: " + result);
            }
        });
    }
    public void send(String clientAddress, String message) {
        String request = clientAddress + "?message=" + Uri.encode(message);
        Debug.log(request);
        AsyncHttpClient.getDefaultInstance().executeString(new AsyncHttpGet(request),
                new AsyncHttpClient.StringCallback() {
                    @Override
                    public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
                        Debug.log("Server response: " + result);
                    }
                });
    }

    private String getAPIUrl(String host, int port) {
        return String.format(Locale.ROOT, "http://%s:%d/api/", host, port);
    }

    void setServerAddress(String host, int port) {
        mServerAddress = getAPIUrl(host, port);
    }

    String getDeviceAddress() {
        return getAPIUrl(mLocalHost, mLocalPort);
    }

    int getPort() {
        return mLocalPort;
    }

    String getHost() {
        return mLocalHost;
    }

    public String[] getWifiInfo() {
        String[] result = new String[] {"", ""};
        if (mWifiManager != null && mWifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                if (state == NetworkInfo.DetailedState.CONNECTED
                        || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                    result[0] = formatIpAddress(wifiInfo.getIpAddress());
                    result[1] = wifiInfo.getSSID();
                }
            }
        }
        return result;
    }

    public static String formatIpAddress(int intIp) {
        byte[] iPAddress = BigInteger.valueOf(intIp).toByteArray();
        ArrayUtils.reverse(iPAddress);
        try {
            return InetAddress.getByAddress(iPAddress).getHostAddress();
        } catch (Exception e) {
            // not interesting
        }
        return "";
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

