enum class Size { SMALL, MEDIUM, LARGE }

abstract class Beverage(val size: Size = Size.MEDIUM) {
    abstract fun getDescription(): String
    abstract fun cost(): Double
}

abstract class CondimentDecorator(protected val beverage: Beverage) : Beverage(beverage.size)

class Espresso(size: Size = Size.MEDIUM) : Beverage(size) {
    override fun getDescription() = "Espresso"
    override fun cost() = when (size) {
        Size.SMALL  -> 1.79
        Size.MEDIUM -> 1.99
        Size.LARGE  -> 2.29
    }
}

class HouseBlend(size: Size = Size.MEDIUM) : Beverage(size) {
    override fun getDescription() = "House Blend Coffee"
    override fun cost() = when (size) {
        Size.SMALL  -> 0.69
        Size.MEDIUM -> 0.89
        Size.LARGE  -> 1.09
    }
}

class Mocha(beverage: Beverage) : CondimentDecorator(beverage) {
    override fun getDescription() = beverage.getDescription() + ", Mocha"
    override fun cost() = beverage.cost() + when (size) {
        Size.SMALL  -> 0.10
        Size.MEDIUM -> 0.20
        Size.LARGE  -> 0.30
    }
}

class Soy(beverage: Beverage) : CondimentDecorator(beverage) {
    override fun getDescription() = beverage.getDescription() + ", Soy"
    override fun cost() = beverage.cost() + when (size) {
        Size.SMALL  -> 0.10
        Size.MEDIUM -> 0.15
        Size.LARGE  -> 0.20
    }
}

fun printOrder(beverage: Beverage) {
    println("[${beverage.size}] ${beverage.getDescription()} -> ${"$%.2f".format(beverage.cost())}")
}

fun main() {
    // Medium Espresso (default size)
    printOrder(Espresso())

    // Large Espresso with Mocha
    printOrder(Mocha(Espresso(Size.LARGE)))

    // Small HouseBlend with Mocha + Soy
    var order: Beverage = HouseBlend(Size.SMALL)
    order = Mocha(order)
    order = Soy(order)
    printOrder(order)
}
