interface FlyBehavior {
    fun fly()
}

interface QuackBehavior {
    fun quack()
}

class FlyWithWings : FlyBehavior {
    override fun fly() {
        println("Flying with wings")
    }
}

class FlyNoWay : FlyBehavior {
    override fun fly() {
        println("Can't fly")
    }
}

class FlyRocketPowered : FlyBehavior {
    override fun fly() {
        println("I'm flying with a rocket!")
    }
}

class Quack : QuackBehavior {
    override fun quack() {
        println("Quack")
    }
}

class MuteQuack : QuackBehavior {
    override fun quack() {
        println("Mute quack")
    }
}

class Squeak : QuackBehavior {
    override fun quack() {
        println("Squeak")
    }
}

abstract class Duck(var flyBehavior: FlyBehavior, var quackBehavior: QuackBehavior) {

    fun performFly() {
        flyBehavior.fly()
    }

    fun performQuack() {
        quackBehavior.quack()
    }

    abstract fun display()

    fun swim() {
        println("Swimming")
    }
}

class MallardDuck : Duck(FlyWithWings(), Quack()) {
    override fun display() {
        println("Mallard Duck")
    }
}

class ModelDuck : Duck(FlyNoWay(), MuteQuack()) {
    override fun display() {
        println("Model Duck")
    }
}

fun main() {
    val mallardDuck = MallardDuck()
    mallardDuck.performFly()
    mallardDuck.performQuack()
    mallardDuck.display()
    mallardDuck.swim()

    val modelDuck = ModelDuck()
    modelDuck.performFly()
    modelDuck.performQuack()
    modelDuck.display()
    modelDuck.performFly()
    modelDuck.flyBehavior = FlyRocketPowered()
    modelDuck.performFly()
}
