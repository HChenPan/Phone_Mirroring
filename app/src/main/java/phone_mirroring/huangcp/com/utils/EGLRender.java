package phone_mirroring.huangcp.com.utils;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Huangcp
 * @date 2020/3/17 上午 01:55
 **/

public class EGLRender implements SurfaceTexture.OnFrameAvailableListener {
    private boolean hasCutScreen = false;
    private boolean start;
    private long time = 0;
    private int mWidth;
    private int mHeight;
    private int fps;
    private int videoInterval;
    private boolean mFrameAvailable = true;
    private EGLDisplay megldisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext meglcontext = EGL14.EGL_NO_CONTEXT;
    private EGLContext meglcontextencoder = EGL14.EGL_NO_CONTEXT;
    private EGLSurface meglsurface = EGL14.EGL_NO_SURFACE;
    private EGLSurface meglsurfaceencoder = EGL14.EGL_NO_SURFACE;

    private SurfaceTexture mSurfaceTexture;
    private static final boolean VERBOSE = false;
    private Surface decodeSurface;
    private static final int HANDLER_CUT_SCRENN_SUCCESS = 1;
    private int count = 1;
    private int[] modelData;
    private int mProgram;
    private int mtextureid = -12345;
    private int mumvpmatrixhandle;
    private int mustmatrixhandle;
    private int maPositionHandle;
    private int maTextureHandle;
    private float[] mmvpmatrix = new float[16];
    private float[] mstmatrix = new float[16];
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final float[] FULL_RECTANGLE_COORDS = {
            -1.0f, -1.0f, 1.0f,
            1.0f, -1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f
    };
    private static final float[] FULL_RECTANGLE_TEX_COORDS = {
            0.0f, 1.0f, 1f, 1.0f,
            1.0f, 1.0f, 1f, 1.0f,
            0.0f, 0.0f, 1f, 1.0f,
            1.0f, 0.0f, 1f, 1.0f
    };
    private static final FloatBuffer FULL_RECTANGLE_BUF =
            createFloatBuffer(FULL_RECTANGLE_COORDS);
    private static final FloatBuffer FULL_RECTANGLE_TEX_BUF =
            createFloatBuffer(FULL_RECTANGLE_TEX_COORDS);
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uSTMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec4 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = uSTMatrix * aTextureCoord;\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec4 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord.xy/vTextureCoord.z);" +
                    "}\n";

    public EGLRender(Surface surface, int mWidth, int mHeight, int fps) {
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        initfps(fps);
        eglSetup(surface);
        makeCurrent();
        setup();
    }


    private final ExecutorService cutScreeenThreadPool = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1024), new ThreadFactoryBuilder().setNameFormat("CutScreeen-pool-%d").build(), new ThreadPoolExecutor.AbortPolicy());

    /**
     * 初始化FPS
     */
    private void initfps(int fps) {
        this.fps = fps;
        videoInterval = 1000 / fps;
    }

    /**
     * 创建OpenGL环境
     */
    private void eglSetup(Surface surface) {
        // 获取显示设备(默认的显示设备)
        megldisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        //判断是否存在显示设备
        if (megldisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }
        int[] version = new int[2];
        //初始化
        if (!EGL14.eglInitialize(megldisplay, version, 0, version, 1)) {
            megldisplay = null;
            throw new RuntimeException("unable to initialize EGL14");
        }
        // 获取FrameBuffer格式和能力
        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE,
                EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE
        };

        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];

        if (!EGL14.eglChooseConfig(megldisplay, attribList, 0, configs, 0, configs.length,
                numConfigs, 0)) {
            throw new RuntimeException("unable to find RGB888+recordable ES2 EGL config");
        }
        EGLConfig configEncoder = getConfig(2);
        // 创建OpenGL上下文
        int[] attribLists = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE};
        meglcontext = EGL14.eglCreateContext(megldisplay, configs[0], EGL14.EGL_NO_CONTEXT, attribLists, 0);
        checkEglError("eglCreateContext");
        if (meglcontext == null) {
            throw new RuntimeException("null context");
        }
        meglcontextencoder = EGL14.eglCreateContext(megldisplay, configEncoder, meglcontext, attribLists, 0);
        checkEglError("eglCreateContext");
        if (meglcontextencoder == null) {
            throw new RuntimeException("null context2");
        }
        int[] surfaceAttribs = {
                EGL14.EGL_WIDTH, mWidth,
                EGL14.EGL_HEIGHT, mHeight,
                EGL14.EGL_NONE
        };
        meglsurface = EGL14.eglCreatePbufferSurface(megldisplay, configs[0], surfaceAttribs, 0);

        checkEglError("eglCreatePbufferSurface");
        if (meglsurface == null) {
            throw new RuntimeException("surface was null");
        }
        int[] surfaceAttribs2 = {
                EGL14.EGL_NONE
        };
        meglsurfaceencoder = EGL14.eglCreateWindowSurface(megldisplay, configEncoder, surface, surfaceAttribs2, 0);
        checkEglError("eglCreateWindowSurface");
        if (meglsurfaceencoder == null) {
            throw new RuntimeException("surface was null");
        }
    }

    private EGLConfig getConfig(int version) {
        int renderableType = EGL14.EGL_OPENGL_ES2_BIT;
        int three = 3;
        if (version >= three) {
            renderableType |= EGLExt.EGL_OPENGL_ES3_BIT_KHR;
        }
        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE,
                renderableType,
                EGL14.EGL_NONE, 0,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(megldisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0)) {
            LogUtil.w("unable to find RGB8888 / " + version + " EGLConfig");
            return null;
        }
        return configs[0];
    }

    /**
     * Makes our EGL context and surface current.
     */
    private void makeCurrent() {
        if (!EGL14.eglMakeCurrent(megldisplay, meglsurface, meglsurface, meglcontext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    private void makeCurrent(int index) {
        if (index == 0) {
            if (!EGL14.eglMakeCurrent(megldisplay, meglsurface, meglsurface, meglcontext)) {
                throw new RuntimeException("eglMakeCurrent failed");
            }
        } else {
            if (!EGL14.eglMakeCurrent(megldisplay, meglsurfaceencoder, meglsurfaceencoder, meglcontextencoder)) {
                throw new RuntimeException("eglMakeCurrent failed");
            }
        }
    }

    /**
     * Creates interconnected instances of TextureRender, SurfaceTexture, and Surface.
     */
    private void setup() {
        Matrix.setIdentityM(mstmatrix, 0);
        surfaceCreated();
        if (VERBOSE) {
            LogUtil.d("textureID=" + getTextureId());
        }
        mSurfaceTexture = new SurfaceTexture(getTextureId());
        mSurfaceTexture.setDefaultBufferSize(mWidth, mHeight);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        decodeSurface = new Surface(mSurfaceTexture);
    }

    private void checkEglError(String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }

    public void cutScreen() {
        hasCutScreen = true;
    }

    public void stop() {
        start = false;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mFrameAvailable = true;
    }

    public Surface getDecodeSurface() {
        return decodeSurface;
    }

    /**
     * 开始录屏
     */
    public void start(MediaCodec mEncoder, MediaCodec.BufferInfo mBufferInfo, BufferedOutputStream outputStream, Handler handler) {
        start = true;
        while (start) {
            makeCurrent(1);
            awaitNewImage();
            long currentTime = System.currentTimeMillis();
            if (currentTime - time >= videoInterval) {
                // 帧率控制
                drawFrame();
                startEncode(mEncoder, mBufferInfo, outputStream);
                setPresentationTime(computePresentationTimeNsec(count++));
                swapBuffers();
                if (hasCutScreen) {
                    getScreen(handler);
                    hasCutScreen = false;
                }
                time = currentTime;
            }
        }
    }

    private void awaitNewImage() {
        if (mFrameAvailable) {
            mFrameAvailable = false;
            mSurfaceTexture.updateTexImage();
        }
    }

    private void drawImage() {
        drawFrame();
    }

    private void setPresentationTime(long nsecs) {
        EGLExt.eglPresentationTimeANDROID(megldisplay, meglsurfaceencoder, nsecs);
        checkEglError("eglPresentationTimeANDROID");
    }

    private long computePresentationTimeNsec(int frameIndex) {
        final long onebillion = 1000000000;
        return frameIndex * onebillion / fps;
    }

    private void swapBuffers() {
        EGL14.eglSwapBuffers(megldisplay, meglsurfaceencoder);
        checkEglError("eglSwapBuffers");
    }


    /**
     * 获取当前屏幕信息
     */
    private void getScreen(Handler handler) {
        IntBuffer buffer = IntBuffer.allocate(mWidth * mHeight);
        buffer.position(0);
        GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        modelData = buffer.array();
        buffer.clear();
        cutScreeenThreadPool.execute(() -> {
            int[] arData = new int[modelData.length];
            int offset1, offset2;
            for (int i = 0; i < mHeight; i++) {
                offset1 = i * mWidth;
                offset2 = (mHeight - i - 1) * mWidth;
                for (int j = 0; j < mWidth; j++) {
                    int texturePixel = modelData[offset1 + j];
                    int blue = (texturePixel >> 16) & 0xff;
                    int red = (texturePixel << 16) & 0x00ff0000;
                    int pixel = (texturePixel & 0xff00ff00) | red | blue;
                    arData[offset2 + j] = pixel;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(arData, mWidth, mHeight, Bitmap.Config.ARGB_8888);
            modelData = null;

            handler.obtainMessage(HANDLER_CUT_SCRENN_SUCCESS, bitmap).sendToTarget();
        });
    }

    private void startEncode(MediaCodec mEncoder, MediaCodec.BufferInfo mBufferInfo, BufferedOutputStream outputStream) {
        ByteBuffer[] byteBuffers = null;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            byteBuffers = mEncoder.getOutputBuffers();
        }
        int timeoutus = 10000;
        int index = mEncoder.dequeueOutputBuffer(mBufferInfo, timeoutus);
        if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat newFormat = mEncoder.getOutputFormat();
            LogUtil.i("output format changed.\n new format: " + newFormat.toString());
            getSpsPpsByteBuffer(newFormat);
            LogUtil.i("started media muxer, videoIndex");
        } else if (index == MediaCodec.INFO_TRY_AGAIN_LATER) {
            Log.d("---", "retrieving buffers time out!");
            try {
                // wait 10ms
                Thread.sleep(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (index >= 0) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                encodeToVideoTrack(byteBuffers[index], mBufferInfo, outputStream);
            } else {
                encodeToVideoTrack(mEncoder.getOutputBuffer(index), mBufferInfo, outputStream);
            }
            mEncoder.releaseOutputBuffer(index, false);
        }
    }

    private byte[] sps = null;
    private byte[] pps = null;

    /**
     * 获取编码SPS和PPS信息
     */
    private void getSpsPpsByteBuffer(MediaFormat newFormat) {
        sps = Objects.requireNonNull(newFormat.getByteBuffer("csd-0")).array();
        pps = Objects.requireNonNull(newFormat.getByteBuffer("csd-1")).array();
        LogUtil.d("编码器初始化完成");
    }

    private void encodeToVideoTrack(ByteBuffer encodeData, MediaCodec.BufferInfo mBufferInfo, BufferedOutputStream outputStream) {


        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            LogUtil.d("ignoring BUFFER_FLAG_CODEC_CONFIG");
            mBufferInfo.size = 0;
        }
        if (mBufferInfo.size == 0) {
            LogUtil.d("info.size == 0, drop it.");
            encodeData = null;
        } else {
            LogUtil.d("got buffer, info: size=" + mBufferInfo.size
                    + ", presentationTimeUs=" + mBufferInfo.presentationTimeUs
                    + ", offset=" + mBufferInfo.offset);
        }
        if (encodeData != null) {
            encodeData.position(mBufferInfo.offset);
            encodeData.limit(mBufferInfo.offset + mBufferInfo.size);
            byte[] bytes;
            if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME) {
                bytes = new byte[mBufferInfo.size + sps.length + pps.length];
                System.arraycopy(sps, 0, bytes, 0, sps.length);
                System.arraycopy(pps, 0, bytes, sps.length, pps.length);
                encodeData.get(bytes, sps.length + pps.length, mBufferInfo.size);
            } else {
                bytes = new byte[mBufferInfo.size];
                encodeData.get(bytes, 0, mBufferInfo.size);
            }
            onScreenInfo(bytes, outputStream);
            LogUtil.d("send:" + mBufferInfo.size + "\tflag:" + mBufferInfo.flags);
        }
    }

    private void onScreenInfo(byte[] data, BufferedOutputStream outputStream) {
        byte[] content = new byte[data.length + 4];
        System.arraycopy(intToBytes(data.length), 0, content, 0, 4);
        System.arraycopy(data, 0, content, 4, data.length);
        try {
            outputStream.write(content, 0, content.length);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] intToBytes(int value) {
        byte[] byteSrc = new byte[4];
        byteSrc[3] = (byte) ((value & 0xFF000000) >> 24);
        byteSrc[2] = (byte) ((value & 0x00FF0000) >> 16);
        byteSrc[1] = (byte) ((value & 0x0000FF00) >> 8);
        byteSrc[0] = (byte) ((value & 0x000000FF));
        return byteSrc;
    }


    /**
     * Initializes GL state.  Call this after the EGL surface has been created and made current.
     */
    private void surfaceCreated() {
        mProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (mProgram == 0) {
            throw new RuntimeException("failed creating program");
        }
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        mumvpmatrixhandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        mustmatrixhandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
        mtextureid = initTex();
    }

    private static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
//        checkGlError("glCreateProgram");
        if (program == 0) {
            LogUtil.e("Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
//        checkGlError("glAttachShader");
        GLES20.glAttachShader(program, pixelShader);
//        checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            LogUtil.e("Could not link program: ");
            LogUtil.e(GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }


    /**
     * Compiles the provided shader source.
     *
     * @return A handle to the shader, or 0 on failure.
     */
    private static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);

        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            LogUtil.e("Could not compile shader " + shaderType + ":");
            LogUtil.e(" " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    /**
     * Allocates a direct float buffer, and populates it with the float array data.
     */
    private static FloatBuffer createFloatBuffer(float[] coords) {
        final int sizeoffloat = 4;
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * sizeoffloat);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    /**
     * create external texture
     *
     * @return texture ID
     */
    private static int initTex() {
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        return tex[0];
    }

    private int getTextureId() {
        return mtextureid;
    }

    /**
     * Draws the external texture in SurfaceTexture onto the current EGL surface.
     */
    private void drawFrame() {
        GLES20.glUseProgram(mProgram);
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 3 * FLOAT_SIZE_BYTES, FULL_RECTANGLE_BUF);
        GLES20.glEnableVertexAttribArray(maTextureHandle);
        GLES20.glVertexAttribPointer(maTextureHandle, 4, GLES20.GL_FLOAT, false, 4 * FLOAT_SIZE_BYTES, FULL_RECTANGLE_TEX_BUF);
        Matrix.setIdentityM(mmvpmatrix, 0);
        GLES20.glUniformMatrix4fv(mumvpmatrixhandle, 1, false, mmvpmatrix, 0);
        GLES20.glUniformMatrix4fv(mustmatrixhandle, 1, false, mstmatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(maPositionHandle);
        GLES20.glDisableVertexAttribArray(maTextureHandle);
        GLES20.glUseProgram(0);
    }
}
