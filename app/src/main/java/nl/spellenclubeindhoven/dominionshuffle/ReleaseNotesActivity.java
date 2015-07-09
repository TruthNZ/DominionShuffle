package nl.spellenclubeindhoven.dominionshuffle;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * Created by gynn.rickerby on 7/07/2015.
 */
public class ReleaseNotesActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.releasenotes);
    }

    public void close(View view) {
        finish();
    }
}
