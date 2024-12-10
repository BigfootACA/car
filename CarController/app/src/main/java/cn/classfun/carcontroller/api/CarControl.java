package cn.classfun.carcontroller.api;
import android.util.Log;
import androidx.annotation.NonNull;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import cn.classfun.carcontroller.lib.Pool;

@SuppressWarnings("unused")
public class CarControl{
	private final InetAddress address;
	private final int port;
	private final DatagramSocket socket;

	public CarControl(InetAddress address,int port)throws SocketException{
		this.address=address;
		this.port=port;
		this.socket=new DatagramSocket();
	}

	public CarControl(String address,int port)throws SocketException,UnknownHostException{
		this(InetAddress.getByName(address),port);
	}

	public void send(@NonNull Command cmd,int value){
		var data=new byte[4];
		data[0]=(byte)0x31;
		data[1]=(byte)0x9C;
		data[2]=(byte)cmd.value;
		data[3]=(byte)value;
		var p=new DatagramPacket(data,data.length,address,port);
		Pool.execute(()->{
			try{
				socket.send(p);
			}catch(IOException e){
				Log.e("car","send command failed",e);
			}
		});
	}
	public void send(@NonNull Command cmd){send(cmd,0);}

	public enum Command{
		CMD_HELLO            (0x01),
		CMD_MOTOR_STOP       (0x40),
		CMD_MOTOR_FORWARD    (0x41),
		CMD_MOTOR_BACK       (0x42),
		CMD_MOTOR_LEFT       (0x43),
		CMD_MOTOR_RIGHT      (0x44),
		CMD_SERVO_RESET      (0x80),
		CMD_SERVO_UP         (0x81),
		CMD_SERVO_DOWN       (0x82),
		CMD_SERVO_LEFT       (0x83),
		CMD_SERVO_RIGHT      (0x84),
		CMD_LIGHT            (0xC0),
		CMD_BEEP             (0xC1),
		CMD_LIDAR_EN         (0xD0),
		CMD_LIDAR_SPEED      (0xD1),
		CMD_MAX              (0xFF);

		public final int value;
		Command(int value){
			this.value=value;
		}
	}
}
