package cn.classfun.carcontroller.api;
import androidx.annotation.NonNull;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

public class WebRTCNew extends APIHeader{
	@Getter
	@Setter
	@JsonIgnoreProperties(ignoreUnknown=true)
	public static class Response extends APIHeader{
		@JsonProperty
		private UUID id;
	}
	public static @NonNull API.Call<Simple.Request,Response>Call(@NonNull API api){
		return api.Call(new Simple.Request(),Response.class,"/api/webrtc/new");
	}
}
