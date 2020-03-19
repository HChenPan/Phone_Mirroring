package phone_mirroring.huangcp.com.utils;


import android.app.Application;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.os.Build;

/**
 * 系统信息： 版本号 屏幕信息
 * 每个Android App运行时，会首先自动创建Application 类并实例化 Application 对象，且只有一个 即 Application类 是单例模式（singleton）类
 * 即不同的组件（如Activity、Service）都可获得Application对象且都是同一个对象
 *
 * @author Huangcp
 * @date 2020/3/12 下午 04:22
 **/

public class SystemInfo extends Application {
    /**
     * 系统API版本号
     */
    public int api;
    /**
     * 屏幕宽度
     */
    public int screen_width;
    /**
     * 屏幕高度
     */
    public int screen_height;
    /**
     * 屏幕密度
     **/
    public int screen_dpi;

    private static SystemInfo systemInfo;
    /**
     * 显示管理 服务
     */
    private DisplayManager displayManager;
    /**
     * 捕捉屏幕API
     */
    private MediaProjection mediaProjection;

    public int getApi() {
        return api;
    }

    public void setApi(int api) {
        this.api = api;
    }

    public int getScreen_width() {
        return screen_width;
    }

    public void setScreen_width(int screen_width) {
        this.screen_width = screen_width;
    }

    public int getScreen_height() {
        return screen_height;
    }

    public void setScreen_height(int screen_height) {
        this.screen_height = screen_height;
    }

    public int getScreen_dpi() {
        return screen_dpi;
    }

    public void setScreen_dpi(int screen_dpi) {
        this.screen_dpi = screen_dpi;
    }

    public static SystemInfo getSystemInfo() {
        return systemInfo;
    }

    public static void setSystemInfo(SystemInfo systemInfo) {
        SystemInfo.systemInfo = systemInfo;
    }

    public DisplayManager getDisplayManager() {
        return displayManager;
    }

    public void setDisplayManager(DisplayManager displayManager) {
        this.displayManager = displayManager;
    }

    public MediaProjection getMediaProjection() {
        return mediaProjection;
    }

    public void setMediaProjection(MediaProjection mediaProjection) {
        this.mediaProjection = mediaProjection;
    }

    public static SystemInfo getInstance() {
        return systemInfo;
    }

    /**
     * description(描述) 初始化 应用程序级别 的资源，如全局对象、环境配置变量、图片资源初始化、推送服务的注册等
     *
     * @return void
     * @author Huangcp
     * @date 2020/3/14 上午 01:43
     **/
    @Override
    public void onCreate() {
        super.onCreate();
        systemInfo = this;
        setApi(Build.VERSION.SDK_INT);
        setScreen_width(getResources().getDisplayMetrics().widthPixels);
        setScreen_height(getResources().getDisplayMetrics().heightPixels);
        setScreen_dpi(getResources().getDisplayMetrics().densityDpi);
    }
}
