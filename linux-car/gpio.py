from pathlib import Path
from gpiod import request_lines, LineSettings, LineRequest
from gpiod.line import Direction, Value

class GPIO:
	def __init__(self, lines: LineRequest, line: int):
		self.line = line
		self.lines = lines

	def Set(self, value: bool) -> None:
		self.lines.set_value(self.line, GPIO.BoolToValue(value))

	def Get(self) -> bool:
		return self.lines.get_value(self.line) == Value.ACTIVE

	@staticmethod
	def FromConfig(cfg: dict):
		dev = Path(f"/dev/gpiochip{cfg["chip"]}")
		if not dev.exists() or not dev.is_char_device():
			raise Exception(f"bad device {dev}")
		settings = LineSettings(
			direction=GPIO.StringToDirection(cfg.get("direction", "out")),
			output_value=GPIO.BoolToValue(cfg.get("value", True)),
			active_low=cfg.get("active-low", False),
		)
		lines = request_lines(
			str(dev),
			consumer=cfg.get("consumer", "gpio"),
			config={cfg["line"]: settings}
		)
		return GPIO(lines, cfg["line"])

	@staticmethod
	def StringToDirection(val: str) -> Direction:
		if val == "in": return Direction.INPUT
		if val == "out": return Direction.OUTPUT
		if val == "as-is": return Direction.AS_IS
		if val == "input": return Direction.INPUT
		if val == "output": return Direction.OUTPUT
		raise Exception(f"bad direction {val}")

	@staticmethod
	def BoolToValue(val: bool) -> Value:
		return Value.ACTIVE if val else Value.INACTIVE
