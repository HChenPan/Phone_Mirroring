package phone_mirroring.huangcp.com.utils;


import android.text.TextUtils;
import android.util.Log;

import java.util.Locale;

/**
 * Log工具，类似android.util.Log。
 * tag自动产生，格式: customTagPrefix:className.methodName(L:lineNumber),
 * customTagPrefix为空时只输出：className.methodName(L:lineNumber)。
 *
 * @author Huangcp
 * @date 2020/3/10 下午 11:32
 */
public class LogUtil {
    private static boolean IsDebug = true;

    private LogUtil() {
    }

    /**
     * description(描述) tag自动产生
     *
     * @return tag 格式: customTagPrefix:className.methodName(L:lineNumber)
     * @author Huangcp
     * @date 2020/3/11 上午 01:35
     **/
    private static String generateTag() {
        StackTraceElement caller = new Throwable().getStackTrace()[2];
        String tag = "%s.%s(L:%d)";
        String callerClazzName = caller.getClassName();
        callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1);
        tag = String.format(Locale.getDefault(), tag, callerClazzName, caller.getMethodName(), caller.getLineNumber());
        String customTagPrefix = "Debug_log";
        tag = TextUtils.isEmpty(customTagPrefix) ? tag : customTagPrefix + ":" + tag;
        return tag;
    }

    /**
     * description(描述) Log.d(tag, content)
     *
     * @param content 参数
     * @return void
     * @author Huangcp
     * @date 2020/3/11 上午 01:33
     **/
    public static void d(String content) {
        if (!IsDebug || TextUtils.isEmpty(content)) {
            return;
        }
        String tag = generateTag();
        Log.d(tag, content);
    }

    /**
     * description(描述) Log.d(tag, content, tr)
     *
     * @param content 内容
     * @param tr      参数
     * @return void
     * @author Huangcp
     * @date 2020/3/11 上午 01:33
     **/
    public static void d(String content, Throwable tr) {
        if (!IsDebug || TextUtils.isEmpty(content)) {
            return;
        }
        String tag = generateTag();
        Log.d(tag, content, tr);
    }

    /**
     * description(描述) Log.e(tag, content)
     *
     * @param content 参数
     * @return void
     * @author Huangcp
     * @date 2020/3/11 上午 01:33
     **/
    public static void e(String content) {
        if (!IsDebug || TextUtils.isEmpty(content)) {
            return;
        }
        String tag = generateTag();
        Log.e(tag, content);
    }

    /**
     * description(描述) Log.e(tag, content, tr)
     *
     * @param content
     * @param tr      参数
     * @return void
     * @author Huangcp
     * @date 2020/3/11 上午 01:33
     **/
    public static void e(String content, Throwable tr) {
        if (!IsDebug || TextUtils.isEmpty(content)) {
            return;
        }
        String tag = generateTag();
        Log.e(tag, content, tr);
    }

    /**
     * description(描述) Log.i(tag, content)
     *
     * @param content 内容
     * @return void
     * @author Huangcp
     * @date 2020/3/11 上午 01:32
     **/
    public static void i(String content) {
        if (!IsDebug || TextUtils.isEmpty(content)) {
            return;
        }
        String tag = generateTag();
        Log.i(tag, content);
    }

    /**
     * description(描述) Log.i(tag, content, tr)
     *
     * @param content 内容
     * @param tr      参数
     * @return void
     * @author Huangcp
     * @date 2020/3/11 上午 01:32
     **/
    public static void i(String content, Throwable tr) {
        if (!IsDebug || TextUtils.isEmpty(content)) {
            return;
        }
        String tag = generateTag();
        Log.i(tag, content, tr);
    }

    /**
     * description(描述) Log.v(tag, content)
     *
     * @param content 内容
     * @return void
     * @author Huangcp
     * @date 2020/3/11 上午 01:30
     **/
    public static void v(String content) {
        if (!IsDebug || TextUtils.isEmpty(content)) {
            return;
        }
        String tag = generateTag();
        Log.v(tag, content);
    }

    /**
     * description(描述) Log.v(tag, content, tr)
     *
     * @param content 内容
     * @param tr      参数
     * @return void
     * @author Huangcp
     * @date 2020/3/11 上午 01:30
     **/
    public static void v(String content, Throwable tr) {
        if (!IsDebug || TextUtils.isEmpty(content)) {
            return;
        }
        String tag = generateTag();
        Log.v(tag, content, tr);
    }

    /**
     * description(描述) Log.w(tag, content)
     *
     * @param content 内容
     * @return void
     * @author Huangcp
     * @date 2020/3/11 上午 01:29
     **/
    public static void w(String content) {
        if (!IsDebug || TextUtils.isEmpty(content)) {
            return;
        }
        String tag = generateTag();
        Log.w(tag, content);
    }

    /**
     * description(描述) Log.w(tag, content, tr)
     *
     * @param content 内容
     * @param tr      参数
     * @return void
     * @author Huangcp
     * @date 2020/3/11 上午 01:27
     **/
    public static void w(String content, Throwable tr) {
        if (!IsDebug || TextUtils.isEmpty(content)) {
            return;
        }
        String tag = generateTag();
        Log.w(tag, content, tr);
    }

    /**
     * description(描述) Log.w(tag, tr)
     *
     * @param tr 参数
     * @return void
     * @author Huangcp
     * @date 2020/3/11 上午 01:27
     **/
    public static void w(Throwable tr) {
        if (!IsDebug) {
            return;
        }
        String tag = generateTag();
        Log.w(tag, tr);
    }

    /**
     * description(描述) Log.wtf(tag, content)
     *
     * @param content 参数
     * @return void
     * @author Huangcp
     * @date 2020/3/11 上午 01:27
     **/
    public static void wtf(String content) {
        if (!IsDebug || TextUtils.isEmpty(content)) {
            return;
        }
        String tag = generateTag();
        Log.wtf(tag, content);
    }

    /**
     * description(描述) Log.wtf(tag, content, tr)
     *
     * @param content 内容
     * @param tr      参数
     * @return void
     * @author Huangcp
     * @date 2020/3/11 上午 01:26
     **/
    public static void wtf(String content, Throwable tr) {
        if (!IsDebug || TextUtils.isEmpty(content)) {
            return;
        }
        String tag = generateTag();
        Log.wtf(tag, content, tr);
    }

    /**
     * description(描述) Log.wtf(tag, tr)
     *
     * @param tr 参数
     * @return void
     * @author Huangcp
     * @date 2020/3/11 上午 01:25
     **/
    public static void wtf(Throwable tr) {
        if (!IsDebug) {
            return;
        }
        String tag = generateTag();
        Log.wtf(tag, tr);
    }
}
