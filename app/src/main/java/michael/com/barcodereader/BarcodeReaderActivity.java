package michael.com.barcodereader;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.microblink.detectors.DetectorResult;
import com.microblink.detectors.points.PointsDetectorResult;
import com.microblink.detectors.quad.QuadDetectorResult;
import com.microblink.hardware.SuccessCallback;
import com.microblink.hardware.orientation.Orientation;
import com.microblink.metadata.DetectionMetadata;
import com.microblink.metadata.Metadata;
import com.microblink.metadata.MetadataListener;
import com.microblink.metadata.MetadataSettings;
import com.microblink.recognition.InvalidLicenceKeyException;
import com.microblink.recognizers.BaseRecognitionResult;
import com.microblink.recognizers.RecognitionResults;
import com.microblink.recognizers.blinkbarcode.BarcodeType;
import com.microblink.recognizers.blinkbarcode.bardecoder.BarDecoderRecognizerSettings;
import com.microblink.recognizers.blinkbarcode.bardecoder.BarDecoderScanResult;
import com.microblink.recognizers.blinkbarcode.pdf417.Pdf417RecognizerSettings;
import com.microblink.recognizers.blinkbarcode.pdf417.Pdf417ScanResult;
import com.microblink.recognizers.blinkbarcode.zxing.ZXingRecognizerSettings;
import com.microblink.recognizers.blinkbarcode.zxing.ZXingScanResult;
import com.microblink.recognizers.settings.RecognitionSettings;
import com.microblink.recognizers.settings.RecognizerSettings;
import com.microblink.util.CameraPermissionManager;
import com.microblink.view.CameraAspectMode;
import com.microblink.view.CameraEventsListener;
import com.microblink.view.OrientationAllowedListener;
import com.microblink.view.recognition.RecognizerView;
import com.microblink.view.recognition.ScanResultListener;
import com.microblink.view.viewfinder.PointSetView;
import com.microblink.view.viewfinder.quadview.QuadViewManager;
import com.microblink.view.viewfinder.quadview.QuadViewManagerFactory;
import com.microblink.view.viewfinder.quadview.QuadViewPreset;

public class BarcodeReaderActivity extends Activity implements ScanResultListener, CameraEventsListener, MetadataListener {

    private final static String TAG = BarcodeReaderActivity.class.getSimpleName();
    private RecognizerView mRecognizerView;
    private CameraPermissionManager mCameraPermissionManager;
    private QuadViewManager mQvManager = null;
    private PointSetView mPointSetView;
    private View mLayout;
    private Button mBackButton;
    private Button mTorchButton;
    private RecognitionSettings mSettings;
    private boolean mTorchEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_scan);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mRecognizerView = findViewById(R.id.recognizerView);

        setLicenseKey();
        buildRecognitionSettings();
        setRecognizerViewSettings();
        setMetaDataSettings();

        mRecognizerView.setAnimateRotation(true);
        mRecognizerView.setAspectMode(CameraAspectMode.ASPECT_FILL);

        mCameraPermissionManager = new CameraPermissionManager(this);
        View v = mCameraPermissionManager.getAskPermissionOverlay();
        if (v != null) {
            ViewGroup vg = findViewById(R.id.my_default_scan_root);
            vg.addView(v);
        }

        mRecognizerView.create();

        mQvManager = QuadViewManagerFactory.createQuadViewFromPreset(mRecognizerView, QuadViewPreset.DEFAULT_CORNERS_FROM_PDF417_SCAN_ACTIVITY);
        mPointSetView = new PointSetView(this, null, mRecognizerView.getHostScreenOrientation());
        mRecognizerView.addChildView(mPointSetView, false);

        mLayout = getLayoutInflater().inflate(R.layout.default_barcode_camera_overlay, null);

        setupBackButton();
        setupTorchButton();
        mRecognizerView.addChildView(mLayout, true);
    }

    private void setupTorchButton() {
        mTorchButton = mLayout.findViewById(R.id.defaultTorchButton);
        mTorchButton.setVisibility(View.GONE);
    }

    private void setupBackButton() {
        mBackButton = mLayout.findViewById(R.id.defaultBackButton);
        mBackButton.setText(getString(R.string.mbHome));
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void setMetaDataSettings() {
        MetadataSettings metadataSettings = new MetadataSettings();
        metadataSettings.setDetectionMetadataAllowed(true);
        mRecognizerView.setMetadataListener(this, metadataSettings);
    }

    private void setLicenseKey() {
        try {
            mRecognizerView.setLicenseKey(License.MICROBLINK_LICENSE_KEY);
        } catch (InvalidLicenceKeyException e) {
            e.printStackTrace();
        }
    }

    private void setRecognizerViewSettings() {
        mRecognizerView.setRecognitionSettings(mSettings);
        mRecognizerView.setScanResultListener(this);
        mRecognizerView.setCameraEventsListener(this);
        mRecognizerView.setOrientationAllowedListener(new OrientationAllowedListener() {
            @Override
            public boolean isOrientationAllowed(Orientation orientation) {
                return false;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mRecognizerView != null) {
            mRecognizerView.start();
        }
        Log.d(TAG, "OnStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRecognizerView != null) {
            mRecognizerView.resume();
        }
        Log.d(TAG, "OnResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRecognizerView != null) {
            mRecognizerView.pause();
        }
        Log.d(TAG, "OnPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mRecognizerView != null) {
            mRecognizerView.stop();
        }
        Log.d(TAG, "OnStop");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRecognizerView != null) {
            mRecognizerView.destroy();
        }
        Log.d(TAG, "OnDestroy");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mRecognizerView != null) {
            mRecognizerView.changeConfiguration(newConfig);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onCameraPreviewStarted() {
        if (mRecognizerView.isCameraTorchSupported()) {
            mTorchButton.setVisibility(View.VISIBLE);
            mTorchButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mRecognizerView.setTorchState(!mTorchEnabled, new SuccessCallback() {
                        @Override
                        public void onOperationDone(final boolean success) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (success) {
                                        mTorchEnabled = !mTorchEnabled;
                                        if (mTorchEnabled) {
                                            mTorchButton.setText("ON");
                                        } else {
                                            mTorchButton.setText("OFF");
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onCameraPreviewStopped() {
        Log.d(TAG, "CameraPreview stopped");
    }

    @Override
    public void onAutofocusFailed() {
        Log.d(TAG, "AutoFocus failed");
    }

    @Override
    public void onAutofocusStarted(Rect[] rects) {
        Log.d(TAG, "AutoFocus started");
    }

    @Override
    public void onAutofocusStopped(Rect[] rects) {
        Log.d(TAG, "AutoFocus stopped");
        mRecognizerView.focusCamera();
    }

    @Override
    public void onMetadataAvailable(Metadata metadata) {
        if (metadata instanceof DetectionMetadata) {
            DetectorResult detectorResult = ((DetectionMetadata) metadata).getDetectionResult();
            if (detectorResult == null) {
                if (mPointSetView != null) {
                    mPointSetView.setPointsDetectionResult(null);
                }
                if (mQvManager != null) {
                    mQvManager.animateQuadToDefaultPosition();
                }
            } else if (detectorResult instanceof PointsDetectorResult) {
                mPointSetView.setPointsDetectionResult((PointsDetectorResult) detectorResult);
            } else if (detectorResult instanceof QuadDetectorResult) {
                mQvManager.animateQuadToDetectionPosition((QuadDetectorResult) detectorResult);
                if (mPointSetView != null) {
                    mPointSetView.setPointsDetectionResult(null);
                }
            }
        }
    }

    @Override
    public void onScanningDone(RecognitionResults results) {
        final boolean isDataAvailable = validResult(results);
        if (isDataAvailable) {
            mRecognizerView.pauseScanning();
            showResults(results);
        }
    }

    @Override
    public void onError(Throwable ex) {
        com.microblink.util.Log.e(this, ex, "Error");
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setMessage("There has been an error!")
                .setTitle("Error")
                .setCancelable(false)
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }).create().show();
    }

    @Override
    @TargetApi(23)
    public void onCameraPermissionDenied() {
        mCameraPermissionManager.askForCameraPermission();
    }

    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        mCameraPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void buildRecognitionSettings() {
        Log.d("", "buildRecognitionSettings()");

        Pdf417RecognizerSettings pdf417RecognizerSettings = new Pdf417RecognizerSettings();
        pdf417RecognizerSettings.setNullQuietZoneAllowed(true);

        BarDecoderRecognizerSettings oneDimensionalRecognizerSettings = new BarDecoderRecognizerSettings();
        oneDimensionalRecognizerSettings.setScanCode39(true);
        oneDimensionalRecognizerSettings.setScanCode128(true);
        oneDimensionalRecognizerSettings.setTryHarder(true);

        ZXingRecognizerSettings zXingRecognizerSettings = new ZXingRecognizerSettings();
        zXingRecognizerSettings.setScanQRCode(true);
        zXingRecognizerSettings.setScanITFCode(true);

        Log.d("", "creating mSettings object");
        mSettings = new RecognitionSettings();

        Log.d("", "setting mSettings");
        mSettings.setRecognizerSettingsArray(new RecognizerSettings[]{
                pdf417RecognizerSettings,
                oneDimensionalRecognizerSettings,
                zXingRecognizerSettings
        });
    }

    public void showResults(RecognitionResults results) {
        StringBuilder sb = new StringBuilder();
        String barcodeData;

        if (results != null) {
            BaseRecognitionResult[] resultArray = results.getRecognitionResults();

            for (BaseRecognitionResult res : resultArray) {
                if (res instanceof Pdf417ScanResult) {
                    Pdf417ScanResult result = (Pdf417ScanResult) res;
                    barcodeData = result.getStringData();
                    sb.append(barcodeData);

                } else if (res instanceof BarDecoderScanResult) {
                    BarDecoderScanResult result = (BarDecoderScanResult) res;
                    BarcodeType type = result.getBarcodeType();

                    barcodeData = result.getStringData();
                    sb.append(type.name());
                    sb.append(" string data:\n");
                    sb.append(barcodeData);
                    sb.append("\n\n\n");

                } else if (res instanceof ZXingScanResult) {
                    ZXingScanResult result = (ZXingScanResult) res;

                    BarcodeType type = result.getBarcodeType();
                    barcodeData = result.getStringData();
                    sb.append(type.name());
                    sb.append(" string data:\n");
                    sb.append(barcodeData);
                    sb.append("\n\n\n");

                }
            }
        } else {
            sb = new StringBuilder();
            sb.append("Bad image or invalid barcode. Please retry...");
        }
        showData(sb.toString(), mRecognizerView);
    }

    private void showData(String message, final RecognizerView view) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Scan result")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        view.resumeScanning(true);
                    }
                })
                .create();
        dialog.show();
    }

    private boolean validResult(RecognitionResults result) {
        BaseRecognitionResult[] brrs = result.getRecognitionResults();
        boolean isDataAvailable = false;
        if (brrs != null) {
            Log.d("BarcodeReaderActivity", "looping through BaseRecognitionResult[]");

            for (BaseRecognitionResult brr : brrs) {
                if (!brr.isEmpty() && brr.isValid()) {
                    Log.d(TAG, "Found some valid data!");
                    isDataAvailable = true;
                    break;
                }
            }
        }
        return isDataAvailable;
    }

}
