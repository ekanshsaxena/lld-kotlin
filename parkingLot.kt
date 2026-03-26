/*
---------------------------------------------------------------
Functional Requirements:
1. Vehicle can be BIKE, CAR, TRUCK as vehicle type, and has vehicleNumber which will be unique
2. There will be parking spots identified by unique slotId, and it can be free or occupied.
3. There can be multiple floors in the parking lot
4. Vehicle can only be parked on spot meant for it or larger spots
5. We need to prioritize smallest applicable spots (BIKE < CAR < TRUCK) for the vehicle first.
6. When Vehicle reaches the parking lot, it gets the ticket which contains: slotId, entryTime, vehicle number.
7. When Vehicle leaves the parking lot, it pays the charge and leaves. After payment done spot will be freed.
8. Each vehicle type has its own base amount.
9. In charge, hours will be ceiled to nearest integer value like 1.2 hour becomes 2 hours.
10. Example Charge: BIKE (has base amount 5) * hours (2) = 10
11. If ticket is not valid during exit time, system will reject the exit request.
12. Once ticket used (exit done after successful payment), it will be invalid or can't used for entry again.
13. Supported Payment Methods are Cash, UPI, and Card
14. If Parking Lot is full, entry of new vehicle should be rejected.
15. Smallest applicable spot will be searched globally across all floors. In case of tie, lower floor is preferred.


Non-Functional:
1. Concurrency should be handled.
2. Scalable for large parking lots
3. Optimized to find spots.

Assumptions:
1. System automatically handles entry, exit, and ticketing (no manual intervention required)
2. No need to implement payment gateway, just calculate charges based on vehicle type and parking hours.
3. We will not deeply design edge cases like lost ticket, payment failure — only acknowledge them.
4. Assume only one gate is there in the parking lot.


Edge Cases:
1. Ticket lost
2. Over-stay
3. Payment Failure

---------------------------------------------------------------
Entities:

    VEHICLE_TYPE: BIKE, CAR, TRUCK

    Vehicle:
        - type: VEHICLE_TYPE
        - vehicleNumber: String

    ParkingSpot:
        - spotNumber: String
        - floorNumber: Int
        - type: VEHICLE_TYPE
        - isOccupied: Boolean
        - vehicle: Vehicle?

        * parkVehicle(vehicle: Vehicle): Boolean
        * removeVehicle(): Boolean

    ParkingFloor:
        - floorNumber: Int
        - spotManagers: HashMap<VEHICLE_TYPE, SpotManager>

        * parkVehicle(vehicle: Vehicle): ParkingSpot?
        * removeVehicle(spot: ParkingSpot): Boolean
        * findAvailableSpot(vehicleType: VEHICLE_TYPE): ParkingSpot?

    SpotManager:
        - spotsCapacity: Int
        - type: VEHICLE_TYPE
        - availableSpots: MutableList<ParkingSpot>
        - occupiedSpots: MutableList<ParkingSpot>

        * addSpot(spot: ParkingSpot): Boolean
        * removeSpot(spot: ParkingSpot): Boolean
        * findAvailableSpot(): ParkingSpot?
        * parkVehicle(vehicle: Vehicle): ParkingSpot?
        * removeVehicle(spot: ParkingSpot): Boolean

    Payment:
        - paymentId: String
        - ticket: Ticket
        - amount: Double
        - paymentMethod: PAYMENT_METHOD
        - paidAt: LocalDateTime

    PaymentStrategy:
        * getCharge(entryTime: LocalDateTime): Double

    BikePaymentStrategy: PaymentStrategy
    CarPaymentStrategy: PaymentStrategy
    TruckPaymentStrategy: PaymentStrategy

    PAYMENT_METHOD: CASH, UPI, CARD

    PaymentMethodStrategy:
        * pay(amount: Double, ticket: Ticket): Payment

    CashPaymentStrategy: PaymentMethodStrategy
    UpiPaymentStrategy: PaymentMethodStrategy
    CardPaymentStrategy: PaymentMethodStrategy

    PaymentService:
        - paymentStrategies: Map<VEHICLE_TYPE, PaymentStrategy>

        * getPaymentMethodStrategy(paymentMethod: PAYMENT_METHOD): PaymentMethodStrategy
        * collectPayment(ticket: Ticket, paymentMethod: PAYMENT_METHOD): Payment?

    TICKET_STATUS: ACTIVE, COMPLETED, INVALID

    Ticket:
        - ticketNumber: String
        - parkingSpot: ParkingSpot
        - entryTime: LocalDateTime
        - vehicle: Vehicle
        - status: TICKET_STATUS

        * invalidateTicket()
        * completeTicket()

    ParkingManager: (Singleton)
        - floorCapacity: Int
        - issuedTickets: MutableMap<String, Ticket>
        - parkingFloors: MutableList<ParkingFloor>
        - instance: ParkingManager?

        * getInstance(): ParkingManager

    EntryManager -> ParkingManager
        * findAvailableSpot(vehicleType: VEHICLE_TYPE): ParkingSpot?
        * generateTicket(vehicle: Vehicle, spot: ParkingSpot): Ticket
        * parkVehicle(vehicle: Vehicle): Ticket?

    ExitManager -> ParkingManager
        - paymentService: PaymentService

        * validateTicket(vehicle: Vehicle, ticket: Ticket): Boolean
        * removeVehicle(ticket: Ticket): Boolean
        * exitParking(vehicle: Vehicle, ticket: Ticket, paymentMethod: PAYMENT_METHOD): Boolean

------------------------------------------------------------------------------------------------------------------------

*/

import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.ceil
import kotlin.synchronized

enum class VEHICLE_TYPE {
    BIKE,
    CAR,
    TRUCK
}

class Vehicle(val type: VEHICLE_TYPE, val vehicleNumber: String)

class ParkingSpot(val spotNumber: String, val floorNumber: Int, val type: VEHICLE_TYPE) {
    var isOccupied: Boolean = false
    var vehicle: Vehicle? = null

    fun parkVehicle(vehicle: Vehicle): Boolean {
        if (isOccupied || vehicle.type.ordinal > type.ordinal) return false
        this.vehicle = vehicle
        isOccupied = true
        return true
    }

    fun removeVehicle(): Boolean {
        if (!isOccupied) return false
        vehicle = null
        isOccupied = false
        return true
    }
}

class SpotManager(val spotsCapacity: Int, val type: VEHICLE_TYPE) {
    val availableSpots: MutableList<ParkingSpot> = mutableListOf()
    val occupiedSpots: MutableList<ParkingSpot> = mutableListOf()

    fun addSpot(spot: ParkingSpot): Boolean {
        if (spot.type == this.type && (availableSpots.size + occupiedSpots.size) < spotsCapacity) {
            availableSpots.add(spot)
            return true
        }
        return false
    }

    fun removeSpot(spot: ParkingSpot): Boolean {
        if (spot.type != this.type) return false
        if (availableSpots.contains(spot)) {
            availableSpots.remove(spot)
            return true
        }
        if (occupiedSpots.contains(spot)) {
            occupiedSpots.remove(spot)
            return true
        }
        return false
    }

    fun findAvailableSpot(): ParkingSpot? = availableSpots.firstOrNull()

    fun parkVehicle(vehicle: Vehicle): ParkingSpot? {
        val spot = availableSpots.firstOrNull()
        if (spot != null && spot.parkVehicle(vehicle)) {
            availableSpots.remove(spot)
            occupiedSpots.add(spot)
            return spot
        }
        return null
    }

    fun removeVehicle(spot: ParkingSpot): Boolean {
        if (spot.type != this.type || !occupiedSpots.contains(spot)) return false
        if (spot.removeVehicle()) {
            availableSpots.add(spot)
            occupiedSpots.remove(spot)
            return true
        }
        return false
    }
}

class ParkingFloor(val floorNumber: Int) {
    val spotManagers: HashMap<VEHICLE_TYPE, SpotManager> =
            hashMapOf(
                    VEHICLE_TYPE.BIKE to SpotManager(10, VEHICLE_TYPE.BIKE),
                    VEHICLE_TYPE.CAR to SpotManager(10, VEHICLE_TYPE.CAR),
                    VEHICLE_TYPE.TRUCK to SpotManager(10, VEHICLE_TYPE.TRUCK)
            )
    companion object {
        val vehicleTypeOrder: List<VEHICLE_TYPE> =
                listOf(VEHICLE_TYPE.BIKE, VEHICLE_TYPE.CAR, VEHICLE_TYPE.TRUCK)
    }

    init {
        for (i in 1..10) {
            spotManagers[VEHICLE_TYPE.BIKE]?.addSpot(
                    ParkingSpot("B$i", floorNumber, VEHICLE_TYPE.BIKE)
            )
            spotManagers[VEHICLE_TYPE.CAR]?.addSpot(
                    ParkingSpot("C$i", floorNumber, VEHICLE_TYPE.CAR)
            )
            spotManagers[VEHICLE_TYPE.TRUCK]?.addSpot(
                    ParkingSpot("T$i", floorNumber, VEHICLE_TYPE.TRUCK)
            )
        }
    }

    fun parkVehicle(vehicle: Vehicle): ParkingSpot? {
        val spot = findAvailableSpot(vehicle.type)
        return if (spot != null) {
            spotManagers[spot.type]?.parkVehicle(vehicle)
        } else {
            null
        }
    }

    fun removeVehicle(spot: ParkingSpot): Boolean {
        val spotManager = spotManagers[spot.type]
        return if (spotManager != null) {
            spotManager.removeVehicle(spot)
        } else {
            false
        }
    }

    fun findAvailableSpot(vehicleType: VEHICLE_TYPE): ParkingSpot? {
        val typeIndex = vehicleTypeOrder.indexOf(vehicleType)
        for (i in typeIndex..vehicleTypeOrder.size - 1) {
            val spot = spotManagers[vehicleTypeOrder[i]]?.findAvailableSpot()
            if (spot != null) return spot
        }
        return null
    }
}

enum class TICKET_STATUS {
    ACTIVE,
    COMPLETED,
    INVALID
}

class Ticket(
        val ticketNumber: String,
        val parkingSpot: ParkingSpot,
        val entryTime: LocalDateTime,
        val vehicle: Vehicle,
        var status: TICKET_STATUS
) {
    fun invalidateTicket() {
        status = TICKET_STATUS.INVALID
    }

    fun completeTicket() {
        status = TICKET_STATUS.COMPLETED
    }
}

interface PaymentStrategy {
    fun getCharge(entryTime: LocalDateTime): Double
}

class BikePaymentStrategy : PaymentStrategy {
    override fun getCharge(entryTime: LocalDateTime): Double {
        val hourDiff = ceil(Duration.between(entryTime, LocalDateTime.now()).toMinutes() / 60.0)
        return 5 * hourDiff
    }
}

class CarPaymentStrategy : PaymentStrategy {
    override fun getCharge(entryTime: LocalDateTime): Double {
        val hourDiff = ceil(Duration.between(entryTime, LocalDateTime.now()).toMinutes() / 60.0)
        return 10 * hourDiff
    }
}

class TruckPaymentStrategy : PaymentStrategy {
    override fun getCharge(entryTime: LocalDateTime): Double {
        val hourDiff = ceil(Duration.between(entryTime, LocalDateTime.now()).toMinutes() / 60.0)
        return 15 * hourDiff
    }
}

enum class PAYMENT_METHOD {
    CASH,
    UPI,
    CARD
}

class Payment(
        val paymentId: String,
        val ticket: Ticket,
        val amount: Double,
        val paymentMethod: PAYMENT_METHOD,
        val paidAt: LocalDateTime
)

interface PaymentMethodStrategy {
    fun pay(amount: Double, ticket: Ticket): Payment
}

class CashPaymentStrategy : PaymentMethodStrategy {
    override fun pay(amount: Double, ticket: Ticket): Payment {
        return Payment(
                "CASH-${ticket.ticketNumber}",
                ticket,
                amount,
                PAYMENT_METHOD.CASH,
                LocalDateTime.now()
        )
    }
}

class UpiPaymentStrategy : PaymentMethodStrategy {
    override fun pay(amount: Double, ticket: Ticket): Payment {
        return Payment(
                "UPI-${ticket.ticketNumber}",
                ticket,
                amount,
                PAYMENT_METHOD.UPI,
                LocalDateTime.now()
        )
    }
}

class CardPaymentStrategy : PaymentMethodStrategy {
    override fun pay(amount: Double, ticket: Ticket): Payment {
        return Payment(
                "CARD-${ticket.ticketNumber}",
                ticket,
                amount,
                PAYMENT_METHOD.CARD,
                LocalDateTime.now()
        )
    }
}

class PaymentService {
    private val paymentStrategies =
            mapOf(
                    VEHICLE_TYPE.BIKE to BikePaymentStrategy(),
                    VEHICLE_TYPE.CAR to CarPaymentStrategy(),
                    VEHICLE_TYPE.TRUCK to TruckPaymentStrategy()
            )

    private fun getPaymentMethodStrategy(paymentMethod: PAYMENT_METHOD): PaymentMethodStrategy {
        return when (paymentMethod) {
            PAYMENT_METHOD.CASH -> CashPaymentStrategy()
            PAYMENT_METHOD.UPI -> UpiPaymentStrategy()
            PAYMENT_METHOD.CARD -> CardPaymentStrategy()
        }
    }

    fun collectPayment(ticket: Ticket, paymentMethod: PAYMENT_METHOD): Payment? {
        val paymentStrategy = paymentStrategies[ticket.vehicle.type]
        if (paymentStrategy == null) return null
        val amount = paymentStrategy.getCharge(ticket.entryTime)
        val paymentMethodStrategy = getPaymentMethodStrategy(paymentMethod)
        return paymentMethodStrategy.pay(amount, ticket)
    }
}

// Or in kotlin we can just replace class with object, and it will be thread safe
class ParkingManager private constructor() {
    val floorCapacity: Int = 3
    val issuedTickets: MutableMap<String, Ticket> = mutableMapOf()
    val parkingFloors: MutableList<ParkingFloor> = mutableListOf()

    init {
        for (i in 1..floorCapacity) {
            parkingFloors.add(ParkingFloor(i - 1))
        }
    }

    companion object {
        @Volatile private var instance: ParkingManager? = null

        fun getInstance(): ParkingManager {
            if (instance != null) return instance!!
            synchronized(this) {
                if (instance == null) {
                    instance = ParkingManager()
                }
            }
            return instance!!
        }
    }
}

class EntryManager(private val parkingManager: ParkingManager) {
    private fun findAvailableSpot(vehicleType: VEHICLE_TYPE): ParkingSpot? {
        for (floor in parkingManager.parkingFloors) {
            val spot = floor.findAvailableSpot(vehicleType)
            if (spot != null) return spot
        }
        return null
    }

    private fun generateTicket(vehicle: Vehicle, spot: ParkingSpot): Ticket {
        val ticket =
                Ticket(
                        "TICKET-${vehicle.vehicleNumber}",
                        spot,
                        LocalDateTime.now(),
                        vehicle,
                        TICKET_STATUS.ACTIVE
                )
        parkingManager.issuedTickets[ticket.ticketNumber] = ticket
        return ticket
    }

    fun parkVehicle(vehicle: Vehicle): Ticket? {
        synchronized(parkingManager) {
            val spot = findAvailableSpot(vehicle.type)
            return if (spot != null) {
                if (spot.floorNumber < 0 || spot.floorNumber >= parkingManager.parkingFloors.size) {
                    println("Invalid floor number: ${spot.floorNumber}")
                    return null
                }
                val floor = parkingManager.parkingFloors[spot.floorNumber]
                val parkedSpot = floor.parkVehicle(vehicle)
                if (parkedSpot == null) return null
                return generateTicket(vehicle, parkedSpot)
            } else {
                println("No spot available for vehicle type: ${vehicle.type}")
                null
            }
        }
    }
}

class ExitManager(private val parkingManager: ParkingManager) {
    private val paymentService = PaymentService()

    private fun validateTicket(vehicle: Vehicle, ticket: Ticket): Boolean {
        if (ticket.status == TICKET_STATUS.COMPLETED) {
            println("Ticket is already used")
            return false
        }
        val existingTicket = parkingManager.issuedTickets[ticket.ticketNumber]
        if (ticket.status == TICKET_STATUS.INVALID || existingTicket != ticket) {
            println("Ticket is invalid")
            return false
        }
        if (vehicle.vehicleNumber != ticket.vehicle.vehicleNumber) {
            println("Ticket does not belong to this vehicle")
            return false
        }
        return true
    }

    private fun removeVehicle(ticket: Ticket): Boolean {
        val spot = ticket.parkingSpot
        if (spot.floorNumber < 0 || spot.floorNumber >= parkingManager.parkingFloors.size) {
            println("Invalid floor number: ${spot.floorNumber}")
            return false
        }
        val floor = parkingManager.parkingFloors[spot.floorNumber]
        return floor.removeVehicle(spot)
    }

    fun exitParking(vehicle: Vehicle, ticket: Ticket, paymentMethod: PAYMENT_METHOD): Boolean {
        if (ticket.status == TICKET_STATUS.COMPLETED) return false
        synchronized(parkingManager) {
            if (!validateTicket(vehicle, ticket)) return false
            val payment = paymentService.collectPayment(ticket, paymentMethod)
            if (payment == null) {
                println("Payment failed")
                return false
            }

            val isRemoved = removeVehicle(ticket)
            if (isRemoved) {
                ticket.completeTicket()
                parkingManager.issuedTickets.remove(ticket.ticketNumber)
                return true
            } else {
                println(
                        "Critical Error: Payment succeeded but could not remove vehicle. Refunding money..."
                )
                // paymentService.refund(payment) // Rollback the transaction
                return false
            }
        }
    }
}

fun main() {
    val parkingManager = ParkingManager.getInstance()
    val entryManager = EntryManager(parkingManager)
    val exitManager = ExitManager(parkingManager)

    val vehicle1 = Vehicle(VEHICLE_TYPE.CAR, "KA-01-AB-1234")
    val vehicle2 = Vehicle(VEHICLE_TYPE.BIKE, "KA-01-BC-5678")
    val vehicle3 = Vehicle(VEHICLE_TYPE.TRUCK, "KA-01-CD-9012")

    val ticket1 = entryManager.parkVehicle(vehicle1)
    val ticket2 = entryManager.parkVehicle(vehicle2)
    val ticket3 = entryManager.parkVehicle(vehicle3)

    if (ticket1 != null) {
        println("Ticket 1: ${ticket1.ticketNumber}")
    }
    if (ticket2 != null) {
        println("Ticket 2: ${ticket2.ticketNumber}")
    }
    if (ticket3 != null) {
        println("Ticket 3: ${ticket3.ticketNumber}")
    }

    exitManager.exitParking(vehicle1, ticket1!!, PAYMENT_METHOD.CASH)
    exitManager.exitParking(vehicle2, ticket2!!, PAYMENT_METHOD.UPI)
    exitManager.exitParking(vehicle3, ticket3!!, PAYMENT_METHOD.CARD)
}
