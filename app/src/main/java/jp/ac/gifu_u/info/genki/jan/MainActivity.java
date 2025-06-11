package jp.ac.gifu_u.info.genki.jan;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.gson.Gson; // ファイル上部に追加

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // ←これを追加！

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "カメラの使用が許可されていません", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchRakutenInfo(String janCode) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://app.rakuten.co.jp/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RakutenApiService service = retrofit.create(RakutenApiService.class);

        Call<RakutenResponse> call = service.searchItem(
                "1042527241521827202", // ←ここをあなたのIDに書き換え
                janCode,
                "json",
                1
        );

        call.enqueue(new Callback<RakutenResponse>() {
            @Override
            public void onResponse(Call<RakutenResponse> call, Response<RakutenResponse> response) {
                Log.d("RakutenAPI", "検索JANコード: " + janCode);
                if (response.isSuccessful()) {
                    // 🔽 レスポンス全体のJSONを確認
                    String json = new Gson().toJson(response.body());
                    Log.d("RakutenAPI", "レスポンス内容: " + json);

                    if (response.body() != null && !response.body().Items.isEmpty()) {
                        RakutenResponse.Item item = response.body().Items.get(0).Item;
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                                "商品: " + item.itemName + "\n¥" + item.itemPrice, Toast.LENGTH_LONG).show());
                    } else {
                        Log.e("RakutenAPI", "商品が見つかりませんでした");
                    }
                } else {
                    Log.e("RakutenAPI", "レスポンス失敗: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<RakutenResponse> call, Throwable t) {
                Log.e("RakutenAPI", "通信失敗", t);
            }
        });
    }



    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                ImageAnalysis imageAnalysis =
                        new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    scanBarcodes(image); // ML Kitで解析
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void scanBarcodes(ImageProxy image) {
        if (image == null || image.getImage() == null) {
            image.close();
            return;
        }

        @SuppressWarnings("UnsafeOptInUsageError")
        InputImage inputImage =
                InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees());

        BarcodeScanner scanner = BarcodeScanning.getClient();

        scanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue();
                        Log.d("ScanResult", "検出されたコード: " + rawValue);
                        runOnUiThread(() ->
                                Toast.makeText(this, "検出: " + rawValue, Toast.LENGTH_SHORT).show());

                        // 🔽楽天APIに問い合わせ
                        fetchRakutenInfo(rawValue);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ScanError", "スキャン失敗", e);
                })
                .addOnCompleteListener(task -> {
                    image.close();
                });
    }



}