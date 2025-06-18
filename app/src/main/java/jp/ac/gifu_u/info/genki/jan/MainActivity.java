package jp.ac.gifu_u.info.genki.jan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private PreviewView previewView;
    private TextView    textProductInfo;

    private ExecutorService cameraExecutor;

    /** 二重呼び出し防止用 */
    private String lastJan = "";
    private long   lastCallTime = 0;   // ms

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView      = findViewById(R.id.previewView);
        textProductInfo  = findViewById(R.id.textProductInfo);
        cameraExecutor   = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    // ───────────────────────────────── API 呼び出し ──────────────────────────────

    private void fetchRakutenInfo(String janCode) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://app.rakuten.co.jp/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RakutenApiService service = retrofit.create(RakutenApiService.class);

        Call<RakutenResponse> call = service.searchItem(
                "1042527241521827202",   // ★差し替え必須
                janCode,             // keyword として送る
                "json",
                1
        );

        call.enqueue(new Callback<RakutenResponse>() {
            @Override
            public void onResponse(Call<RakutenResponse> call, Response<RakutenResponse> response) {

                Log.d("RakutenAPI", "検索JAN: " + janCode);

                if (response.isSuccessful()) {

                    Log.d("RakutenAPI", "レスポンス: " + new Gson().toJson(response.body()));

                    if (response.body() != null && !response.body().Items.isEmpty()) {
                        RakutenResponse.Item item = response.body().Items.get(0).Item;
                        runOnUiThread(() -> {
                            textProductInfo.setText(
                                    "商品名: " + item.itemName + "\n価格: ¥" + item.itemPrice);
                        });
                    } else {
                        runOnUiThread(() -> textProductInfo.setText("商品が見つかりませんでした"));
                    }

                } else {
                    Log.e("RakutenAPI", "HTTPエラー: " + response.code());
                    runOnUiThread(() -> textProductInfo.setText("APIエラー: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<RakutenResponse> call, Throwable t) {
                Log.e("RakutenAPI", "通信失敗", t);
                runOnUiThread(() -> textProductInfo.setText("通信失敗: " + t.getClass().getSimpleName()));
            }
        });
    }

    /** 連続スキャン対策＋13桁数字のみ許可 */
    private void tryFetch(String raw) {

        if (!raw.matches("\\d{8,13}")) return;              // JAN/EAN 以外スキップ
        long now = System.currentTimeMillis();

        if (raw.equals(lastJan) && now - lastCallTime < 3000) return;  // 3秒以内は無視

        lastJan      = raw;
        lastCallTime = now;
        fetchRakutenInfo(raw);
    }

    // ───────────────────────────── CameraX & ML Kit ────────────────────────────

    private void startCamera() {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::scanBarcodes);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                );

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void scanBarcodes(ImageProxy image) {

        if (image.getImage() == null) {
            image.close();
            return;
        }

        InputImage inputImage =
                InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees());

        BarcodeScanning.getClient()
                .process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    for (com.google.mlkit.vision.barcode.common.Barcode bc : barcodes) {
                        String rawValue = bc.getRawValue();
                        Log.d("ScanResult", "検出: " + rawValue);
                        tryFetch(rawValue);    // ←ここだけ
                    }
                })
                .addOnCompleteListener(task -> image.close());
    }
}
