package cn.classfun.carcontroller.ui.main;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import cn.classfun.carcontroller.R;
import cn.classfun.carcontroller.exceptions.SettingsException;
import cn.classfun.carcontroller.ui.settings.SettingsActivity;
import lombok.Getter;

@Getter
public class FailedDialog{
	private final AlertDialog dialog;

	public FailedDialog(@NonNull Context ctx,@NonNull Throwable th){
		AlertDialog.Builder ab=new AlertDialog.Builder(ctx);
		ab.setTitle(R.string.connection_failed);
		ab.setMessage(th.getLocalizedMessage());
		ab.setNegativeButton(android.R.string.cancel,(d,w)->d.dismiss());
		if(th instanceof SettingsException)ab.setPositiveButton(R.string.settings,(d,w)->{
			final Intent it=new Intent(ctx,SettingsActivity.class);
			ctx.startActivity(it);
		});
		dialog=ab.create();
		dialog.show();
	}
}
