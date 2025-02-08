private fun normalizeTime(seconds: Long, milliseconds: Int) = Time(
    seconds = seconds + milliseconds / 1000,
    milliseconds = milliseconds % 1000,
)

val Int.milliseconds: Time
    get() = normalizeTime(
        seconds = 0,
        milliseconds = this,
    )

val Int.seconds: Time
    get() = Time(
        seconds = this.toLong(),
        milliseconds = 0,
    )

val Int.minutes: Time
    get() = this.seconds * 60

val Int.hours: Time
    get() = this.minutes * 60

operator fun Time.plus(other: Time): Time = normalizeTime(
    seconds = this.seconds + other.seconds,
    milliseconds = this.milliseconds + other.milliseconds,
)

operator fun Time.minus(other: Time): Time = normalizeTime(
    seconds = this.seconds - other.seconds - 1,
    milliseconds = this.milliseconds - other.milliseconds + 1000,
)

operator fun Time.times(times: Int): Time = normalizeTime(
    seconds = this.seconds * times,
    milliseconds = this.milliseconds * times,
)

operator fun Time.compareTo(other: Time): Int = compareBy<Time>(
    { it.seconds },
    { it.milliseconds },
).compare(this, other)
