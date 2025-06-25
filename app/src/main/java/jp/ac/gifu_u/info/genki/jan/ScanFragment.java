package jp.ac.gifu_u.info.genki.jan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.ac.gifu_u.info.genki.jan.databinding.FragmentScanBinding;

/**
 * カメラプレビュー＋JAN スキャンを担当する Fragment
 */
public class ScanFragment extends Fragment {

    private FragmentScanBinding binding;
    private ExecutorService     cameraExecutor;

    /** 二重呼び出し抑制用 */
    private String lastJan   = "";
    private long   lastCall  = 0;   // ms

    /*── Permission launcher ───────────────────────────────────────────────*/
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) startCamera();
                        else          Log.e("ScanFragment", "Camera permission denied");
                    });

    /*── Lifecycle ────────────────────────────────────────────────────────*/
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentScanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        cameraExecutor = Executors.newSingleThreadExecutor();

        if (hasCameraPermission()) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        binding = null;
    }

    /*── Permission helper ────────────────────────────────────────────────*/
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /*── CameraX セットアップ ───────────────────────────────────────────────*/
    private void startCamera() {

        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(requireContext());

        providerFuture.addListener(() -> {
            try {
                ProcessCameraProvider provider = providerFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(cameraExecutor, this::analyzeBarcode);

                provider.unbindAll();
                provider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                );

            } catch (ExecutionException | InterruptedException e) {
                Log.e("ScanFragment", "Camera init failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /*── ML Kit 解析 ───────────────────────────────────────────────────────*/
    private void analyzeBarcode(ImageProxy image) {

        if (image.getImage() == null) {
            image.close();
            return;
        }

        InputImage ii = InputImage.fromMediaImage(
                image.getImage(), image.getImageInfo().getRotationDegrees());

        BarcodeScanning.getClient()
                .process(ii)
                .addOnSuccessListener(list -> {
                    for (Barcode bc : list) {
                        String raw = bc.getRawValue();
                        tryNavigate(raw);
                    }
                })
                .addOnCompleteListener(t -> image.close());
    }

    /*── 連続スキャン抑制 & 画面遷移 ────────────────────────────────────────*/
    private void tryNavigate(String raw) {
        if (raw == null || !raw.matches("\\d{8,13}")) return;

        long now = System.currentTimeMillis();
        if (raw.equals(lastJan) && now - lastCall < 3000) return;

        lastJan  = raw;
        lastCall = now;

        NavHostFragment.findNavController(this)
                .navigate(ScanFragmentDirections.actionScanFragmentToResultFragment(raw));
    }
}
