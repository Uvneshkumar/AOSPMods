package sh.siava.AOSPMods;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.topjohnwu.superuser.Shell;

import java.util.Objects;

import sh.siava.AOSPMods.utils.SystemUtils;

public class SettingsActivity extends AppCompatActivity {

    Context DPContext;

    public void backButtonDisabled() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DPContext = this.createDeviceProtectedStorageContext();
        DPContext.moveSharedPreferencesFrom(this, BuildConfig.APPLICATION_ID + "_preferences");
        super.onCreate(savedInstanceState);
        backButtonDisabled();
        try {
            Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER)); // access full filesystem
        } catch (Exception ignored) {
        }
        setContentView(R.layout.settings_activity);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.settings, new AospModsFragment()).commit();
        Objects.requireNonNull(getSupportActionBar()).setBackgroundDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.color_surface_overlay));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemID = item.getItemId();
        if (itemID == android.R.id.home) {
            onBackPressed();
        } else if (itemID == R.id.menu_restart) {
            SystemUtils.Restart();
        } else if (itemID == R.id.menu_restartSysUI) {
            SystemUtils.RestartSystemUI();
        } else if (itemID == R.id.menu_restartLauncher) {
            SystemUtils.RestartLauncher();
        } else if (itemID == R.id.menu_vibration) {
            startActivity(new Intent(this, VibrationActivity.class));
        }
        return true;
    }

    @SuppressWarnings("ConstantConditions")
    public static class AospModsFragment extends PreferenceFragmentCompat {

        private FrameLayout pullDownIndicator;
        SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> updateVisibililty(sharedPreferences);

        @SuppressLint("RtlHardcoded")
        private void updateVisibililty(SharedPreferences sharedPreferences) {
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
            updateVisibililty(prefs);
            prefs.registerOnSharedPreferenceChangeListener(listener);
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

        @Override
        public void onResume() {
            super.onResume();
            requireActivity().setTitle(requireActivity().getResources().getString(R.string.app_name));
        }
    }
}
