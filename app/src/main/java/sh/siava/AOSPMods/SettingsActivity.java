package sh.siava.AOSPMods;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.topjohnwu.superuser.Shell;

public class SettingsActivity extends AppCompatActivity {

    Context DPContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DPContext = this.createDeviceProtectedStorageContext();
        DPContext.moveSharedPreferencesFrom(this, BuildConfig.APPLICATION_ID + "_preferences");
        super.onCreate(savedInstanceState);
        try {
            Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER)); // access full filesystem
        } catch (Exception ignored) {
        }
        setContentView(R.layout.settings_activity);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.settings, new AospModsFragment()).commit();
    }

    @SuppressWarnings("ConstantConditions")
    public static class AospModsFragment extends PreferenceFragmentCompat {

        private FrameLayout pullDownIndicator;
        SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> updateVisibility(sharedPreferences);

        @SuppressLint("RtlHardcoded")
        private void updateVisibility(SharedPreferences sharedPreferences) {
            try {
                boolean QSPulldownEnabled = sharedPreferences.getBoolean("QSPulldownEnabled", false);
                int displayWidth = getActivity().getWindowManager().getCurrentWindowMetrics().getBounds().width();
                findPreference("QSPulldownPercent").setVisible(QSPulldownEnabled);
                findPreference("QSPulldownSide").setVisible(QSPulldownEnabled);
                findPreference("QSPulldownPercent").setSummary(sharedPreferences.getInt("QSPulldownPercent", 50) + "%");
                pullDownIndicator.setVisibility(findPreference("QSPulldownPercent").isVisible() ? View.VISIBLE : View.GONE);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) pullDownIndicator.getLayoutParams();
                lp.width = Math.round(sharedPreferences.getInt("QSPulldownPercent", 50) * displayWidth / 100f);
                lp.gravity = Gravity.TOP | (Integer.parseInt(sharedPreferences.getString("QSPulldownSide", "1")) == 1 ? Gravity.RIGHT : Gravity.LEFT);
                pullDownIndicator.setLayoutParams(lp);
            } catch (Exception ignored) {
            }
        }

        @Override
        public void onDestroy() {
            ((ViewGroup) pullDownIndicator.getParent()).removeView(pullDownIndicator);
            super.onDestroy();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            createPullDownIndicator();
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.aosp_mods_prefs, rootKey);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
            updateVisibility(prefs);
            prefs.registerOnSharedPreferenceChangeListener(listener);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            View recyclerView = ((ViewGroup) ((ViewGroup) view).getChildAt(0)).getChildAt(0);
            ViewCompat.setOnApplyWindowInsetsListener(recyclerView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(
                        systemBars.left,
                        systemBars.top + 100,
                        systemBars.right,
                        systemBars.bottom + 100
                );
                return insets;
            });
        }

        private void createPullDownIndicator() {
            pullDownIndicator = new FrameLayout(getContext());
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(0, 25);
            lp.gravity = Gravity.TOP;
            pullDownIndicator.setLayoutParams(lp);
            pullDownIndicator.setBackgroundColor(getContext().getColor(android.R.color.system_accent1_200));
            pullDownIndicator.setAlpha(.7f);
            pullDownIndicator.setVisibility(View.VISIBLE);
            ((ViewGroup) getActivity().getWindow().getDecorView().getRootView()).addView(pullDownIndicator);
        }
    }
}
