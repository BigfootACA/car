package cn.classfun.carcontroller.ui.main;

import android.util.Log;

import androidx.annotation.NonNull;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import cn.classfun.carcontroller.api.APIHeader;
import cn.classfun.carcontroller.api.WebRTCSetSDP;
import cn.classfun.carcontroller.lib.Pool;

public class SDPObserver implements SdpObserver{
	private final Connection connection;

	public SDPObserver(Connection connection){
		this.connection=connection;
	}

	public @Override void onCreateSuccess(@NonNull SessionDescription sdp){
		Log.i("webrtc","sdp create success");
		var peer=connection.getPeer();
		Pool.execute(()->peer.setLocalDescription(this,sdp));
	}

	public @Override void onSetSuccess(){
		Log.i("webrtc","sdp set success");
		Pool.execute(()->{
			var peer=connection.getPeer();
			var sdp=peer.getLocalDescription();
			if(sdp!=null){
				var req=new WebRTCSetSDP.Request();
				req.setId(connection.getConnectionID());
				req.setSdp(sdp.description);
				WebRTCSetSDP.Call(connection.getApi(),req).Enqueue(
					APIHeader::Check,
					connection::onFailed
				);
			}
		});
	}

	public @Override void onCreateFailure(String s){
		Log.e("webrtc","sdp create failure: "+s);
	}

	public @Override void onSetFailure(String s){
		Log.e("webrtc","sdp set failure: "+s);
	}
}
