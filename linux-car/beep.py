from wave import open as OpenWave
from pyaudio import PyAudio
from threading import Thread, Semaphore
from utils import GetNowMS


class CarBeep:

	def __init__(self):
		self.wave = OpenWave("/etc/sound/beep.wav", "rb")
		self.audio = PyAudio()
		self.stream = self.audio.open(
			format=self.audio.get_format_from_width(self.wave.getsampwidth()),
			channels=self.wave.getnchannels(),
			rate=self.wave.getframerate(),
			output=True
		)
		self.sem = Semaphore()
		self.play = False
		self.last_play = GetNowMS()
		self.beep_thread = Thread(target=self.BeepLoop)
		self.beep_thread.setName("BeepAudio")
		self.beep_thread.start()

	def __del__(self):
		self.stream.stop_stream()
		self.stream.close()
		self.audio.terminate()

	def BeepLoop(self):
		while True:
			while not self.play:
				self.sem.acquire()
			self.wave.rewind()
			while True:
				data = self.wave.readframes(1024)
				if not data:
					break
				self.stream.write(data)
				now = GetNowMS()
				if now - self.last_play > 200:
					self.play = False
					break

	def Beep(self):
		self.last_play = GetNowMS()
		self.play = True
		self.sem.release()
