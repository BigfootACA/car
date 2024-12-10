package cn.classfun.carcontroller.ui.settings;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import cn.classfun.carcontroller.R;
public class SettingsActivity extends AppCompatActivity{
	@Override
	protected void onCreate(Bundle saved){
		super.onCreate(saved);
		setContentView(R.layout.activity_settings);
		if(saved==null)getSupportFragmentManager()
			.beginTransaction()
			.replace(R.id.settings,new SettingsFragment())
			.commit();
		final ActionBar bar=getSupportActionBar();
		if(bar!=null)bar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if(item.getItemId()==android.R.id.home){
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
