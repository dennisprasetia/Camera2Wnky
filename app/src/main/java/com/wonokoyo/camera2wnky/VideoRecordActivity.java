package com.wonokoyo.camera2wnky;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public class VideoRecordActivity extends AppCompatActivity implements
        SurfaceHolder.Callback, View.OnClickListener {

    protected static final int RESULT_ERROR = 0x00000001;

    private static final int MAX_VIDEO_DURATION = 60 * 1000;
    private static final int ID_TIME_COUNT = 0x1006;

    private SurfaceView mSurfaceView;
    private ImageView iv_cancel, iv_ok, iv_record;
    private TextView tv_counter;

    private SurfaceHolder mSurfaceHolder;
    private MediaRecorder mMediaRecorder;
    private Camera mCamera;

    private List<Camera.Size> mSupportVideoSizes;

    private String filePath;

    private boolean mIsRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_record);

        getSupportActionBar().hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        initView();
    }

    private void initView() {
        mSurfaceView = findViewById(R.id.surfaceView);

        iv_record = findViewById(R.id.imgCapture);
        iv_cancel = findViewById(R.id.imgCancel);
        iv_ok = findViewById(R.id.imgAccept);

        tv_counter = findViewById(R.id.txtCounter);
        tv_counter.setVisibility(View.GONE);

        iv_record.setOnClickListener(this);

        iv_cancel.setOnClickListener(this);
        iv_cancel.setVisibility(View.GONE);

        iv_ok.setOnClickListener(this);
        iv_ok.setVisibility(View.GONE);

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
    }

    private void exit(final int resultCode, final Intent data) {
        if (mIsRecording) {
            new AlertDialog.Builder(VideoRecordActivity.this)
                    .setTitle("Video Recorder")
                    .setMessage("Do you want to exit?")
                    .setPositiveButton("yes",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    stopRecord();
                                    if (resultCode == RESULT_CANCELED) {
                                        if (filePath != null)
                                            deleteFile(new File(filePath));
                                    }
                                    setResult(resultCode, data);
                                    finish();
                                }
                            })
                    .setNegativeButton("no",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {

                                }
                            }).show();
            return;
        }
        if (resultCode == RESULT_CANCELED) {
            if (filePath != null)
                deleteFile(new File(filePath));
        }
        setResult(resultCode, data);
        finish();
    }

    private void deleteFile(File delFile) {
        if (delFile == null) {
            return;
        }
        final File file = new File(delFile.getAbsolutePath());
        delFile = null;
        new Thread() {
            @Override
            public void run() {
                super.run();
                if (file.exists()) {
                    file.delete();
                }
            }
        }.start();
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case ID_TIME_COUNT:
                    if (mIsRecording) {
                        if (msg.arg1 > msg.arg2) {
                            // mTvTimeCount.setVisibility(View.INVISIBLE);
                            tv_counter.setText("00:00");
                            stopRecord();
                        } else {
                            if ((msg.arg2 - msg.arg1) > 9) {
                                tv_counter.setText("00:" + (msg.arg2 - msg.arg1));
                            } else {
                                tv_counter.setText("00:0" + (msg.arg2 - msg.arg1));
                            }
                            Message msg2 = mHandler.obtainMessage(ID_TIME_COUNT,
                                    msg.arg1 + 1, msg.arg2);
                            mHandler.sendMessageDelayed(msg2, 1000);
                        }
                    }
                    break;

                default:
                    break;
            }
        };
    };

    private void openCamera() {
        try {
            this.mCamera = Camera.open();
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setRotation(90);
            parameters.set("orientation", "portrait");

            mCamera.lock();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                try {
                    mCamera.setDisplayOrientation(90);
                } catch (NoSuchMethodError e) {
                    e.printStackTrace();
                }
            }
            mSupportVideoSizes = parameters.getSupportedVideoSizes();
            if (mSupportVideoSizes == null || mSupportVideoSizes.isEmpty()) {
                String videoSize = parameters.get("video-size");
                mSupportVideoSizes = new ArrayList<>();
                if (!VideoRecordActivity.isEmpty(videoSize)) {
                    String[] size = videoSize.split("x");
                    if (size.length > 1) {
                        try {
                            int width = Integer.parseInt(size[0]);
                            int height = Integer.parseInt(size[1]);
                            mSupportVideoSizes.add(mCamera.new Size(width, height));
                        } catch (Exception e) {
                            Log.e("Eror", e.toString());
                        }
                    }
                }
            }
            parameters.setPreviewSize(mSupportVideoSizes.get(1).width, mSupportVideoSizes.get(1).height);
            parameters.setPictureSize(mSupportVideoSizes.get(1).width, mSupportVideoSizes.get(1).height);
            mCamera.setParameters(parameters);
            for (Camera.Size size : mSupportVideoSizes) {
                Log.i("Info", size.width + "<>" + size.height);
            }
        } catch (Exception e) {
            Log.e("Error", "Open Camera error\n" + e.toString());
        }
    }

    private boolean initVideoRecorder() {
        mMediaRecorder = new MediaRecorder();

        if (mCamera == null) {
            mCamera = Camera.open();
            mCamera.unlock();
        } else {
            mCamera.unlock();
        }

        mMediaRecorder.setCamera(mCamera);

        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            /*CamcorderProfile lowProfile = CamcorderProfile
                    .get(CamcorderProfile.QUALITY_LOW);
            CamcorderProfile hightProfile = CamcorderProfile
                    .get(CamcorderProfile.QUALITY_HIGH);
            if (lowProfile != null && hightProfile != null) {
                lowProfile.videoFrameRate = 24;
                lowProfile.videoBitRate = 1500000;
                if (mSupportVideoSizes != null && !mSupportVideoSizes.isEmpty()) {
                    int width = 640;
                    int height = 480;
                    Collections.sort(mSupportVideoSizes, new SizeComparator());
                    int lwd = mSupportVideoSizes.get(1).width;
                    for (Camera.Size size : mSupportVideoSizes) {
                        int wd = Math.abs(size.width - 640);
                        if (wd < lwd) {
                            width = size.width;
                            height = size.height;
                            lwd = wd;
                        } else {
                            break;
                        }
                    }

                    lowProfile.videoFrameWidth = mSupportVideoSizes.get(10).width;
                    lowProfile.videoFrameHeight = mSupportVideoSizes.get(10).height;
                }

                mMediaRecorder.setProfile(lowProfile);
            }*/

            mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_CIF));
        } catch (Exception e) {
            try {
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (mSupportVideoSizes != null && !mSupportVideoSizes.isEmpty()) {
                Collections.sort(mSupportVideoSizes, new SizeComparator());
                Camera.Size size = mSupportVideoSizes.get(1);
                try {
                    mMediaRecorder.setVideoSize(size.width, size.height);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                try {
                    mMediaRecorder.setVideoSize(640, 480);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
        }

        File f = null;
        try {
            f = setUpVideoFile();
            filePath = f.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            f = null;
            filePath = null;
        }
        mMediaRecorder.setOutputFile(filePath);

        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            try {
                mMediaRecorder.setOrientationHint(90);
            } catch (NoSuchMethodError e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d("VideoPreview",
                    "IllegalStateException preparing MediaRecorder: "
                            + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d("VideoPreview",
                    "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (Exception e) {
            releaseMediaRecorder();
            e.printStackTrace();
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private void startRecord() {
        try {
            if (initVideoRecorder()) {
                mMediaRecorder.start();
                iv_record.setBackgroundResource(R.drawable.ic_stop_white);
                Toast.makeText(VideoRecordActivity.this, "Start Recording", Toast.LENGTH_LONG).show();
                Log.d("Debug", "Start Recording");
            } else {
                releaseMediaRecorder();
                iv_record.setBackgroundResource(R.drawable.ic_capture_white);
            }
            tv_counter.setVisibility(View.VISIBLE);
            tv_counter.setText("00:" + (MAX_VIDEO_DURATION / 1000));
            Message msg = mHandler.obtainMessage(ID_TIME_COUNT, 1,
                    MAX_VIDEO_DURATION / 1000);
            mHandler.sendMessage(msg);
            mIsRecording = true;
        } catch (Exception e) {
            showShortToast("problem while capturing video");
            e.printStackTrace();
            exit(RESULT_ERROR, null);
        }
    }

    private void stopRecord() {
        try {
            mMediaRecorder.stop();
            mCamera.stopPreview();
        } catch (Exception e) {
            if (new File(filePath) != null
                    && new File(filePath).exists()) {
                new File(filePath).delete();
            }
        }
        releaseMediaRecorder();
        mCamera.lock();
        iv_record.setBackgroundResource(R.drawable.ic_capture_white);
        mIsRecording = false;

        iv_record.setVisibility(View.GONE);
        iv_cancel.setVisibility(View.VISIBLE);
        iv_ok.setVisibility(View.VISIBLE);
    }

    public static void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo(); // Since API level 9
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @Override
    protected void onResume() {
        super.onResume();
        openCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit(RESULT_CANCELED, null);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View arg0) {
        switch (arg0.getId()) {
            case R.id.imgAccept:
                Intent data = new Intent();
                if (filePath != null) {
                    data.putExtra("videopath", filePath);
                }
                exit(RESULT_OK, data);
                break;
            case R.id.imgCancel:
                exit(RESULT_CANCELED, null);
                break;
            case R.id.imgCapture:
                if (mIsRecording) {
                    stopRecord();
                } else {
                    startRecord();
                }
                break;
            default:
                break;
        }
    }

    protected void showShortToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private File setUpVideoFile() throws IOException {

        File videoFile = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment
                .getExternalStorageState())) {

            File storageDir = new File(
                    String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)) +
                    "/WONOKOYO");

            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d("CameraSample", "failed to create directory");
                        return null;
                    }
                }
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String date = sdf.format(new Date());
            videoFile = File.createTempFile("WNKY_" + date, ".mp4", storageDir);
        } else {
            Log.v(getString(R.string.app_name),
                    "External storage is not mounted READ/WRITE.");
        }

        return videoFile;
    }

    private class SizeComparator implements Comparator<Camera.Size> {

        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            return rhs.width - lhs.height;
        }
    }

    public static boolean isEmpty(String str) {
        return str == null || "".equals(str.trim());
    }
}
