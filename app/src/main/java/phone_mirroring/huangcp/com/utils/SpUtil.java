package phone_mirroring.huangcp.com.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * description(描述) SharedPreferences 操作封装
 *
 * @author Huangcp
 * @date 2020/3/12 下午 03:50
 **/
public class SpUtil {
    private SharedPreferences sharedPreferences;
    private static SpUtil sPutil;

    /**
     * 构造函数
     */
    private SpUtil(Context context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences("RecentIP", Context.MODE_PRIVATE);
        }
    }

    /**
     * description(描述) 初始化SharedPreferences
     *
     * @param context 上下文参数
     * @return SpUtil
     * @author Huangcp
     * @date 2020/3/14 上午 01:38
     **/
    public static SpUtil init(Context context) {


        if (sPutil == null) {
            sPutil = new SpUtil(context);
        }
        return sPutil;
    }

    /**
     * description(描述) 读取SharedPreferences中最近使用的IP地址
     *
     * @return IP 若存在最近的IP则返回该IP
     * @author Huangcp
     * @date 2020/3/14 上午 01:36
     **/
    public String getip() {
        return sharedPreferences.getString("IP", "");
    }

    /**
     * description(描述) 存入SharedPreferences中最近使用的IP地址
     *
     * @param ip 最近输入的有效IP
     * @author Huangcp
     * @date 2020/3/14 上午 01:37
     **/
    public void setip(String ip) {
        sharedPreferences.edit().putString("IP", ip).apply();
    }
}
