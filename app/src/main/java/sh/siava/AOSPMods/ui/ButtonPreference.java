package sh.siava.AOSPMods.ui;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import sh.siava.AOSPMods.ExportActivity;
import sh.siava.AOSPMods.R;
import sh.siava.AOSPMods.VibrationActivity;
import sh.siava.AOSPMods.utils.SystemUtils;

public class ButtonPreference extends Preference {

    public ButtonPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initResource();
    }

    public ButtonPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initResource();
    }

    public ButtonPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initResource();
    }

    public ButtonPreference(@NonNull Context context) {
        super(context);
        initResource();
    }

    private void initResource() {
        setLayoutResource(R.layout.custom_preference_button);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.findViewById(android.R.id.title).setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(getContext(), v, Gravity.END);
            popupMenu.inflate(R.menu.main_menu);
            popupMenu.setOnMenuItemClickListener(item -> {
                int itemID = item.getItemId();
                if (itemID == R.id.menu_vibration) {
                    getContext().startActivity(new Intent(getContext(), VibrationActivity.class));
                } else if (itemID == R.id.menu_exportPrefs) {
                    importExportSettings(true);
                } else if (itemID == R.id.menu_importPrefs) {
                    importExportSettings(false);
                } else if (itemID == R.id.menu_restart) {
                    SystemUtils.Restart();
                } else if (itemID == R.id.menu_restartSysUI) {
                    SystemUtils.RestartSystemUI();
                } else if (itemID == R.id.menu_restartLauncher) {
                    SystemUtils.RestartLauncher();
                }
                return true;
            });
            popupMenu.show();
        });
    }

    private void importExportSettings(boolean isExport) {
        Intent intent = new Intent(getContext(), ExportActivity.class);
        intent.putExtra("isExport", isExport);
        getContext().startActivity(intent);
    }
}
