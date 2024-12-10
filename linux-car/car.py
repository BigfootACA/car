from enum import Enum
from utils import *
from pwm import *


class Wheel(Enum):
	FRONT_LEFT  = 0
	FRONT_RIGHT = 1
	BACK_LEFT   = 2
	BACK_RIGHT  = 3


class Direction(Enum):
	FORWARD = 0
	BACK = 1
	ALL = 2


class Car:
	def __init__(self, pwm: PWMChip, motors: dict) -> None:
		self._pwm = pwm
		self._motors = motors
		max_chan = max(self._motors[m]["channel"] for m in self._motors)
		self._chan = [-1] * (max_chan + 1)
		for motor in self._motors:
			cfg = self._motors[motor]
			chan = cfg["channel"]
			self._chan[chan] = pwm.ChannelFromConfig(cfg)

	def set_speed(self, ch: int, percent: int) -> None:
		if percent > 100 or percent < 0:
			raise ValueError("bad percent")
		pwm: PWM = self._chan[ch]
		pwm.SetPercent(percent)

	def get_wheel(self, wheel: Wheel) -> tuple[int, int]:
		if wheel == Wheel.FRONT_LEFT:
			return (
				self._motors["FRONT_LEFT_FORWARD"]["channel"],
				self._motors["FRONT_LEFT_BACK"]["channel"],
			)
		elif wheel == Wheel.FRONT_RIGHT:
			return (
				self._motors["FRONT_RIGHT_FORWARD"]["channel"],
				self._motors["FRONT_RIGHT_BACK"]["channel"],
			)
		elif wheel == Wheel.BACK_LEFT:
			return (
				self._motors["BACK_LEFT_FORWARD"]["channel"],
				self._motors["BACK_LEFT_BACK"]["channel"],
			)
		elif wheel == Wheel.BACK_RIGHT:
			return (
				self._motors["BACK_RIGHT_FORWARD"]["channel"],
				self._motors["BACK_RIGHT_BACK"]["channel"],
			)
		else:
			raise ValueError(f"bad wheel {wheel}")

	def set_wheel_speed(self, wheel: Wheel, dir: Direction, speed: int) -> None:
		motors = self.get_wheel(wheel)
		forward = 0
		back = 0
		if dir == Direction.FORWARD:
			forward = speed
		elif dir == Direction.BACK:
			back = speed
		elif dir == Direction.ALL:
			forward = speed
			back = speed
		else:
			raise ValueError(f"bad direction {dir}")
		self.set_speed(motors[0], forward)
		self.set_speed(motors[1], back)

	def forward(self, speed: int=100) -> None:
		self.set_wheel_speed(Wheel.FRONT_LEFT,  Direction.FORWARD, speed)
		self.set_wheel_speed(Wheel.FRONT_RIGHT, Direction.FORWARD, speed)
		self.set_wheel_speed(Wheel.BACK_LEFT,   Direction.FORWARD, speed)
		self.set_wheel_speed(Wheel.BACK_RIGHT,  Direction.FORWARD, speed)

	def back(self, speed: int=100) -> None:
		self.set_wheel_speed(Wheel.FRONT_LEFT,  Direction.BACK,    speed)
		self.set_wheel_speed(Wheel.FRONT_RIGHT, Direction.BACK,    speed)
		self.set_wheel_speed(Wheel.BACK_LEFT,   Direction.BACK,    speed)
		self.set_wheel_speed(Wheel.BACK_RIGHT,  Direction.BACK,    speed)

	def turn_left(self, speed: int=100) -> None:
		self.set_wheel_speed(Wheel.FRONT_LEFT,  Direction.BACK,    speed)
		self.set_wheel_speed(Wheel.FRONT_RIGHT, Direction.FORWARD, speed)
		self.set_wheel_speed(Wheel.BACK_LEFT,   Direction.BACK,    speed)
		self.set_wheel_speed(Wheel.BACK_RIGHT,  Direction.FORWARD, speed)

	def turn_right(self, speed: int=100) -> None:
		self.set_wheel_speed(Wheel.FRONT_LEFT,  Direction.FORWARD, speed)
		self.set_wheel_speed(Wheel.FRONT_RIGHT, Direction.BACK,    speed)
		self.set_wheel_speed(Wheel.BACK_LEFT,   Direction.FORWARD, speed)
		self.set_wheel_speed(Wheel.BACK_RIGHT,  Direction.BACK,    speed)

	def stop(self) -> None:
		self.set_wheel_speed(Wheel.FRONT_LEFT,  Direction.ALL, 0)
		self.set_wheel_speed(Wheel.FRONT_RIGHT, Direction.ALL, 0)
		self.set_wheel_speed(Wheel.BACK_LEFT,   Direction.ALL, 0)
		self.set_wheel_speed(Wheel.BACK_RIGHT,  Direction.ALL, 0)

