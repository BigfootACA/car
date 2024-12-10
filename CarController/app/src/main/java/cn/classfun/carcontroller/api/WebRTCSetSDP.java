package cn.classfun.carcontroller.api;
import androidx.annotation.NonNull;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

public class WebRTCSetSDP{
	@Getter
	@Setter
	@JsonIgnoreProperties(ignoreUnknown=true)
	public static class Request{
		@JsonProperty
		private UUID id;
		@JsonProperty
		private String sdp;
	}
	public static @NonNull API.Call<Request,Simple.Response>Call(
		@NonNull API api,@NonNull Request req
	){
		return api.Call(req,Simple.Response.class,"/api/webrtc/set_sdp");
	}
}
