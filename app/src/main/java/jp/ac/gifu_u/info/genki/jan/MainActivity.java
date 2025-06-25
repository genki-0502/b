package jp.ac.gifu_u.info.genki.jan;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * NavHost だけを表示する殻の Activity
 * ( activity_main.xml に <FragmentContainerView … nav_graph> を置く )
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);   // NavHost しか無いレイアウト
    }
}
