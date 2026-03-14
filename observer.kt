interface Subject {
    fun registerObserver(observer: Observer)
    fun unregisterObserver(observer: Observer)
    fun notifyObservers()
}

interface Observer {
    fun update(subject: Subject)
}

class WeatherStation : Subject {
    private val observers = mutableListOf<Observer>()
    private var temperature: Double = 0.0

    override fun registerObserver(observer: Observer) {
        observers.add(observer)
    }

    override fun unregisterObserver(observer: Observer) {
        observers.remove(observer)
    }

    override fun notifyObservers() {
        observers.forEach { it.update(this) }
    }

    fun setTemperature(temperature: Double) {
        this.temperature = temperature
        notifyObservers()
    }

    fun getTemperature(): Double = temperature
}

class PhoneDisplay(private val name: String) : Observer {
    override fun update(subject: Subject) {
        val weatherStation = subject as WeatherStation
        println("$name: Temperature is ${weatherStation.getTemperature()}")
    }
}

fun main() {
    val weatherStation = WeatherStation()
    val phone1 = PhoneDisplay("Phone 1")
    val phone2 = PhoneDisplay("Phone 2")

    weatherStation.registerObserver(phone1)
    weatherStation.registerObserver(phone2)

    weatherStation.setTemperature(25.0)
    weatherStation.setTemperature(30.0)
}

