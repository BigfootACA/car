package cn.classfun.carcontroller.ui.main;

import static java.lang.String.format;

import android.util.Log;

import androidx.annotation.NonNull;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceConnectionState;
import org.webrtc.PeerConnection.Observer;
import org.webrtc.PeerConnection.SignalingState;

public class PeerObserver implements Observer{
	private final Connection connection;

	public PeerObserver(Connection connection){
		this.connection=connection;
	}

	public @Override void onSignalingChange(@NonNull SignalingState state){
		Log.i("webrtc","signaling changed to "+state.name());
	}

	public @Override void onIceConnectionChange(@NonNull IceConnectionState state){
		Log.i("webrtc","ice connection changed to "+state.name());
		if(state==IceConnectionState.CONNECTED&&connection.getStatus()==Status.Connecting)
			connection.setStatus(Status.Connected);
		if(state==IceConnectionState.CLOSED&&connection.getStatus()==Status.Disconnecting)
			connection.setStatus(Status.Disconnected);
		if(state==IceConnectionState.DISCONNECTED&&connection.getStatus()==Status.Connected)
			connection.disconnect();
	}

	public @Override void onIceConnectionReceivingChange(boolean b){
		Log.i("webrtc",format("ice connection receiving changed to %s",b?"true":"false"));
	}

	public @Override void onIceGatheringChange(@NonNull PeerConnection.IceGatheringState state){
		Log.i("webrtc","ice gathering changed to "+state.name());
	}

	public @Override void onIceCandidate(@NonNull IceCandidate candidate){
		Log.i("webrtc","ice candidate");
		Log.i("webrtc",candidate.sdp);
	}

	public @Override void onIceCandidatesRemoved(IceCandidate[] candidate){

	}

	public @Override void onAddStream(@NonNull MediaStream stream){
		Log.i("webrtc","add stream "+stream.getId());
		if(!stream.videoTracks.isEmpty()){
			var videoTrack=stream.videoTracks.get(0);
			videoTrack.setEnabled(true);
			videoTrack.addSink(connection.getRenderer());
		}
	}

	public @Override void onRemoveStream(@NonNull MediaStream stream){
		Log.i("webrtc","remove stream "+stream.getId());
	}

	public @Override void onDataChannel(@NonNull DataChannel ch){
		Log.i("webrtc","data channel "+ch.label());
	}

	public @Override void onRenegotiationNeeded(){
		Log.i("webrtc","renegotiation needed");
	}
}
