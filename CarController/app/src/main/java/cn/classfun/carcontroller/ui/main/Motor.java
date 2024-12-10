package cn.classfun.carcontroller.ui.main;

import static cn.classfun.carcontroller.api.CarControl.Command.CMD_MOTOR_BACK;
import static cn.classfun.carcontroller.api.CarControl.Command.CMD_MOTOR_FORWARD;
import static cn.classfun.carcontroller.api.CarControl.Command.CMD_MOTOR_LEFT;
import static cn.classfun.carcontroller.api.CarControl.Command.CMD_MOTOR_RIGHT;
import static cn.classfun.carcontroller.api.CarControl.Command.CMD_MOTOR_STOP;

import androidx.annotation.NonNull;

import cn.classfun.carcontroller.api.CarControl;

public class Motor{
	private final CarControl car;

	public Motor(CarControl car){this.car=car;}

	public void runMotorForward(int speed){car.send(CMD_MOTOR_FORWARD,speed);}
	public void runMotorBack(int speed){car.send(CMD_MOTOR_BACK,speed);}
	public void runMotorLeft(int speed){car.send(CMD_MOTOR_LEFT,speed);}
	public void runMotorRight(int speed){car.send(CMD_MOTOR_RIGHT,speed);}
	public void runMotorStop(){car.send(CMD_MOTOR_STOP);}

	public void runMotor(@NonNull Direction dir,int speed){
		switch(dir){
			case STOP->runMotorStop(        );
			case FORWARD->runMotorForward(speed);
			case BACK->runMotorBack(speed);
			case LEFT->runMotorLeft(speed);
			case RIGHT->runMotorRight(speed);
		}
	}

	public boolean runMotorWith(float value,float th,@NonNull Orientation orig,int speed){
		Direction upper,lower,dir;
		switch(orig){
			case HORIZONTAL:
				upper=Direction.RIGHT;
				lower=Direction.LEFT;
				break;
			case VERTICAL:
				upper=Direction.BACK;
				lower=Direction.FORWARD;
				break;
			default:return false;
		}
		if(value<-th)dir=lower;
		else if(value>th)dir=upper;
		else return false;
		runMotor(dir,speed);
		return true;
	}

	public enum Orientation{
		HORIZONTAL,
		VERTICAL,
	}

	public enum Direction{
		STOP,
		FORWARD,
		BACK,
		LEFT,
		RIGHT,
	}
}
