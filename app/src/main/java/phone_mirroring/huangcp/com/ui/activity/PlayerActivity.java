package phone_mirroring.huangcp.com.ui.activity;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.RequiresApi;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import phone_mirroring.huangcp.com.R;
import phone_mirroring.huangcp.com.utils.LogUtil;

import static android.view.SurfaceHolder.Callback;

/**
 * @author Huangcp
 * @date 2020/3/14 上午 01:33
 **/
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PlayerActivity extends Activity implements Callback {

    /**
     * MediaCodec是Android提供的用于对音视频进行编解码的类
     */
    private MediaCodec decoder;
    /**
     * 缓冲输入流
     * 为另一个输入流添加一些功能，
     * 例如，提供"缓冲功能"以及支持"mark()标记"和"reset()重置方法"
     */
    private BufferedInputStream dataInputStream;
    /***/
    private boolean isStart = true;

    private int mCount = 0;
    /**
     * 传输的数据容量
     */
    private long timerSize;
    /**
     * 线程池大小，线程工厂(需要添加依赖implementation 'org.apache.commons:commons-lang3:3.9')
     */
    ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(false).build());

    ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("demo-pool-%d").build();

    ExecutorService singleThreadPool = new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1024),
            namedThreadFactory,
            new ThreadPoolExecutor.AbortPolicy()
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        //初始化界面
        initView();
    }

    private void initView() {
        SurfaceView surfaceviewPlay = findViewById(R.id.surfaceview_play);
        surfaceviewPlay.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //当Surface第一次创建后会立即调用该函数。程序可以在该函数中做些和绘制界面相关的初始化工作，一般情况下都是在另外的线程来绘制界面，所以不要在这个函数中绘制Surface。
        LogUtil.d("Surface创建");
        //解码器配置
        decodeConfig(holder.getSurface());
        //开启新的线程解析收到的视频流

        singleThreadPool.execute(() -> {
            System.out.println(Thread.currentThread().getName());
            try {
                //多线程并行处理定时任务
                scheduleTask();
                //创建一个ServerSocket，这里可以指定连接请求的队列长度
                //new ServerSocket(port,3);意味着当队列中有3个连接请求是，如果Client再请求连接，就会被Server拒绝
                int port = 6111;
                ServerSocket serverSocket = new ServerSocket(port);
                //从请求队列中取出一个连接
                Socket clientSocket = serverSocket.accept();
                // 处理这次连接
                //缓冲输入流 读取客户端数据
                dataInputStream = new BufferedInputStream(clientSocket.getInputStream());
                while (isStart) {
                    //返回此输入流下一个方法调用可以不受阻塞地从此输入流读取（或跳过）的估计字节数。
                    // 下一个调用可能是同一个线程，也可能是另一个线程。
                    // 一次读取或跳过此估计数个字节不会受阻塞，但读取或跳过的字节数可能小于该数。
                    //获取文件大小，进而用来定义缓冲数组的长度
                    if (dataInputStream.available() < 4) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    int ret = 0;
                    byte[] tmpArray = new byte[4];
                    //每一次循环都能构造出一个4个元素的一字节的数组
                    do {
                        //read(byte[],int off,int len);
                        //将输入流中最多 len 个数据字节读入 byte 数组。
                        // 尝试读取 len 个字节，但读取的字节也可能小于该值。
                        // 以整数形式返回实际读取的字节数。
                        //在输入数据可用、检测到流末尾或者抛出异常前，此方法一直阻塞。 
                        //如果 len 为 0，则不读取任何字节并返回 0；否则，尝试读取至少一个字节。如果因为流位于文件末尾而没有可用的字节，则返回值 -1；否则，至少读取一个字节并将其存储在 b 中。 
                        //将读取的第一个字节存储在元素 b[off] 中，下一个存储在 b[off+1] 中，依次类推。读取的字节数最多等于 len。设 k 为实际读取的字节数；
                        // 这些字节将存储在 b[off] 到 b[off+k-1] 的元素中，不影响 b[off+k] 到 b[off+len-1] 的元素。 
                        //在任何情况下，b[0] 到 b[off] 的元素以及 b[off+len] 到 b[b.length-1] 的元素都不会受到影响。 
                        LogUtil.d("ret:" + ret + "4-ret" + (4 - ret));
                        ret += dataInputStream.read(tmpArray, ret, 4 - ret);
                    } while (ret < 4);

                    //数据解析
                    paseTeacherMessage(tmpArray);

                }
                if (dataInputStream != null) {
                    dataInputStream.close();
                }
                clientSocket.close();
                serverSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });


    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //当Surface的状态（大小和格式）发生变化的时候会调用该函数，在surfaceCreated调用后该函数至少会被调用一次。
        LogUtil.d("Surface的状态变化了" + "format" + format + "width" + width + "height" + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //当Surface被摧毁前会调用该函数，该函数被调用后就不能继续使用Surface了，一般在该函数中来清理使用的资源。 当Surface被摧毁前会调用该函数，该函数被调用后就不能继续使用Surface了，一般在该函数中来清理使用的资源。
        LogUtil.d("Surface被销毁了");
    }

    private void paseTeacherMessage(byte[] data) {
        //这一个字节数组的大小
        int size = bytesToInt(data, 0);
        //返回给网速计算线程
        timerSize += size;
        byte[] tmpArray = new byte[size];
        int ret = 0;
        try {
            do {
                ret += dataInputStream.read(tmpArray, ret, size - ret);
            } while (ret < size);
            {
                //放入处理数据
                //获取缓存队列
                ByteBuffer[] inputBuffers = decoder.getInputBuffers();
                //返回要用有效数据填充的输入缓冲区的索引
                int inputBufferIndex = decoder.dequeueInputBuffer(0);
                if (inputBufferIndex >= 0) {
                    //获取编码器传入数据ByteBuffer
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    //清除以前数据
                    inputBuffer.clear();
                    //tmpArray需要编码器处理数据
                    inputBuffer.put(tmpArray, 0, tmpArray.length);
                    //通知编码器 数据放入
                    //第四个参数是时间戳，其实怎么写都无所谓，只要是按时间线性增加的就可以，这里就随便弄一个了。
                    // 后面一段的代码就是把缓冲区给释放掉，因为我们直接让解码器显示，就不需要解码出来的数据了，但是必须要这么释放一下，否则解码器始终给你留着，内存就该不够用了。
                    decoder.queueInputBuffer(inputBufferIndex, 0, tmpArray.length, mCount * 1000000 / 1000, 0);
                    mCount++;
                }

                //处理完成数据
                //获取编码数据
                BufferInfo bufferInfo = new BufferInfo();
                //获取解码数据
                int outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);
                while (outputBufferIndex >= 0) {
                    //outputBuffer 编码器处理完成的数据
                    //释放数据
                    decoder.releaseOutputBuffer(outputBufferIndex, true);
                    //可能一次放入的数据处理会输出多个数据
                    outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * description(描述) 新线程开启定时任务
     * 多线程并行处理定时任务
     *
     * @author Huangcp
     * @date 2020/3/14 下午 10:15
     **/
    private void scheduleTask() {
        /*
         * 多线程并行处理定时任务
         */
        // 第一个参数是任务，第二个参数为首次执行的延时时间，第三个参数为定时执行的间隔时间,第四个参数是时间单位
        scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> {
            LogUtil.d("接收速度:" + (timerSize / 1024) + "kb/s");
            timerSize = 0;
        }, 0, 1000, TimeUnit.MILLISECONDS);

    }

    /**
     * description(描述) 解码器配置
     *
     * @param surface 参数
     * @author Huangcp
     * @date 2020/3/14 下午 10:14
     **/
    private void decodeConfig(Surface surface) {
        try {
            //创建最小的视频格式。
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1080, 1920);
            //根据特定MIME类型(如"video/avc")创建codec
            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            //配置解码器
            //输入数据的格式(解码器)或输出数据的所需格式(编码器)
            // 指定Surface，用于解码器输出的渲染
            //指定一个crypto对象，用于对媒体数据进行安全解密。对于非安全的编解码器，传null。
            //当组件是编码器时，flags指定为常量CONFIGURE_FLAG_ENCODE 解码器则传入0
            decoder.configure(mediaFormat, surface, null, 0);
            //调用start()解码器开始工作
            decoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * description(描述) byte[]与int转换
     * Java 总是把 byte 当做有符处理；我们可以通过将其和 0xFF 进行二进制与得到它的无符值
     *
     * @param ary 参数
     * @return int
     * @author Huangcp
     * @date 2020/3/14 下午 11:21
     **/
    private static int bytesToInt(byte[] ary, int offset) {
        int value;
        value = (int) ((ary[offset] & 0xFF)
                | ((ary[offset + 1] << 8) & 0xFF00)
                | ((ary[offset + 2] << 16) & 0xFF0000)
                | ((ary[offset + 3] << 24) & 0xFF000000));
        return value;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.d("销毁线程");
        singleThreadPool.shutdown();
        scheduledThreadPoolExecutor.shutdown();
        isStart = false;
    }
}
