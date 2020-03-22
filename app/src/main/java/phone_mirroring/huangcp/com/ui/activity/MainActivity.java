package phone_mirroring.huangcp.com.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import phone_mirroring.huangcp.com.R;
import phone_mirroring.huangcp.com.utils.LogUtil;
import phone_mirroring.huangcp.com.utils.SpUtil;
import phone_mirroring.huangcp.com.utils.SystemInfo;

/**
 * @author Huangcp
 * @date 2020/3/13 下午 09:47
 **/


public class MainActivity extends Activity implements View.OnClickListener {

    private EditText edittextTcpSendIp;
    private static final String DEFAULT_IP = "192.168.2.30";
    /**
     * 请求码便于在onRequestPermissionsResult 方法中根据requestCode进行判断.
     */
    private final int PERMISSION_CODE = 0x12;
    /**
     * 结果码
     */
    private final int REQUEST_CODE = 0x11;
    /**
     * 系统级服务管理屏幕采集
     */
    private MediaProjectionManager mediaProjectionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化界面控件
        initView();
        //授权
        authorization();
    }


    private void initView() {
        Button buttonTcpPreview = findViewById(R.id.button_tcp_preview);
        Button buttonTcpSend = findViewById(R.id.button_tcp_send);
        Button buttonTcpSendPC = findViewById(R.id.button_tcp_sendPC);
        edittextTcpSendIp = findViewById(R.id.edittext_tcp_send_ip);
        //从 SharedPreferences 中读取最近的 IP 并填入 edittextTcpSendIp
        edittextTcpSendIp.setText(SpUtil.init(this).getip());
        buttonTcpPreview.setOnClickListener(this);
        buttonTcpSend.setOnClickListener(this);
        buttonTcpSendPC.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button_tcp_preview) {
            LogUtil.d("接收端程序启动");
            LogUtil.d("接收程序启动");
            startActivity(new Intent(this, PlayerActivity.class));
        } else if (id == R.id.button_tcp_send) {
            LogUtil.d("发送端程序启动");
            LogUtil.d("发送程序启动");
            String ip = edittextTcpSendIp.getText().toString();
            String ipRegex = "(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)\\.(25[0-5]|2[0-4]\\d|[0-1]\\d{2}|[1-9]?\\d)";
            if (TextUtils.isEmpty(ip)) {
                ip = DEFAULT_IP;
            }
            if (!ip.matches(ipRegex)) {
                Toast.makeText(this, "请输入有效的IP地址", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, TcpSendActivity.class);
            intent.putExtra("ip", ip);
            startActivity(intent);
        } else if (id == R.id.button_tcp_sendPC) {
            LogUtil.d("发送端Pc程序启动");
            LogUtil.d("发送程序启动");
            Intent intent = new Intent(this, TcpSendPCActivity.class);
            startActivity(intent);
        }
    }


    private void authorization() {
        LogUtil.d("授权");
        //查询系统版本 索取不同权限
        LogUtil.d("授权流程");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //系统版本号大于等于23 即 Android 6.0 (MarshMallow)
            LogUtil.d("授予读写权限");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CODE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //系统版本号大于等于21 即 Android 5.0 (Lollipop)
            LogUtil.d("授予申请录屏权限");
            //一个系统级的服务，类似WindowManager，AlarmManager等，你可以通过getSystemService方法来获取它的实例
            mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            if (mediaProjectionManager != null) {
                //返回一个Intent，必须将其传递给startActivityForResult（）才能开始屏幕捕获。
                // 该活动将提示用户是否允许屏幕捕获。
                // 通过startActivityForResult来传递这个intent，
                // 所以我们可以通过onActivityResult来获取结果，
                // 通过getMediaProjection来取出intent中的数据
                Intent screenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent();
                startActivityForResult(screenCaptureIntent, REQUEST_CODE);
            }
        } else {
            //Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
            //系统版本号小于21 即 Android 4.4W (Kitkat Wear) 以下 需要root权限或系统签名
            LogUtil.d("授予显示管理服务");
            //获取显示器的镜像显示服务
            DisplayManager systemService = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
            SystemInfo.getInstance().setDisplayManager(systemService);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtil.d("onActivityResult" + "授权结果");
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            LogUtil.d("读写授权成功继续申请录屏权限");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //通过onActivityResult来获取结果，通过getMediaProjection来取出intent中的数据
                MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
                //设置全局变量 MediaProjection mediaProjection
                SystemInfo.getInstance().setMediaProjection(mediaProjection);
            }
        } else {
            if (requestCode != PERMISSION_CODE) {
                LogUtil.d("Unknown request code: " + requestCode);
                return;
            }
            if (resultCode != RESULT_OK) {
                Toast.makeText(this,
                        "User denied screen sharing permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        LogUtil.e("onRequestPermissionsResult" + "授权结果");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //系统版本号大于等于21 即 Android 5.0 (Lollipop)
                LogUtil.d("授予申请录屏权限");
                //一个系统级的服务，类似WindowManager，AlarmManager等，你可以通过getSystemService方法来获取它的实例
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                    if (mediaProjectionManager != null) {
                        //返回一个Intent，必须将其传递给startActivityForResult（）才能开始屏幕捕获。
                        // 该活动将提示用户是否允许屏幕捕获。
                        // 通过startActivityForResult来传递这个intent，
                        // 所以我们可以通过onActivityResult来获取结果，
                        // 通过getMediaProjection来取出intent中的数据
                        Intent screenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent();
                        startActivityForResult(screenCaptureIntent, REQUEST_CODE);
                    }
                }
            }
        }

    }
}
