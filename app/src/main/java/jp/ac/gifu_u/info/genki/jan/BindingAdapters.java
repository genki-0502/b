// BindingAdapters.java
package jp.ac.gifu_u.info.genki.jan;

import android.widget.ImageView;
import androidx.databinding.BindingAdapter;
import com.bumptech.glide.Glide;

public class BindingAdapters {

    @BindingAdapter("imageUrl")
    public static void loadFromUrl(ImageView view, String url) {
        if (url != null && !url.isEmpty()) {
            Glide.with(view.getContext())
                    .load(url)
                    .into(view);
        }
    }
}
