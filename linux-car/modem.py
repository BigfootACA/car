#!/usr/bin/env python3
import sys
import json
import os
import io
import time
import socket
import datetime
import requests
from logging import *
from serial import Serial
from pydbus import SystemBus

PIN_FILE = "/etc/.sim-pin.txt"
XTRA_URL = "http://xtrapath1.izatcloud.net/xtra2.bin"

class CMEError(Exception):
        def __init__(self, code: int, message: str=None):
                if message is None:
                        reason = self.find_error(code)
                        message = f"CME error {code} ({reason})"
                super().__init__(message)
                self.code = code

        def load_error(self) -> list:
                try:
                        with open("cme.json", "r") as f:
                                return json.load(f)
                except BaseException as f:
                        return []

        def find_error(self, code: int, default: str="Unknown error") -> str:
                reason: str = default
                for item in self.load_error():
                        if item["code"] == code:
                                reason = item["reason"]
                return reason

def parse_args(value: str) -> list[str]:
        idx = 0
        ret = []
        buffer = ""
        while idx < len(value):
                if value[idx] == "\"" or value[idx] == "'":
                        sp = value[idx]
                        idx += 1
                        while True:
                                if value[idx] == "\\":
                                        buffer += value[idx + 1]
                                        idx += 2
                                elif value[idx] == sp:
                                        idx += 1
                                        break
                                else:
                                        buffer += value[idx]
                                        idx += 1
                elif value[idx] == ",":
                        idx += 1
                        ret.append(buffer)
                        buffer = ""
                else:
                        buffer += value[idx]
                        idx += 1
        if len(buffer) > 0:
                ret.append(buffer)
        return ret

class ModemAT:
        def __init__(self, dev: str, baudrate: 9600):
                info(f"open port {dev} with {baudrate}")
                self.ser = Serial(dev, baudrate, timeout=1)

        def __del__(self):
                self.ser.close()

        def send_at(self, cmd: str):
                data = f"AT+{cmd}\r\n"
                info(f"TX: {data.strip()}")
                self.ser.write(data.encode())
                self.ser.flush()

        def read_cmd(self) -> str:
                while True:
                        line = self.ser.readline()
                        if len(line) == 0:
                                continue
                        line = line.decode().strip()
                        if len(line) == 0:
                                continue
                        info(f"RX: {line}")
                        if line == "ERROR":
                                raise Exception("found a error")
                        if line.startswith("+CME ERROR: "):
                                raise CMEError(int(line[12:]))
                        return line

        def wait_res(self, cmd: str) -> str:
                part = f"+{cmd}: "
                while True:
                        line: str = self.read_cmd()
                        if line.startswith(part):
                                return line[len(part):]

        def wait_for(self, wait: str):
                while True:
                        line = self.read_cmd()
                        if line == wait:
                                return

        def wait_ok(self):
                self.wait_for("OK")

        def exec_one(self, cmd: str):
                self.send_at(cmd)
                self.wait_ok()

        def exec_get(self, cmd: str, args: str=None) -> str:
                req = f"{cmd}?" if args is None else f"{cmd}={args}"
                self.send_at(req)
                ret = self.wait_res(cmd)
                self.wait_ok()
                return ret

        def exec_get_multi(self, cmd: str, args: str=None) -> list[str]:
                req = f"{cmd}?" if args is None else f"{cmd}={args}"
                self.send_at(req)
                part = f"+{cmd}: "
                ret = []
                while True:
                        line: str = self.read_cmd()
                        if line.startswith(part):
                                ret.append(line[len(part):])
                        if line == "OK":
                                return ret

        def exec_get_args(self, cmd: str, args: str=None) -> list[str]:
                return parse_args(self.exec_get(cmd, args))

        def exec_get_multi_args(self, cmd: str, args: str=None) -> list[list[str]]:
                ret = []
                for item in self.exec_get_multi(cmd, args):
                        ret.append(parse_args(item))
                return ret

        def calc_xor_checksum(self, data: bytes, size: int=0, checksum: int=0):
                if size == 0:
                        size = len(data)
                i = 0
                while i < size - 1:
                        pair = data[i] << 8 | data[i + 1]
                        checksum ^= pair
                        i += 2
                if size % 2 == 1:
                        last_char = data[-1] << 8
                        checksum ^= last_char
                return checksum

        def write_data(self, data: io.BytesIO, off: int=0, size: int=0, ack: bool=True) -> int:
                wrote = 0
                sum = 0
                self.wait_for("CONNECT")
                data.seek(off, os.SEEK_SET)
                while True:
                        part = data.read(1024)
                        if not part:
                                if wrote < size:
                                        raise Exception("unexcepted eof")
                                break
                        sum = self.calc_xor_checksum(part, checksum=sum)
                        self.ser.write(part)
                        self.ser.flush()
                        wrote += len(part)
                        if wrote >= size:
                                break
                        if ack:
                                self.wait_for("A")
                return sum

        def upload_file(self, name: str, data: io.BytesIO, size: int=0, delete: bool=False, ack: bool=True):
                if len(name.encode("utf-8")) > 80:
                        raise Exception("filename too large")
                if size <= 0:
                        data.seek(0, os.SEEK_END)
                        size = data.tell()
                        if size <= 0:
                                raise Exception("bad file size")
                if delete:
                        try:
                                self.exec_one(f"QFDEL=\"{name}\"")
                        except CMEError as e:
                                if e.code != 418:
                                        raise
                self.send_at(f"QFUPL=\"{name}\",{size},5,{1 if ack else 0}")
                sum = self.write_data(data, 0, size, ack=ack)
                data.seek(0, os.SEEK_SET)
                ret = parse_args(self.wait_res("QFUPL"))
                self.wait_ok()
                rsize = int(ret[0])
                rsum = int(ret[1], base=16)
                if rsize != size:
                        raise Exception(f"size mismatch {rsize} != {size}")
                if rsum != sum:
                        raise Exception(f"checksum mismatch {rsum} != {sum}")

        def send_file(self, path: str, dest: str, force: bool=False, ack: bool=True):
                with open(path, "rb") as f:
                        if not force:
                                try:
                                        f.seek(0, os.SEEK_END)
                                        size = f.tell()
                                        arg = at.exec_get_args("QFLST",f"\"{dest}\"")
                                        if size == int(arg[1]):
                                                return
                                except BaseException:
                                        pass
                        f.seek(0, os.SEEK_SET)
                        at.upload_file(dest, f, delete=True, ack=ack)

        def unlock_sim(self, pin: str=None, pin_file: str=None):
                if self.exec_get("CPIN") == "SIM PIN":
                        info("try to unlock")
                        if pin is None and pin_file is not None:
                                if not os.path.exists(pin_file):
                                        raise Exception("no sim pin file")
                                with open(PIN_FILE, "r") as f:
                                        pin = f.readline().strip()
                                if len(pin) == 0:
                                        raise Exception("bad sim pin")
                        if pin is None:
                                raise Exception("no sim pin")
                        self.exec_one(f"CPIN={pin}")
                        ret = self.exec_get("CPIN")
                        if ret == "READY":
                                info("successful")
                        else:
                                raise Exception(f"unlock failed: {ret}")

def FindModem(mm) -> dict:
        modems = mm.GetManagedObjects()
        for dev in mm.GetManagedObjects():
                modem = modems[dev]["org.freedesktop.ModemManager1.Modem"]
                rev: str = modem["Revision"]
                if rev.startswith("EC20"):
                        return modem
        raise Exception("no modem found")

def FindPort(modem, type: int) -> str:
        for port in modem["Ports"]:
                if port[1] == type:
                        return port[0]
        raise Exception("no modem at port found")

def GetModemATPort(modem) -> ModemAT:
        dev = FindPort(modem, 3)
        return ModemAT(f"/dev/{dev}", 9600)

def InitGPS(at: ModemAT):
        try:
                at.exec_one("QGPSEND")
        except CMEError as e:
                if e.code != 505:
                        raise
        at.exec_one("QGPSXTRA=1")
        arg = at.exec_get_args("QGPSXTRADATA")
        if int(arg[0]) == 0:
                info("need update data")
                now = datetime.datetime.now(datetime.timezone.utc)
                us = now.strftime('%Y/%m/%d,%H:%M:%S')
                at.exec_one(f"QGPSXTRATIME=0,\"{us}\",1,1,3500")
                xtra_name = "xtra2.bin"
                xtra_dest = f"UFS:{xtra_name}"
                if not os.path.exists(xtra_name):
                        res = requests.get(XTRA_URL)
                        if res.status_code != 200:
                                raise Exception(f"download {XTRA_URL} failed: {res.status_code}")
                        data = res.content
                        info(f"download {XTRA_URL} size {len(data)}")
                        with open(xtra_name, "wb") as f:
                                f.write(data)
                at.send_file(xtra_name, xtra_dest, ack=False, force=True)
                at.exec_one(f"QGPSXTRADATA=\"{xtra_dest}\"")
        at.exec_one("QGPS=1")
        try:
                at.exec_get("QGPSLOC", "0")
        except CMEError as e:
                if e.code != 516:
                        raise
                info("not fixed now")

def InitGPSD(modem):
        try:
                gps = FindPort(modem, 5)
                server = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
                server.connect("/run/gpsd.sock")
                info(f"add gpsd device /dev/{gps}")
                server.send(f"+/dev/{gps}\r\n".encode())
                server.close()
        except BaseException as f:
                error(f)

def WaitModem(mm) -> dict:
        try:
                modem = FindModem(mm)
        except Exception:
                info("waiting for modem")
                while True:
                        time.sleep(1)
                        try:
                                modem = FindModem(mm)
                                if modem:
                                        break
                        except:
                                pass
        return modem

basicConfig(stream=sys.stdout, level=INFO)
mm = SystemBus().get('.ModemManager1')
modem = WaitModem(mm)
# at = GetModemATPort(modem)
at = ModemAT(f"/dev/ttyS0", 115200)
at.unlock_sim(pin_file=PIN_FILE)
InitGPS(at)
InitGPSD(modem)
