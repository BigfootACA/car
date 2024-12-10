package cn.classfun.carcontroller.ui.settings;
import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import cn.classfun.carcontroller.R;
public class SettingsFragment extends PreferenceFragmentCompat{
	@Override
	public void onCreatePreferences(Bundle savedInstanceState,String rootKey){
		setPreferencesFromResource(R.xml.root_preferences,rootKey);
	}
}
