from car import Car, Wheel, Direction
from hardware import *
from pwm import PWMChip
from ptz import PanTiltZoom
from time import sleep
from random import randint
from linuxpy import video

p = PWMChip.FromDevicePath(motor_ctrl)

ptz = PanTiltZoom.FromConfig(p, ptz_servo["camera"])
# while any([ptz.MoveDown(1.0), ptz.MoveLeft(1.0)]):
# 	sleep(0.01)
# ptz.ResetSlow()
# while ptz.MoveDown(1.0):
# 	sleep(0.01)
# while ptz.MoveUp(1.0):
# 	sleep(0.01)
# ptz.ResetSlow()

# for i in range(100):
# 	x = randint(0,180)
# 	y = randint(0,180)
# 	ptz.MoveTo((x, y))
# 	sleep(0.2)

ptz.FullTest()
# ptz.ResetSlow()

exit(0)

c = Car(pwm=p, motors=motors)
for w in [
	Wheel.FRONT_LEFT,
	Wheel.FRONT_RIGHT,
	Wheel.BACK_LEFT,
	Wheel.BACK_RIGHT,
]:
	c.set_wheel_speed(w, Direction.FORWARD, 20)
	sleep(3)
	c.stop()
#c.forward()
c.stop()
