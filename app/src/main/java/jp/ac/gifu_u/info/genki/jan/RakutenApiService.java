package jp.ac.gifu_u.info.genki.jan;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RakutenApiService {
    @GET("services/api/IchibaItem/Search/20170706")
    Call<RakutenResponse> searchItem(
            @Query("applicationId") String appId,
            @Query("keyword") String keyword,   // ← 変更点
            @Query("format") String format,
            @Query("hits") int hits
    );
}


