package jp.ac.gifu_u.info.genki.jan;

import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;           // ← 追加
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;          // ← 追加

import jp.ac.gifu_u.info.genki.jan.databinding.FragmentResultBinding;

public class ResultFragment extends Fragment {

    private FragmentResultBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentResultBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 引数で受け取った JAN
        String jan = ResultFragmentArgs.fromBundle(getArguments()).getJanCode();

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).popBackStack());

        fetchRakutenInfo(jan);
    }

    /** 楽天 API 呼び出し */
    private void fetchRakutenInfo(String janCode) {
        binding.progress.setVisibility(View.VISIBLE);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://app.rakuten.co.jp/")
                .addConverterFactory(GsonConverterFactory.create())   // ← ここ OK
                .build();

        RakutenApiService service = retrofit.create(RakutenApiService.class);

        Call<RakutenResponse> call = service.searchItem(
                "1042527241521827202",     // ← アプリ ID
                janCode,
                "json",
                1
        );

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<RakutenResponse> c,
                                   @NonNull Response<RakutenResponse> res) {
                binding.progress.setVisibility(View.GONE);

                if (res.isSuccessful()
                        && res.body() != null
                        && res.body().getItems() != null
                        && !res.body().getItems().isEmpty()) {

                    // ItemWrapper → Item
                    RakutenResponse.Item item =
                            res.body().getItems().get(0).getItem();

                    // DataBinding で画面に反映
                    binding.setItem(item);
                    binding.executePendingBindings();

                    // 楽天サイトを開く
                    binding.btnOpenWeb.setOnClickListener(v -> {
                        Uri uri = Uri.parse(item.getItemUrl());
                        startActivity(
                                new android.content.Intent(
                                        android.content.Intent.ACTION_VIEW, uri));
                    });

                } else {
                    showError("商品が見つかりません");
                }
            }

            @Override
            public void onFailure(@NonNull Call<RakutenResponse> c,
                                  @NonNull Throwable t) {
                binding.progress.setVisibility(View.GONE);
                showError("通信エラー: " + t.getMessage());
            }
        });
    }

    /** Snackbar でエラー表示 */
    private void showError(String msg) {
        Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
    }
}
