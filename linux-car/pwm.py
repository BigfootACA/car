from enum import Enum
from os.path import exists
from utils import *
from re import search


class PWMPolarity(Enum):
	NORMAL = 0
	INVERSED = 1


class PWMChip:
	def __init__(self, chip: int=0) -> None:
		self._path = f"/sys/class/pwm/pwmchip{chip}"

	@property
	def npwm(self) -> str:
		return ReadFileInt(f"{self.path}/npwm")

	@property
	def path(self) -> str:
		return self._path

	@property
	def export(self) -> None:
		return None

	@export.setter
	def export(self, pwm: int):
		if pwm >= self.npwm:
			raise IndexError(f"only {self.npwm} PWMs available")
		ctrl = f"{self.path}/pwm{pwm}"
		if not exists(ctrl):
			WriteFileInt(f"{self.path}/export", pwm)

	@property
	def unexport(self) -> None:
		return None

	@unexport.setter
	def unexport(self, pwm: int):
		if pwm >= self.npwm:
			raise IndexError(f"only {self.npwm} PWMs available")
		ctrl = f"{self.path}/pwm{pwm}"
		if exists(ctrl):
			WriteFileInt(f"{self.path}/unexport", pwm)

	def Channel(self, pwm: int):
		return PWM(self, pwm)

	def ChannelFromConfig(self, cfg: dict):
		pwm: PWM = PWM(self, cfg["channel"])
		if "period" in cfg and cfg["period"] != pwm.period:
			pwm.period = cfg["period"]
		return pwm

	@staticmethod
	def FromDevicePath(reg: str):
		from pyudev import Context
		context = Context()
		for dev in context.list_devices(subsystem="pwm"):
			if not search(reg, dev.device_path):
				continue
			if not dev.sys_name.startswith("pwmchip"):
				continue
			return PWMChip(int(dev.sys_name[7:]))
		raise Exception("no match pwm device found")


class PWM:
	def __init__(self, chip: PWMChip, chan: int) -> None:
		self._chip = chip
		self._chan = chan
		self._chip.export = self._chan

	@property
	def chip(self) -> PWMChip:
		return self._chip

	@property
	def channel(self) -> PWMChip:
		return self._chan

	@property
	def path(self) -> str:
		return f"{self.chip.path}/pwm{self.channel}"

	@property
	def period(self) -> str:
		return ReadFileInt(f"{self.path}/period")

	@period.setter
	def period(self, value: int):
		WriteFileInt(f"{self.path}/period", value)

	@property
	def duty_cycle(self) -> str:
		return ReadFileInt(f"{self.path}/duty_cycle")

	@duty_cycle.setter
	def duty_cycle(self, value: int):
		WriteFileInt(f"{self.path}/duty_cycle", value)

	@property
	def enable(self) -> bool:
		return ReadFileInt(f"{self.path}/enable") != 0

	@enable.setter
	def enable(self, en: bool):
		WriteFileInt(f"{self.path}/enable", 1 if en else 0)

	@property
	def polarity(self) -> PWMPolarity:
		return PWMPolarity(ReadFile(f"{self.path}/polarity").upper())

	@polarity.setter
	def polarity(self, value: PWMPolarity):
		WriteFile(f"{self.path}/polarity", value.name.lower())

	def Set(self, duty_cycle: int, period: int=0):
		if period != 0:
			self.period = period
		self.duty_cycle = duty_cycle
		self.enable = 1

	def SetValue(self, value: int, max: int=0):
		if max == 0:
			max = self.period
		if max == 0:
			raise ValueError("bad max")
		if value > max or value < 0:
			raise ValueError("bad value")
		self.duty_cycle = int(value * self.period / max)
		self.enable = 1

	def SetPercent(self, value: int):
		self.SetValue(value, 100)
