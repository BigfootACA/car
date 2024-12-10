package cn.classfun.carcontroller.ui.main;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import cn.classfun.carcontroller.R;
import cn.classfun.carcontroller.ui.settings.SettingsActivity;

public class MainMenu{
	private final MainActivity main;
	private final Connection conn;
	private final PopupMenu popup;
	private final MenuItem menu_connect;
	private final MenuItem menu_disconnect;

	public MainMenu(@NonNull MainActivity main){
		this.main=main;
		this.conn=main.getConnection();
		FloatingActionButton fab=main.findViewById(R.id.button_menu);
		popup=new PopupMenu(main,fab);
		Menu menu=popup.getMenu();
		popup.getMenuInflater().inflate(R.menu.main_menu,menu);
		popup.setOnMenuItemClickListener(this::onMenuItemClick);
		fab.setOnClickListener(v->popup.show());
		menu_connect=menu.findItem(R.id.menu_connect);
		menu_disconnect=menu.findItem(R.id.menu_disconnect);
		conn.addStatusCallback(this::onStatusChanged);
	}

	private void onStatusChanged(@NonNull Status status){
		main.runOnUiThread(()->{
			menu_connect.setEnabled(false);
			menu_connect.setVisible(false);
			menu_disconnect.setEnabled(false);
			menu_disconnect.setVisible(false);
			switch(status){
				case Connected:
					menu_disconnect.setEnabled(true);
				case Disconnecting:
					menu_disconnect.setVisible(true);
				break;
				case Disconnected:
					menu_connect.setEnabled(true);
				case Connecting:
					menu_connect.setVisible(true);
				break;
			}
		});
	}

	private boolean onMenuItemClick(@NonNull MenuItem item){
		int itemId=item.getItemId();
		if(itemId==R.id.menu_connect){
			conn.connect();
			return true;
		}else if(itemId==R.id.menu_disconnect){
			conn.disconnect();
			return true;
		}else if(itemId==R.id.menu_settings){
			final Intent it=new Intent(main,SettingsActivity.class);
			main.startActivity(it);
			return true;
		}
		return false;
	}
}
