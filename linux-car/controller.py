from evdev import InputDevice, ecodes
from time import sleep
from threading import Thread
from car import Car, Direction, Wheel
from pwm import PWMChip
from hardware import *

dev = InputDevice('/dev/input/event2')

lx, ly = 0, 0
lcx, lcy = 0, 0
lpx, lpy = 0, 0
running = True
enable_motor = False
p = PWMChip.FromDevicePath(motor_ctrl)
c = Car(pwm=p, motors=motors)

def calculate_motor_speeds(x, y):
	x = max(-100, min(100, x))
	y = max(-100, min(100, y))
	left_front_speed = y + x
	right_front_speed = y - x
	left_back_speed = y + x
	right_back_speed = y - x
	def regulate_speed(speed):
		if speed > 0:
			direction = Direction.FORWARD
		elif speed < 0:
			direction = Direction.BACK
		else:
			speed = 0
			direction = Direction.ALL
		regulated_speed = min(100, max(0, abs(speed)))
		return (regulated_speed, direction)
	left_front_motor = regulate_speed(left_front_speed)
	right_front_motor = regulate_speed(right_front_speed)
	left_back_motor = regulate_speed(left_back_speed)
	right_back_motor = regulate_speed(right_back_speed)

	return [
		{"w": Wheel.FRONT_LEFT,  "d": left_front_motor[1],  "s": left_front_motor[0]},
		{"w": Wheel.FRONT_RIGHT, "d": right_front_motor[1], "s": right_front_motor[0]},
		{"w": Wheel.BACK_LEFT,   "d": left_back_motor[1],   "s": left_back_motor[0]},
		{"w": Wheel.BACK_RIGHT,  "d": right_back_motor[1],  "s": right_back_motor[0]},
	]

def map_value(
	value: int,
	orig_min: int, orig_max: int,
	target_min: int, target_max: int
) -> int:
	norm_value = (value - orig_min) / (orig_max - orig_min)
	scaled_value = norm_value * (target_max - target_min) + target_min
	return int(scaled_value)


def time_loop():
	global running
	old_lpx, old_lpy = 0, 0
	try:
		while running:
			sleep(0.03)
			if old_lpx == lpx and old_lpy == lpy:
				continue
			old_lpx, old_lpy = lpx, lpy
			print("\r\033[2K\rX: %-3d Y: %-3d\r" % (lpx, lpy), end="")
			if not enable_motor:
				continue
			for data in calculate_motor_speeds(lpx, lpy):
				c.set_wheel_speed(data["w"], data["d"], data["s"])
			sleep(0.1)
	except KeyboardInterrupt:
		c.stop()
		running = False
		exit(1)

thread = Thread(target=time_loop)
thread.start()

try:
	print("start loop")
	ax = dev.capabilities()[ecodes.EV_ABS][ecodes.ABS_X]
	ay = dev.capabilities()[ecodes.EV_ABS][ecodes.ABS_Y]
	ai = (ax[1], ay[1])
	print(f"ABS_X range: {ai[0].min} to {ai[0].max}")
	print(f"ABS_Y range: {ai[1].min} to {ai[1].max}")
	for event in dev.read_loop():
		if not running:
			break
		if event.type == ecodes.EV_KEY and  event.value == 1:
			if event.code == ecodes.BTN_A:
				if lx != 0 and ly != 0:
					lcx, lcy = lx, ly
					print(f"calibrate {lcx} {lcy}")
					enable_motor = True
			if event.code == ecodes.BTN_X:
				enable_motor = not enable_motor
				if not enable_motor:
					c.stop()
				print("motor enabled: %s" % enable_motor)
		if event.type == ecodes.EV_ABS:
			if event.code == ecodes.ABS_X:
				lx = event.value
			if event.code == ecodes.ABS_Y:
				ly = event.value
		if lx >= lcx - 100 and lx <= lcx + 100:
			lx = lcx
		if ly >= lcy - 100 and ly <= lcy + 100:
			ly = lcy
		lpx = map_value(lx, ai[0].min, ai[0].max, -100, 100)
		lpy = map_value(ly, ai[1].min, ai[1].max, -100, 100)
except KeyboardInterrupt:
	running = False
	thread.join()
	c.stop()
