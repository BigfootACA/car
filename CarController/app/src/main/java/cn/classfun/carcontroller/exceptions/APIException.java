package cn.classfun.carcontroller.exceptions;
import static java.lang.String.format;
import androidx.annotation.NonNull;
import cn.classfun.carcontroller.api.APIHeader;

public class APIException extends RuntimeException{
	private final APIHeader head;
	public APIException(@NonNull APIHeader head){
		super(format("API Request Failed: %s",head.getMessage()));
		this.head=head;
	}

	public int getCode(){
		return head.getCode();
	}

	public String getReason(){
		return head.getMessage();
	}
}
