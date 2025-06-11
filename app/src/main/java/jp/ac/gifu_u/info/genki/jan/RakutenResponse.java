package jp.ac.gifu_u.info.genki.jan;

import java.util.List;

public class RakutenResponse {
    public List<ItemWrapper> Items;

    public static class ItemWrapper {
        public Item Item;
    }

    public static class Item {
        public String itemName;
        public String itemUrl;
        public int itemPrice;
    }
}

