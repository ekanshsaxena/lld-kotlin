interface Beverage {
    fun getDescription(): String
    fun cost(): Double
}

abstract class CondimentDecorator(protected val beverage: Beverage) : Beverage {
    abstract override fun getDescription(): String
}

class Espresso : Beverage {
    override fun getDescription(): String = "Espresso"
    override fun cost(): Double = 1.99
}

class HouseBlend : Beverage {
    override fun getDescription(): String = "House Blend Coffee"
    override fun cost(): Double = 0.89
}

class Mocha(beverage: Beverage) : CondimentDecorator(beverage) {

    override fun getDescription(): String = beverage.getDescription() + ", Mocha"
    override fun cost(): Double = beverage.cost() + 0.20
}

class Soy(beverage: Beverage) : CondimentDecorator(beverage) {

    override fun getDescription(): String = beverage.getDescription() + ", Soy"
    override fun cost(): Double = beverage.cost() + 0.15
}

fun main() {
    var beverage: Beverage = Espresso()
    println("${beverage.getDescription()} $${beverage.cost()}")

    beverage = Mocha(beverage)
    println("${beverage.getDescription()} $${beverage.cost()}")

    beverage = Soy(beverage)
    println("${beverage.getDescription()} $${beverage.cost()}")
}