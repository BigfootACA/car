package cn.classfun.carcontroller.api;
import static java.lang.String.format;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import cn.classfun.carcontroller.exceptions.APIException;
import cn.classfun.carcontroller.exceptions.SettingsException;
import cn.classfun.carcontroller.lib.Pool;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

@SuppressWarnings({"UnusedReturnValue","unused"})
public class API{
	public static final OkHttpClient client=new OkHttpClient();
	private final Context ctx;

	public API(@NonNull Context ctx){this.ctx=ctx;}

	private @NonNull SharedPreferences getPreference(){
		return PreferenceManager.getDefaultSharedPreferences(ctx);
	}

	@SuppressLint("DefaultLocale")
	private @NonNull String getURL(String api){
		var sp=getPreference();
		var host=sp.getString("server_host",null);
		if(host==null||host.isEmpty())
			throw new SettingsException("No server host specified");
		if(host.contains(":"))host=format("[%s]",host);
		var port=Integer.parseInt(sp.getString("server_webrtc_port","2345"));
		if(port<=0||port>=65536)
			throw new SettingsException("Bad webrtc server port specified");
		var path=sp.getString("server_webrtc_path","/");
		var https=sp.getBoolean("server_webrtc_https",false);
		var url=format("%s://%s:%d",https?"https":"http",host,port);
		if(!path.startsWith("/"))url+="/";
		url+=path;
		if(!url.endsWith("/")&&!api.startsWith("/"))
			url+="/";
		if(url.endsWith("/")&&api.startsWith("/"))
			url=url.substring(0,url.length()-1);
		url+=api;
		return url;
	}

	public <I,O extends APIHeader> @NonNull Call<I,O> Call(
		@NonNull I req,
		@NonNull Class<O>cls,
		@NonNull String path
	){
		return new Call<>(req,cls,path);
	}

	@RequiredArgsConstructor
	public class Call<I,O extends APIHeader> implements Runnable{
		private final static ObjectMapper mapper=new ObjectMapper();
		private final static MediaType jsonType=MediaType.get("application/json");
		private final List<OnFailedAction> onFailed=new ArrayList<>();
		private final List<OnSuccessAction<O>>onSuccess=new ArrayList<>();
		private final List<OnResponseAction<O>>onResponse=new ArrayList<>();

		@NonNull
		private final I request;
		@NonNull
		private final Class<O>output;
		@NonNull
		private final String path;

		@Override
		public void run(){
			try{
				O result;
				var req=new Request.Builder()
					.url(getURL(path))
					.post(RequestBody.create(jsonType,mapper.writeValueAsString(request)))
					.build();
				Log.i("api",format("send request to %s",req.url()));
				try(var res=client.newCall(req).execute()){
					var body=Objects.requireNonNull(res.body());
					try(var ios=body.byteStream()){
						result=mapper.readValue(ios,output);
					}
				}
				for(var action:onResponse)action.OnResponse(result);
				Log.i("api",format("response code %d",result.getCode()));
				if(!result.isSuccess())throw new APIException(result);
				for(var action:onSuccess)action.OnSuccess(result);
			}catch(Throwable e){
				if(onFailed.isEmpty())throw new RuntimeException(e);
				for(var action:onFailed)action.OnFailed(e);
			}
		}

		public void start(){
			Pool.execute(this);
		}

		public Call<I,O>OnFailed(OnFailedAction action){
			onFailed.add(action);
			return this;
		}

		public Call<I,O>OnSuccess(OnSuccessAction<O> action){
			onSuccess.add(action);
			return this;
		}

		public Call<I,O>OnResponse(OnResponseAction<O> action){
			onResponse.add(action);
			return this;
		}

		public Call<I,O>Enqueue(OnSuccessAction<O>success,OnFailedAction failed){
			onSuccess.add(success);
			onFailed.add(failed);
			start();
			return this;
		}

		public interface OnFailedAction{
			void OnFailed(Throwable ex);
		}

		public interface OnSuccessAction<O>{
			void OnSuccess(O payload)throws Throwable;
		}

		public interface OnResponseAction<O>{
			void OnResponse(O result)throws Throwable;
		}
	}
}
