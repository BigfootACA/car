import sys
from time import time

def WriteFile(path: str, data: str|bytes) -> None:
	xstr = data
	if type(xstr) is bytes:
		xstr = xstr.decode()
	if type(xstr) is str:
		xstr = xstr.strip()
	try:
		# print(f"write {xstr} to {path}", file=sys.stderr)
		with open(path, "w") as f:
			f.write(data)
	except BaseException as e:
		print(f"write {xstr} to {path} failed", file=sys.stderr)
		raise e

def ReadFile(path: str) -> str:
	try:
		with open(path, "r") as f:
			return f.read().strip()
	except BaseException as e:
		print(f"read {path} failed")
		raise e

def ReadFileInt(path: str) -> int:
	return int(ReadFile(path))

def WriteFileInt(path: str, data: int, lf: bool=True) -> int:
	return WriteFile(path, f"{data}{"\n" if lf else ""}")

def GetNowMS()->int:
	return int(time() * 1000)
