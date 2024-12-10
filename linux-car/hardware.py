motor_ctrl = ".*/fe5b0000.i2c/i2c-[0-9]+/[0-9]+-0040/.*"

motors = {
	"FRONT_LEFT_FORWARD": {
		"channel": 7,
		"period": 5079040,
	},
	"FRONT_LEFT_BACK": {
		"channel": 6,
		"period": 5079040,
	},
	"FRONT_RIGHT_FORWARD": {
		"channel": 4,
		"period": 5079040,
	},
	"FRONT_RIGHT_BACK": {
		"channel": 5,
		"period": 5079040,
	},
	"BACK_LEFT_FORWARD": {
		"channel": 0,
		"period": 5079040,
	},
	"BACK_LEFT_BACK": {
		"channel": 1,
		"period": 5079040,
	},
	"BACK_RIGHT_FORWARD": {
		"channel": 3,
		"period": 5079040,
	},
	"BACK_RIGHT_BACK": {
		"channel": 2,
		"period": 5079040,
	},
}

ptz_servo = {
	"camera": {
		"x": {
			"channel": 8,
			"period": 5079040,
			"min": 600000,
			"max": 2800000,
			"degress": 180,
			"default": 120,
			"invert": False,
		},
		"y": {
			"channel": 9,
			"period": 5079040,
			"min": 600000,
			"max": 2800000,
			"degress": 180,
			"default": 110,
			"invert": True,
		},
	},
}

peripherals = {
	"light": {
		"channel": 10,
		"period": 5079040,
	},
	"lidar": {
		"speed": {
			"channel": 11,
			"period": 5079040,
		},
		"enable": {
			"chip": 3,
			"line": 1,
		},
	}
}
