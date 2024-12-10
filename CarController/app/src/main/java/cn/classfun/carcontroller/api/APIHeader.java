package cn.classfun.carcontroller.api;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import cn.classfun.carcontroller.exceptions.APIException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown=true)
public abstract class APIHeader{

	@JsonProperty
	private boolean success;

	@JsonProperty
	private int code;

	@JsonProperty
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String message;

	public void Check(){
		if(!success)throw new APIException(this);
	}
}
