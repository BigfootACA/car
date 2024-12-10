package cn.classfun.carcontroller.ui.main;
import static org.webrtc.SessionDescription.Type.OFFER;
import static java.util.Objects.requireNonNull;
import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import org.webrtc.EglBase;
import org.webrtc.HardwareVideoDecoderFactory;
import org.webrtc.HardwareVideoEncoderFactory;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.PeerConnection.RTCConfiguration;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.PeerConnectionFactory.InitializationOptions;
import org.webrtc.PeerConnectionFactory.Options;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import cn.classfun.carcontroller.api.API;
import cn.classfun.carcontroller.api.WebRTCGetSDP;
import cn.classfun.carcontroller.api.WebRTCNew;
import lombok.Getter;
import lombok.Setter;

public class Connection{
	private final Activity act;
	private final List<Consumer<Status>>statusCallback=new ArrayList<>();
	private final SDPObserver sdpObserver=new SDPObserver(this);
	private final PeerObserver peerObserver=new PeerObserver(this);

	@Getter
	private PeerConnection peer=null;

	@Getter
	private final API api;

	@Getter
	private UUID connectionID=null;

	@Getter
	@Setter
	private SurfaceViewRenderer renderer=null;

	@Setter
	private EglBase egl=null;

	@Getter
	private Status status=Status.Disconnected;

	public Connection(Activity act){
		this.act=act;
		api=new API(act);
		setStatus(status);
	}

	private @NonNull PeerConnectionFactory createFactory(){
		final SharedPreferences sp=getPreference();
		final InitializationOptions init=InitializationOptions.builder(act)
			.setEnableInternalTracer(true)
			.createInitializationOptions();
		PeerConnectionFactory.initialize(init);
		final Options options=new Options();
		var ctx=egl.getEglBaseContext();
		VideoEncoderFactory encoder;
		VideoDecoderFactory decoder;
		if(sp.getBoolean("video_hw_encoder",true)){
			encoder=new HardwareVideoEncoderFactory(ctx,false,false);
		}else{
			encoder=new SoftwareVideoEncoderFactory();
		}
		if(sp.getBoolean("video_hw_decoder",true)){
			decoder=new HardwareVideoDecoderFactory(ctx);
		}else{
			decoder=new SoftwareVideoDecoderFactory();
		}
		return PeerConnectionFactory.builder()
			.setOptions(options)
			.setVideoEncoderFactory(encoder)
			.setVideoDecoderFactory(decoder)
			.createPeerConnectionFactory();
	}

	private @NonNull SharedPreferences getPreference(){
		return PreferenceManager.getDefaultSharedPreferences(act);
	}

	private @NonNull PeerConnection createConnection(@NonNull PeerConnectionFactory factory){
		final SharedPreferences sp=getPreference();
		final List<IceServer>ices=new ArrayList<>();
		final String ice=sp.getString("ice_server",null);
		if(ice!=null&&!ice.isEmpty())ices.add(IceServer.builder(ice).createIceServer());
		final RTCConfiguration cfg=new RTCConfiguration(ices);
		return requireNonNull(
			factory.createPeerConnection(cfg,peerObserver),
			"create peer connection failed"
		);
	}

	void setStatus(@NonNull Status status){
		this.status=status;
		statusCallback.forEach(c->c.accept(status));
	}

	public void addStatusCallback(@NonNull final Consumer<Status>cb){
		cb.accept(status);
		statusCallback.add(cb);
	}

	void onFailed(Throwable throwable){
		Log.e("connection","connection failed",throwable);
		peer=null;
		act.runOnUiThread(()->{
			new FailedDialog(act,throwable);
			setStatus(Status.Disconnected);
		});
	}

	public void connect(){
		if(status!=Status.Disconnected)return;
		final API api=new API(act);
		Consumer<WebRTCGetSDP.Response> getSdp=res->{
			res.Check();
			var v=res.getSdp();
			Log.i("webrtc","received remote sdp");
			Log.i("webrtc",v);
			var media=new MediaConstraints();
			var sdp=new SessionDescription(OFFER,v);
			peer.setRemoteDescription(sdpObserver,sdp);
			peer.createAnswer(sdpObserver,media);
		};
		Consumer<WebRTCNew.Response> newPeer=res->{
			res.Check();
			var req=new WebRTCGetSDP.Request();
			connectionID=res.getId();
			req.setId(connectionID);
			WebRTCGetSDP.Call(api,req).Enqueue(getSdp::accept,this::onFailed);
		};
		try{
			setStatus(Status.Connecting);
			var factory=createFactory();
			peer=createConnection(factory);
			WebRTCNew.Call(api).Enqueue(newPeer::accept,this::onFailed);
		}catch(Exception exc){
			onFailed(exc);
		}
	}

	public void disconnect(){
		if(status!=Status.Connected)return;
		try{
			setStatus(Status.Disconnecting);
			peer.close();
		}catch(Exception exc){
			Log.e("connection","disconnect failed",exc);
			new FailedDialog(act,exc);
			setStatus(Status.Connected);
		}
	}
}
