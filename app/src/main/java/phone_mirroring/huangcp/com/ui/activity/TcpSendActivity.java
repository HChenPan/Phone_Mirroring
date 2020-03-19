package phone_mirroring.huangcp.com.ui.activity;


import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Surface;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import phone_mirroring.huangcp.com.R;
import phone_mirroring.huangcp.com.utils.EGLRender;
import phone_mirroring.huangcp.com.utils.LogUtil;
import phone_mirroring.huangcp.com.utils.SpUtil;
import phone_mirroring.huangcp.com.utils.SystemInfo;

/**
 * @author Huangcp
 * @date 2020/3/14 上午 01:33
 **/

public class TcpSendActivity extends Activity {

    private VideoView videoViewTest;
    /**
     * 创建输入流缓冲
     */
    private BufferedInputStream inputStream;
    /**
     * 创建输出流缓冲
     */
    private BufferedOutputStream outputStream;
    private String ip = "192.168.2.30";
    /**
     * TCP连接线程
     */
    private ExecutorService tcpSendThreadPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1024), new ThreadFactoryBuilder().setNameFormat("tcpSend-pool-%d").build(), new ThreadPoolExecutor.AbortPolicy());
    /**
     * 编码线程
     */
    private ExecutorService mediaEncoderhreadPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1024), new ThreadFactoryBuilder().setNameFormat("Media-Encoder-pool-%d").build(), new ThreadPoolExecutor.AbortPolicy());
    private EGLRender eglRender;
    /**
     * startRecording
     */
    private DisplayManager displayManager = SystemInfo.getInstance().getDisplayManager();
    private MediaProjection projection = SystemInfo.getInstance().getMediaProjection();
    //屏幕相关
    private int screenWidth;
    private int screenHeight;
    private int screenDpi;

    private MediaCodec mEncoder;
    private VirtualDisplay virtualDisplay;


    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private static final int HANDLER_CONN_SUCCESS = 0;
    private static final int HANDLER_CUT_SCRENN_SUCCESS = 1;

    /**
     * 在主线程创建一个handler 这个handler的消息就会发送到主线程的消息队列中
     */
    private Handler handler = new Handler(msg -> {
        //可以通过handler在主线程中处理消息 在handerMessage方法中处理消息
        switch (msg.what) {
            case HANDLER_CUT_SCRENN_SUCCESS:
                try {
                    FileOutputStream outputStream = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/111.png");
                    Bitmap bitmap = (Bitmap) msg.obj;
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    Toast.makeText(TcpSendActivity.this, "截图已保存至:sdCard/111.png", Toast.LENGTH_SHORT).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case HANDLER_CONN_SUCCESS:
                Toast.makeText(TcpSendActivity.this, "连接成功，开始录屏", Toast.LENGTH_SHORT).show();
                break;
            default:
        }
        return false;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //初始化UI界面
        setContentView(R.layout.activity_tcp_send);

        videoViewTest = findViewById(R.id.videoView_test);
        //内置资源文件路径
        String uri = "android.resource://" + getPackageName() + "/" + R.raw.test;
        //加载资源到控件
        videoViewTest.setVideoURI(Uri.parse(uri));
        //设置视频播放控件监听，初始化界面就播放测试视频
        videoViewTest.setOnPreparedListener(mediaPlayer -> {
            //设置循环播放
            mediaPlayer.setLooping(true);
        });

        //设置截图按钮监听
        findViewById(R.id.button_cutscreen).setOnClickListener(v -> {
            if (eglRender != null) {
                //屏幕截图
                eglRender.cutScreen();
            }
        });
        //获取主界面传递过来的ip地址
        ip = getIntent().getStringExtra("ip");
        //TCP连接线程调用
        tcpSendThreadPool.execute(() -> {
            System.out.println(Thread.currentThread().getName());
            boolean isRuning;
            try {
                LogUtil.e("等待连接");
                int port = 6111;
                //创建socket连接
                Socket socket = new Socket(ip, port);
                LogUtil.e("连接成功");
                //输入流缓冲
                inputStream = new BufferedInputStream(socket.getInputStream());
                //输出流缓冲
                outputStream = new BufferedOutputStream(socket.getOutputStream());
                //保存IP
                SpUtil.init(this).setip(ip);
                //handler发吐司连接成功
                handler.sendEmptyMessage(HANDLER_CONN_SUCCESS);
                //开始录制屏幕内容
                startRecording();
                isRuning = true;
                while (isRuning) {
                    //输入流预估值
                    int readSize = inputStream.available();
                    if (readSize < 4) {
                        try {
                            Thread.sleep(3);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * description(描述) 初始化屏幕参数
     *
     * @param screenWidth  宽度
     * @param screenHeight 高度
     * @param screenDpi    DPI
     * @author Huangcp
     * @date 2020/3/19 上午 12:26
     **/
    private void initScreenInfo(int screenWidth, int screenHeight, int screenDpi) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.screenDpi = screenDpi;
    }

    /**
     * 连接成功后开始录屏
     */
    private void startRecording() {
        //编码线程开启
        mediaEncoderhreadPool.execute(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //VERSION_CODES.LOLLIPOP 以上 屏幕初始化
                initScreenInfo(1080, 1920, ((SystemInfo) getApplication()).getScreen_dpi());
            } else {
                //VERSION_CODES.LOLLIPOP 以下 屏幕初始化
                initScreenInfo(((SystemInfo) getApplication()).getScreen_width(), ((SystemInfo) getApplication()).getScreen_height(), ((SystemInfo) getApplication()).getScreen_dpi());
            }
            try {
                //初始化编码器
                prepareEncoder();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (projection != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //创建一个VirtualDisplay来捕获屏幕内容
                    virtualDisplay = projection.createVirtualDisplay("screen", screenWidth, screenHeight, screenDpi,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, eglRender.getDecodeSurface(), null, null);
                }
            } else {
                //创建一个VirtualDisplay来捕获屏幕内容
                virtualDisplay = displayManager.createVirtualDisplay("screen", screenWidth, screenHeight, screenDpi,
                        eglRender.getDecodeSurface(), DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC);
            }
            startRecordScreen();
            release();
        });
        videoViewTest.start();
    }

    /**
     * 开始录屏
     */
    private void startRecordScreen() {
        //开始渲染
        eglRender.start(mEncoder, mBufferInfo, outputStream, handler);
    }

    /***/
    private void release() {
        if (mEncoder != null) {
            //终止编解码器
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
    }

    /**
     * 初始化编码器
     */
    private void prepareEncoder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                //创建最小的视频格式。
                MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, screenWidth, screenHeight);
                //颜色格式
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                //编码参数相关
                int frameBit = 2000000;
                //码流
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, frameBit);
                int frameRate = 20;
                //帧数
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
                int frameInternal = 1;
                //关键帧
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, frameInternal);
                //初始化MediaCodec
                mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
                //配置MediaCodec
                mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                //创建surface
                Surface surface = mEncoder.createInputSurface();
                int videoFps = 30;
                //创建EGL渲染
                eglRender = new EGLRender(surface, screenWidth, screenHeight, videoFps);
                //调用start()编码器开始工作
                mEncoder.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //关闭连接
        tcpSendThreadPool.shutdown();

        if (eglRender != null) {
            eglRender.stop();
            //关闭截屏
        }
        if (videoViewTest.isPlaying()) {
            videoViewTest.stopPlayback();
        }

    }
}
