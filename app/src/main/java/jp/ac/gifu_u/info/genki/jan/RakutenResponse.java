// RakutenResponse.java
package jp.ac.gifu_u.info.genki.jan;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class RakutenResponse {

    @SerializedName("Items")
    private List<ItemWrapper> items;

    public List<ItemWrapper> getItems() {
        return items;
    }

    public static class ItemWrapper {

        @SerializedName("Item")
        private Item item;

        public Item getItem() {
            return item;
        }
    }

    public static class Item {

        @SerializedName("itemName")
        private String itemName;

        @SerializedName("itemPrice")
        private int itemPrice;

        @SerializedName("itemUrl")
        private String itemUrl;

        @SerializedName("mediumImageUrls")
        private List<MediumImageUrl> mediumImageUrls;

        /* ★★ ここから Getter を追加 ★★ */

        public String getItemName() {
            return itemName;
        }

        public int getItemPrice() {
            return itemPrice;
        }

        public String getItemUrl() {
            return itemUrl;
        }

        public List<MediumImageUrl> getMediumImageUrls() {   // ← これが無かった
            return mediumImageUrls;
        }
    }


    public static class MediumImageUrl {

        @SerializedName("imageUrl")
        private String imageUrl;

        public String getImageUrl() {
            return imageUrl;
        }
    }
}
