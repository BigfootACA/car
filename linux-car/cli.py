import sys
from car import Car, Wheel, Direction
from hardware import *
from pwm import PWMChip
from ptz import PanTiltZoom

class ConsoleContext:
	def __init__(self):
		self.file = sys.stdin

	def setup(self):
		pass

	def getchar(self):
		pass

	def restore(self):
		pass

class ConsoleContextLinux(ConsoleContext):
	def __init__(self):
		self.file = sys.stdin

	def setup(self):
		import tty
		import termios
		fd = self.file.fileno()
		self.old = termios.tcgetattr(fd)
		tty.setraw(self.file.fileno())

	def getchar(self):
		return sys.stdin.read(1)

	def restore(self):
		import termios
		fd = self.file.fileno()
		termios.tcsetattr(fd, termios.TCSADRAIN, self.old)

con = ConsoleContextLinux()
con.setup()
try:
	pwm = PWMChip.FromDevicePath(motor_ctrl)
	ptz = PanTiltZoom.FromConfig(chip=pwm, cfg=ptz_servo["camera"])
	car = Car(pwm=pwm, motors=motors)
	ptz.ResetSlow()
	while True:
		char = con.getchar()
		if char == "w":
			print("move up", end="\r\n")
			ptz.MoveUp()
		elif char == "a":
			print("move left", end="\r\n")
			ptz.MoveLeft()
		elif char == "s":
			print("move down", end="\r\n")
			ptz.MoveDown()
		elif char == "d":
			print("move right", end="\r\n")
			ptz.MoveRight()
		elif char == "r":
			print("reset", end="\r\n")
			ptz.ResetSlow()
		elif char == "t":
			print("test", end="\r\n")
			ptz.FullTest()
		elif char == "q":
			print("quit", end="\r\n")
			break
		deg = ptz.GetDegress()
		print(f"Degress ({deg[0]:.4f},{deg[1]:.4f})", end="\r\n")
finally:
	con.restore()
