#!/usr/bin/env python3
from socket import socket, AF_INET, SOCK_DGRAM
from logging import *
from traceback import print_exc
from struct import unpack
from enum import Enum
from car import Car
from threading import Thread, Lock
from hardware import *
from gpio import GPIO
from pwm import PWMChip
from ptz import PanTiltZoom
from time import sleep
from utils import GetNowMS
from beep import CarBeep


pwm = PWMChip.FromDevicePath(motor_ctrl)
ptz = PanTiltZoom.FromConfig(chip=pwm, cfg=ptz_servo["camera"])
car = Car(pwm=pwm, motors=motors)
light = pwm.ChannelFromConfig(peripherals["light"])
lidar_enable = GPIO.FromConfig(peripherals["lidar"]["enable"])
lidar_speed = pwm.ChannelFromConfig(peripherals["lidar"]["speed"])
beep = CarBeep()
lock = Lock()
last_report = 0

class ControlCommand(Enum):
	CMD_HELLO            = 0x01
	CMD_MOTOR_STOP       = 0x40
	CMD_MOTOR_FORWARD    = 0x41
	CMD_MOTOR_BACK       = 0x42
	CMD_MOTOR_LEFT       = 0x43
	CMD_MOTOR_RIGHT      = 0x44
	CMD_SERVO_RESET      = 0x80
	CMD_SERVO_UP         = 0x81
	CMD_SERVO_DOWN       = 0x82
	CMD_SERVO_LEFT       = 0x83
	CMD_SERVO_RIGHT      = 0x84
	CMD_LIGHT            = 0xC0
	CMD_BEEP             = 0xC1
	CMD_LIDAR_EN         = 0xD0
	CMD_LIDAR_SPEED      = 0xD1

def UpdateLastContrl():
	global last_report
	last_report = GetNowMS()

def TimeLoop():
	global last_report
	while True:
		sleep(1)
		cur = GetNowMS()
		if cur - last_report > 1000:
			last_report = cur
			debug("wait for command timeout, stop motors")
			with lock:
				car.stop()

def ProcessCommand(cmd: ControlCommand, value: int):
	with lock:
		match cmd:
			case ControlCommand.CMD_SERVO_RESET:
				debug(f"servo reset")
				ptz.ResetSlow()
			case ControlCommand.CMD_SERVO_UP:
				debug(f"servo move up")
				ptz.MoveUp()
			case ControlCommand.CMD_SERVO_DOWN:
				debug(f"servo move down")
				ptz.MoveDown()
			case ControlCommand.CMD_SERVO_LEFT:
				debug(f"servo move left")
				ptz.MoveLeft()
			case ControlCommand.CMD_SERVO_RIGHT:
				debug(f"servo move right")
				ptz.MoveRight()
			case ControlCommand.CMD_MOTOR_STOP:
				debug(f"motor stop")
				UpdateLastContrl()
				car.stop()
			case ControlCommand.CMD_MOTOR_FORWARD:
				debug(f"motor forward {value}")
				UpdateLastContrl()
				car.forward(value)
			case ControlCommand.CMD_MOTOR_BACK:
				debug(f"motor back {value}")
				UpdateLastContrl()
				car.back(value)
			case ControlCommand.CMD_MOTOR_LEFT:
				debug(f"motor left {value}")
				UpdateLastContrl()
				car.turn_left(value)
			case ControlCommand.CMD_MOTOR_RIGHT:
				debug(f"motor right {value}")
				UpdateLastContrl()
				car.turn_right(value)
			case ControlCommand.CMD_LIGHT:
				debug(f"light {value}")
				light.SetValue(value, 0xFF)
			case ControlCommand.CMD_BEEP:
				debug(f"beep")
				beep.Beep()
			case ControlCommand.CMD_LIDAR_EN:
				debug(f"lidar enable {value}")
				lidar_enable.Set(value != 0)
			case ControlCommand.CMD_LIDAR_SPEED:
				debug(f"lidar speed {value}")
				lidar_speed.SetValue(value, 0xFF)


def SocketLoop(listen: tuple[str, int]):
	s = socket(AF_INET, SOCK_DGRAM)
	s.bind(listen)
	info(f"Listen control on UDP {listen[0]}:{listen[1]}")
	buffer = b''
	while True:
		data, address = s.recvfrom(256)
		host = f"{address[0]}:{address[1]}"
		buffer += data
		while len(buffer) >= 4:
			magic, cmd, value = unpack('HBB', buffer[0:4])
			if magic != 0x9C31:
				buffer = buffer[1:]
				continue
			buffer = buffer[4:]
			debug(f"got command {cmd:x} from {host}")
			try:
				ProcessCommand(ControlCommand(cmd), value)
			except:
				print_exc()

def HardwareInit():
	light.SetPercent(0)
	lidar_enable.Set(True)
	lidar_speed.SetPercent(30)
	ptz.Reset()

def RunMain():
	basicConfig(level=INFO)
	HardwareInit()
	time_thread = Thread(target=TimeLoop)
	time_thread.start()
	SocketLoop(("0.0.0.0", 3012))

if __name__ == "__main__":
	RunMain()
