package com.example.carromaim;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    private Button start;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(36, 48, 36, 36);
        box.setBackgroundColor(Color.rgb(16,19,24));

        TextView title = new TextView(this);
        title.setText("Carrom Aim Assistant");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setPadding(0,0,0,18);
        box.addView(title);

        TextView info = new TextView(this);
        info.setText("Practice-focused overlay with an adjustable aim line, power, and cushion prediction. It does not inject taps or play the game automatically.");
        info.setTextColor(Color.LTGRAY);
        info.setTextSize(16);
        box.addView(info);

        start = new Button(this);
        start.setText("Enable Aim Overlay");
        start.setOnClickListener(v -> launchOverlay());
        box.addView(start);

        setContentView(box);
    }

    private void launchOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
            Toast.makeText(this, "Allow display over other apps, then press the button again.", Toast.LENGTH_LONG).show();
            return;
        }
        startService(new Intent(this, AimOverlayService.class));
        Toast.makeText(this, "Aim overlay started.", Toast.LENGTH_SHORT).show();
    }

    @Override protected void onResume() {
        super.onResume();
        if (Settings.canDrawOverlays(this)) start.setText("Start / Restart Aim Overlay");
    }
}
