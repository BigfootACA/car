package cn.classfun.carcontroller.api;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class Simple{

	@JsonIgnoreProperties(ignoreUnknown=true)
	public static class Request{
	}

	@JsonIgnoreProperties(ignoreUnknown=true)
	public static class Response extends APIHeader{
	}
}
