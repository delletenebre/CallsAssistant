package kg.delletenebre.callsassistant;

import android.util.Log;

class Debug {
    private static boolean sEnabled = true;

    private static void print(String type, String message) {
        if (sEnabled) {
            String fullClassName = Thread.currentThread().getStackTrace()[4].getClassName();
            String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
            String methodName = Thread.currentThread().getStackTrace()[4].getMethodName();
            int lineNumber = Thread.currentThread().getStackTrace()[4].getLineNumber();
//
//            for(StackTraceElement ste : Thread.currentThread().getStackTrace()) {
//                Log.d(className, String.valueOf(ste));
//            }
            StackTraceElement ste = Thread.currentThread().getStackTrace()[4];
            switch (type) {
                case "info":
                    Log.i(className, message + " @ " + ste);
                    break;
                case "error":
                    Log.e(className, message + " @ " + ste);
                    break;
                default:
                    Log.d(className, message + " @ " + ste);
                    break;
            }

        }
    }

    static void log(String message) {
        print("debug", message);
    }

    static void error(String message) {
        print("error", message);
    }

    static void info(String message) {
        print("info", message);
    }

    public static void setState(boolean state) {
        sEnabled = state;
    }
}
