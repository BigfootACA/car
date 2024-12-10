package cn.classfun.carcontroller.ui.main;
import static android.view.InputDevice.SOURCE_JOYSTICK;
import static java.lang.String.format;
import static cn.classfun.carcontroller.ui.main.GamepadHandler.GamepadsHandler;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import java.net.SocketException;
import java.net.UnknownHostException;
import cn.classfun.carcontroller.R;
import cn.classfun.carcontroller.api.CarControl;
import cn.classfun.carcontroller.exceptions.SettingsException;
import cn.classfun.carcontroller.lib.Pool;

public class MainActivity extends AppCompatActivity{
	private Connection conn;
	private MainMenu mainMenu;
	private GamepadHandler gamepadLeft;
	private GamepadHandler gamepadRight;
	private GamepadsHandler gamepads;
	private CarControl car=null;
	private Servo servo=null;
	private Motor motor=null;
	private int speed=0,speed_min=0,speed_max=100;
	private int light_level=0;
	private boolean beep_press=false;

	@Override
	public void onCreate(@Nullable Bundle saved){
		super.onCreate(saved);
		initWindow();
		initComponents();
		initRenderer();
		initSettings();
		initGamepad();
	}

	@Override
	public void onResume(){
		super.onResume();
		initSettings();
	}

	private void initComponents(){
		conn=new Connection(this);
		conn.addStatusCallback(this::onStatusChanged);
		mainMenu=new MainMenu(this);
	}

	private void initRenderer(){
		var egl=EglBase.create();
		conn.setEgl(egl);
		var renderer=(SurfaceViewRenderer)findViewById(R.id.video);
		renderer.init(egl.getEglBaseContext(),null);
		renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
		conn.setRenderer(renderer);
	}

	private void initWindow(){
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(
			LayoutParams.FLAG_FULLSCREEN|
				LayoutParams.FLAG_KEEP_SCREEN_ON
		);
		getWindow().getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
				View.SYSTEM_UI_FLAG_FULLSCREEN|
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
		);
		setContentView(R.layout.activity_main);
	}

	private void initSettings(){
		var sp=PreferenceManager.getDefaultSharedPreferences(this);
		speed_max=Integer.parseInt(sp.getString("speed_max",String.valueOf(speed_max)));
		speed_min=Integer.parseInt(sp.getString("speed_min",String.valueOf(speed_min)));
		if(speed_min>speed_max)speed_min=speed_max;
		speed=speed_min;
	}

	private void initGamepad(){
		gamepads=new GamepadsHandler();
		gamepads.addOnChanged(this::onGamepadChanged);

		gamepadLeft=new GamepadHandler();
		gamepadLeft.setDebounce(100);
		gamepadLeft.setPeriod(100);
		gamepadLeft.addOnTimer(this::onMotorTimer);
		gamepads.add(0,gamepadLeft);

		gamepadRight=new GamepadHandler();
		gamepadRight.setDebounce(100);
		gamepadRight.setPeriod(50);
		gamepadRight.addOnTimer(this::onServoGamepadTimer);
		gamepads.add(1,gamepadRight);

		Pool.schedule(this::onBeepTimer,100);
	}

	public @NonNull Connection getConnection(){
		return conn;
	}

	private void connectCar()throws UnknownHostException,SocketException{
		var sp=PreferenceManager.getDefaultSharedPreferences(this);
		var host=sp.getString("server_host",null);
		if(host==null||host.isEmpty())
			throw new SettingsException("No server host specified");
		var port=Integer.parseInt(sp.getString("server_control_port","3012"));
		if(port<=0||port>=65536)
			throw new SettingsException("Bad car control port specified");
		Log.i("car",format("use %s:%d",host,port));
		car=new CarControl(host,port);
		servo=new Servo(car);
		motor=new Motor(car);
	}

	private void onStatusChanged(@NonNull Status status){
		switch(status){
			case Connecting->{
				try{
					connectCar();
				}catch(Exception e){
					Log.e("car","connect failed",e);
				}
			}
			case Disconnected->{
				car=null;
				servo=null;
				motor=null;
			}
		}
	}

	@Override
	public boolean onGenericMotionEvent(@NonNull MotionEvent event){
		if(
			((event.getSource()&SOURCE_JOYSTICK)==SOURCE_JOYSTICK)&&
			(event.getAction()==MotionEvent.ACTION_MOVE)
		){
			float lx=event.getAxisValue(MotionEvent.AXIS_X);
			float ly=event.getAxisValue(MotionEvent.AXIS_Y);
			gamepadLeft.set(lx,ly);
			float rx=event.getAxisValue(MotionEvent.AXIS_Z);
			float ry=event.getAxisValue(MotionEvent.AXIS_RZ);
			gamepadRight.set(rx,ry);
			float br=event.getAxisValue(MotionEvent.AXIS_BRAKE);
			float f_speed=(br*(float)(speed_max-speed_min))+(float)speed_min;
			speed=Math.min(Math.max((int)f_speed,speed_min),speed_max);
			return true;
		}
		return super.onGenericMotionEvent(event);
	}

	@Override
	public boolean onKeyUp(int keyCode,KeyEvent event){
		if(conn.getStatus()!=Status.Connected||car==null)
			return super.onKeyUp(keyCode,event);
		Log.i("gamepad",format("key %s up",keyCode));
		return switch(keyCode){
			case KeyEvent.KEYCODE_BUTTON_A->{
				light_level+=0x10;
				light_level&=0xFF;
				car.send(CarControl.Command.CMD_LIGHT,light_level);
				yield true;
			}
			case KeyEvent.KEYCODE_BUTTON_B->{
				beep_press=false;
				yield true;
			}
			case
				KeyEvent.KEYCODE_BUTTON_X,
				KeyEvent.KEYCODE_BUTTON_Y->
				true;
			case KeyEvent.KEYCODE_BUTTON_THUMBL->{
				motor.runMotorStop();
				yield true;
			}
			case KeyEvent.KEYCODE_BUTTON_THUMBR->{
				servo.runServoReset();
				yield true;
			}
			default->super.onKeyUp(keyCode,event);
		};
	}

	@Override
	public boolean onKeyDown(int keyCode,KeyEvent event){
		if(conn.getStatus()!=Status.Connected||car==null)
			return super.onKeyDown(keyCode,event);
		return switch(keyCode){
			case KeyEvent.KEYCODE_BUTTON_B->{
				beep_press=true;
				yield true;
			}
			case
				KeyEvent.KEYCODE_BUTTON_A,
				KeyEvent.KEYCODE_BUTTON_X,
				KeyEvent.KEYCODE_BUTTON_Y,
				KeyEvent.KEYCODE_BUTTON_THUMBL,
				KeyEvent.KEYCODE_BUTTON_THUMBR->true;
			default->super.onKeyDown(keyCode,event);
		};
	}

	private void onBeepTimer(){
		if(!beep_press||car==null)return;
		car.send(CarControl.Command.CMD_BEEP);
	}

	private void onGamepadChanged(int id,float x,float y){
		Log.v("gamepad",format("id %d x %.4f y %.4f",id,x,y));
	}

	private void onMotorTimer(float x,float y,boolean changed){
		if(conn.getStatus()!=Status.Connected||motor==null)return;
		var hx=motor.runMotorWith(x,0.5f,Motor.Orientation.HORIZONTAL,speed);
		var vx=motor.runMotorWith(y,0.5f,Motor.Orientation.VERTICAL,speed);
		if(!hx&&!vx)motor.runMotorStop();
	}

	private void onServoGamepadTimer(float x,float y,boolean changed){
		if(conn.getStatus()!=Status.Connected||servo==null)return;
		servo.runServoWith(x,0.5f,Servo.Orientation.HORIZONTAL);
		servo.runServoWith(y,0.5f,Servo.Orientation.VERTICAL);
	}
}
