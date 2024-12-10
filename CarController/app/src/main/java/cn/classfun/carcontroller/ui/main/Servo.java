package cn.classfun.carcontroller.ui.main;
import static cn.classfun.carcontroller.api.CarControl.Command.CMD_SERVO_DOWN;
import static cn.classfun.carcontroller.api.CarControl.Command.CMD_SERVO_LEFT;
import static cn.classfun.carcontroller.api.CarControl.Command.CMD_SERVO_RESET;
import static cn.classfun.carcontroller.api.CarControl.Command.CMD_SERVO_RIGHT;
import static cn.classfun.carcontroller.api.CarControl.Command.CMD_SERVO_UP;
import androidx.annotation.NonNull;
import cn.classfun.carcontroller.api.CarControl;

public class Servo{
	private final CarControl car;

	public Servo(CarControl car){this.car=car;}

	public void runServoUp(){car.send(CMD_SERVO_UP);}
	public void runServoDown(){car.send(CMD_SERVO_DOWN);}
	public void runServoLeft(){car.send(CMD_SERVO_LEFT);}
	public void runServoRight(){car.send(CMD_SERVO_RIGHT);}
	public void runServoReset(){car.send(CMD_SERVO_RESET);}

	public void runServo(@NonNull Direction dir){
		switch(dir){
			case UP->runServoUp();
			case DOWN->runServoDown();
			case LEFT->runServoLeft();
			case RIGHT->runServoRight();
		}
	}

	public void runServoWith(float value,float th,@NonNull Orientation orig){
		Direction upper,lower,dir;
		switch(orig){
			case HORIZONTAL:
				upper=Direction.RIGHT;
				lower=Direction.LEFT;
				break;
			case VERTICAL:
				upper=Direction.DOWN;
				lower=Direction.UP;
				break;
			default:return;
		}
		if(value<-th)dir=lower;
		else if(value>th)dir=upper;
		else return;
		runServo(dir);
	}

	public enum Orientation{
		HORIZONTAL,
		VERTICAL,
	}

	public enum Direction{
		UP,
		DOWN,
		LEFT,
		RIGHT,
	}
}
