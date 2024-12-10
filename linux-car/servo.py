from time import sleep
from pwm import PWM, PWMChip

class Servo:
	def __init__(self,
		pwm: PWM,
		min: int,
		max: int,
		deg: int=180,
		default: int=90,
	) -> None:
		self._pwm = pwm
		self._min = min
		self._max = max
		self._deg = deg
		self._def = default
		if self._def < 0 or self._def > self._deg:
			raise ValueError("bad default degress")
		if self._min >= self._max:
			raise ValueError("bad value range")

	@property
	def min_value(self) -> int:
		return self._min

	@property
	def max_value(self) -> int:
		return self._max

	@property
	def value_range(self) -> int:
		return self.max_value - self.min_value

	@property
	def degress_range(self) -> int:
		return self._deg

	@property
	def default(self) -> int:
		return self._def

	def CalcValue(self, deg: float) -> int:
		deg = min(self.degress_range, max(deg, 0))
		val = deg * self.value_range / self.degress_range
		return int(self.min_value + val)

	def SetDegress(self, deg: float) -> None:
		val = self.CalcValue(deg)
		self._pwm.SetValue(val)

	def GetDegress(self) -> float:
		raw = self._pwm.duty_cycle
		if raw <= self.min_value:
			return 0.0
		if raw >= self.max_value:
			return float(self.degress_range)
		val = raw - self.min_value
		return float(val * self.degress_range / self.value_range)

	def MoveTo(self, deg: float) -> None:
		self.SetDegress(deg)

	def MoveSlowTo(self, deg: float, time: float=0.01, step: float=1.0) -> bool:
		cur = self.GetDegress()
		while abs(cur - deg) > 0.1:
			self.MoveTo(min(abs(cur - deg), step))
			cur = self.GetDegress()
			sleep(time)

	def Increment(self, deg: float=1.0) -> bool:
		if deg <= 0:
			deg = -deg
		cur = self.GetDegress()
		if cur >= self.degress_range:
			return False
		self.SetDegress(cur + deg)
		return True

	def Subtract(self, deg: float=1.0) -> bool:
		if deg <= 0:
			deg = -deg
		cur = self.GetDegress()
		if cur <= 0:
			return False
		self.SetDegress(cur - deg)
		return True

	degress = property(GetDegress, SetDegress)

	@staticmethod
	def FromConfig(chip: PWMChip, cfg: dict):
		return Servo(
			chip.ChannelFromConfig(cfg),
			min=cfg["min"],
			max=cfg["max"],
			deg=cfg.get("degress", 180),
			default=cfg.get("default", 90),
		)
