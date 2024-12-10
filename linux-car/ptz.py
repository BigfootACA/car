from servo import Servo
from pwm import PWMChip
from time import sleep


class PanTiltZoom:
	def __init__(
		self,
		servo_x: Servo,
		servo_y: Servo,
		inv_x: bool=False,
		inv_y: bool=False,
	) -> None:
		self._servo_x = servo_x
		self._servo_y = servo_y
		self._inv_x = inv_x
		self._inv_y = inv_y

	def SetDegress(self, deg: tuple[float, float]) -> None:
		self._servo_x.SetDegress(deg[0])
		self._servo_y.SetDegress(deg[1])

	def GetDegress(self) -> tuple[float, float]:
		return (
			self._servo_x.GetDegress(),
			self._servo_y.GetDegress(),
		)

	def MoveX(self, deg: float) -> bool:
		if self._inv_x == (deg > 0):
			return self._servo_x.Increment(deg)
		else:
			return self._servo_x.Subtract(deg)

	def MoveY(self, deg: float) -> bool:
		if self._inv_y == (deg > 0):
			return self._servo_y.Increment(deg)
		else:
			return self._servo_y.Subtract(deg)

	def MoveLeft(self, deg: float=1.0) -> bool:
		return self.MoveX(-deg)

	def MoveRight(self, deg: float=1.0) -> bool:
		return self.MoveX(deg)

	def MoveUp(self, deg: float=1.0) -> bool:
		return self.MoveY(-deg)

	def MoveDown(self, deg: float=1.0) -> bool:
		return self.MoveY(deg)

	def MoveTo(self, deg: tuple[float, float]) -> bool:
		self.SetDegress(deg)

	def MoveSlowTo(self, deg: tuple[float, float], time: float=0.01, step: float=1.0) -> bool:
		tgt_x, tgt_y = deg
		cur_x, cur_y = self.GetDegress()
		dir_x = 1 if tgt_x > cur_x else -1
		dir_y = 1 if tgt_y > cur_y else -1
		while abs(tgt_x - cur_x) > 1e-2 or abs(tgt_y - cur_y) > 1e-2:
			if abs(tgt_x - cur_x) > 1e-2:
				move_x = dir_x * min(step, abs(tgt_x - cur_x))
				cur_x += move_x
			if abs(tgt_y - cur_y) > 1e-2:
				move_y = dir_y * min(step, abs(tgt_y - cur_y))
				cur_y += move_y
			self.MoveTo((cur_x, cur_y))
			sleep(time)
		self.MoveTo(deg)

	def GetDefault(self) -> tuple[float, float]:
		return (
			self._servo_x.default,
			self._servo_y.default,
		)

	def GetMax(self) -> tuple[float, float]:
		return (
			self._servo_x.default,
			self._servo_y.default,
		)

	def Reset(self) -> None:
		self.MoveTo(self.GetDefault())

	def ResetSlow(self) -> None:
		self.MoveSlowTo(self.GetDefault())

	def FullTest(self, time: float=0.01, step: float=1.0) -> None:
		while any([self.MoveDown(step), self.MoveLeft(step)]):
			sleep(time)
		while self.MoveUp(step):
			sleep(time)
		while self.MoveRight(step):
			sleep(time)
		while self.MoveDown(step):
			sleep(time)
		while self.MoveLeft(step):
			sleep(time)
		self.MoveSlowTo(self.GetDefault())

	default = property(GetDefault)
	degress = property(GetDegress, SetDegress)

	@staticmethod
	def FromConfig(chip: PWMChip, cfg: dict):
		return PanTiltZoom(
			servo_x=Servo.FromConfig(chip, cfg["x"]),
			servo_y=Servo.FromConfig(chip, cfg["y"]),
			inv_x=cfg["x"].get("invert", False),
			inv_y=cfg["y"].get("invert", False),
		)
