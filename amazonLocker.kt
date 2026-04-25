/* ═══════════════════════════════════════════════════════════════════════════
// REQUIREMENTS
//
// Example (Tic Tac Toe):
//   1. Two players alternate placing X and O on a 3x3 grid.
//   2. A player wins by completing a row, column, or diagonal.
//   Out of Scope: UI, AI opponent, networking
// ═══════════════════════════════════════════════════════════════════════════

Functional:
1. Multiple Locker stations support should be there, each at a different physical location.
2. Location can be presented as string only.
3. One locker station can have multiple compartments.
4. Compartments come in three sizes: SMALL, MEDIUM, and LARGE.
5. There will be only one parcel at a time in a compartment.
6. There will be no fallback to a larger size compartment if the exact size as of parcel isn't available.
7. When parcel get deposited to a compartment, a unique code is generated and returned to the user.
8. User can access the compartment with that unique access code.
9. If no compartment available of parcel's size, reject the deposit request
10. Access code will be expired after 7 days, package will stay in the compartment until staff manually removes it.
11. If user puts expired or invalid access code, request will be rejected.

Non-Functional:
1. Need to handle concurrency for multiple requests trying to check and act on same compartments.

Assumptions:
Deposit request comes with locker station and parcel size

Out of Scope:
1. If no compartment available, reassigning to a different locker station.
2. UI of the system
3. Choosing nearest locker station is out of scope.
4. No need to handle/store User specific details except access code.
5. Validation of auth of the staff request.

// ═══════════════════════════════════════════════════════════════════════════
// CLASS DESIGN
//
// Example (Tic Tac Toe):
//   class Game:
//     - board: Board
//     - currentPlayer: Player
//     + makeMove(row, col) -> bool
// ═══════════════════════════════════════════════════════════════════════════
PACKAGE_SIZE: SMALL, MEDIUM, LARGE
COMPARTMENT_SIZE: SMALL, MEDIUM, LARGE
COMPARTMENT_STATUS: OPEN, CLOSED

Package:
    - size: PACKAGE_SIZE

Compartment:
    - id: String
    - size: COMPARTMENT_SIZE
    - status: COMPARTMENT_STATUS

    * open(): void
    * close(): void

LockerStation:
    - capacity: Int
    - availableCompartments: HashMap<COMPARTMENT_SIZE, List<Compartment>>
    - occupiedCompartments: HashMap<COMPARTMENT_SIZE, List<Compartment>>
    - activeAccessCodes: HashMap<String, AccessCode> // key: accessCodeId
    - compartmentToAccessCodes: HashMap<String, String> // key: compartmentId, value: accessCodeId // Required to clean active access code in case of staff removal.
    - compartmentToPackage: HashMap<String, Package> // key: compartmentId

DepositManager:
    - lockerStation: LockerStation

    * bookCompartment(package: Package): String? // Checks respective available compartment and book it, returns compartmentId. In case of unavailability returns null.
    * deposit(package: Package): AccessCode? // Calls bookCompartment and generateAccessCode.
    * generateAccessCode(compartmentId: String): AccessCode // It will set expiry to currentDate + 7 days.

RetrieveManager:
    - lockerStation: LockerStation

    * validateAccessCode(accessCode: AccessCode): Boolean
    * releaseCompartment(compartmentId: String) // moves from occupied to available
    * retrieve(accessCode: AccessCode): Package? // Calls validateAccessCode, retrieve package using compartmentToPackage, releaseCompartment.
    * staffRemoval(compartmentId: String): Package? // Get access code from compartmentToAccessCodes and remove it from activeAccessCodes. Then retrieve from compartmentToPackage, and releaseCompartment.

AccessCode:
    - id: String
    - expiry: LocalDateTime
    - compartmentId: String

    * isExpired(): Boolean

Notes: (availableCompartments and occupiedCompartments shows object pool design pattern)


// ═══════════════════════════════════════════════════════════════════════════
// IMPLEMENTATION
// ═══════════════════════════════════════════════════════════════════════════*/
import java.time.LocalDateTime
import java.util.UUID

enum class PACKAGE_SIZE {
    SMALL,
    MEDIUM,
    LARGE
}

enum class COMPARTMENT_SIZE {
    SMALL,
    MEDIUM,
    LARGE
}

enum class COMPARTMENT_STATUS {
    OPEN,
    CLOSED
}

data class Package(val size: PACKAGE_SIZE)

class Compartment(val id: String, val size: COMPARTMENT_SIZE) {
    private var status: COMPARTMENT_STATUS = COMPARTMENT_STATUS.OPEN

    fun open() {
        status = COMPARTMENT_STATUS.OPEN
    }

    fun close() {
        status = COMPARTMENT_STATUS.CLOSED
    }
}

class AccessCode(val id: String, val compartmentId: String, val expiry: LocalDateTime) {
    fun isExpired(): Boolean {
        return expiry.isBefore(LocalDateTime.now())
    }
}

class LockerStation(val capacity: Int) {
    val availableCompartments: HashMap<COMPARTMENT_SIZE, MutableList<Compartment>> = HashMap()
    val occupiedCompartments: HashMap<COMPARTMENT_SIZE, MutableList<Compartment>> = HashMap()
    val activeAccessCodes: HashMap<String, AccessCode> = HashMap()
    val compartmentToAccessCodes: HashMap<String, String> = HashMap()
    val compartmentToPackage: HashMap<String, Package> = HashMap()

    init {
        for (i in 1..capacity) {
            val size =
                    when (i % 3) {
                        0 -> COMPARTMENT_SIZE.SMALL
                        1 -> COMPARTMENT_SIZE.MEDIUM
                        else -> COMPARTMENT_SIZE.LARGE
                    }
            val compartment = Compartment(i.toString(), size)
            availableCompartments.getOrPut(size) { mutableListOf() }.add(compartment)
        }
    }
}

class DepositManager(private val lockerStation: LockerStation) {
    private fun bookCompartment(pkg: Package): Compartment? {
        val compartmentSize = COMPARTMENT_SIZE.valueOf(pkg.size.name)
        val availableCompartments = lockerStation.availableCompartments[compartmentSize]
        if (availableCompartments.isNullOrEmpty()) {
            println("No compartment available for package of size ${pkg.size}")
            return null
        }
        val compartment = availableCompartments.removeAt(availableCompartments.size - 1)
        lockerStation
                .occupiedCompartments
                .getOrPut(compartmentSize) { mutableListOf() }
                .add(compartment)
        return compartment
    }

    private fun generateAccessCode(compartmentId: String): AccessCode {
        val accessCodeId = UUID.randomUUID().toString()
        val expiry = LocalDateTime.now().plusDays(7)
        return AccessCode(accessCodeId, compartmentId, expiry)
    }

    fun deposit(pkg: Package): AccessCode? {
        synchronized(lockerStation) {
            val compartment = bookCompartment(pkg)
            if (compartment == null) {
                return null
            }
            val accessCode = generateAccessCode(compartment.id)
            lockerStation.activeAccessCodes[accessCode.id] = accessCode
            lockerStation.compartmentToAccessCodes[compartment.id] = accessCode.id
            compartment.open()
            lockerStation.compartmentToPackage[compartment.id] = pkg
            compartment.close()
            return accessCode
        }
    }
}

class RetrieveManager(private val lockerStation: LockerStation) {
    private fun validateAccessCode(accessCode: AccessCode): Boolean {
        return lockerStation.activeAccessCodes[accessCode.id]?.isExpired() == false
    }

    private fun getCompartment(compartmentId: String): Compartment? {
        val size = lockerStation.compartmentToPackage[compartmentId]?.size
        if (size != null) {
            val compartmentSize = COMPARTMENT_SIZE.valueOf(size.name)
            val occupiedCompartments = lockerStation.occupiedCompartments[compartmentSize]
            if (occupiedCompartments.isNullOrEmpty()) {
                return null
            }
            var compartmentIndex = -1
            for (i in 0..occupiedCompartments.size - 1) {
                if (occupiedCompartments[i].id == compartmentId) {
                    compartmentIndex = i
                    break
                }
            }
            if(compartmentIndex == -1){
                println("Compartment not found")
                return null
            }
            return occupiedCompartments[compartmentIndex]
        }
        return null
    }

    private fun releaseCompartment(compartment: Compartment) {
        lockerStation.occupiedCompartments[compartment.size]?.remove(compartment)
        lockerStation
                .availableCompartments
                .getOrPut(compartment.size) { mutableListOf() }
                .add(compartment)
    }

    private fun cleanup(accessCodeId: String?, compartment: Compartment) {
        if(accessCodeId != null){
            lockerStation.activeAccessCodes.remove(accessCodeId)
        }
        lockerStation.compartmentToAccessCodes.remove(compartment.id)
        lockerStation.compartmentToPackage.remove(compartment.id)
    }

    fun retrieve(accessCode: AccessCode): Package? {
        synchronized(lockerStation) {
            if (!validateAccessCode(accessCode)) {
                println("Access code is invalid or expired. Please contact Staff.")
                return null
            }
            val compartmentId = accessCode.compartmentId
            val compartment = getCompartment(compartmentId)
            if (compartment == null) {
                println("Compartment not found")
                return null
            }
            compartment.open()
            val pkg = lockerStation.compartmentToPackage[compartmentId]
            releaseCompartment(compartment)
            compartment.close()
            cleanup(accessCode.id, compartment)
            return pkg
        }
    }

    fun staffRemoval(compartmentId: String): Package? {
        synchronized(lockerStation) {
            val accessCodeId = lockerStation.compartmentToAccessCodes[compartmentId]
            val compartment = getCompartment(compartmentId)
            if (compartment == null) {
                println("Compartment not found")
                return null
            }
            compartment.open()
            val pkg = lockerStation.compartmentToPackage[compartmentId]
            releaseCompartment(compartment)
            compartment.close()
            cleanup(accessCodeId, compartment)
            return pkg
        }
    }
}


fun main(){
    val lockerStation = LockerStation(10)
    val depositManager = DepositManager(lockerStation)
    val retrieveManager = RetrieveManager(lockerStation)
    val pkg1 = Package(PACKAGE_SIZE.SMALL)
    val pkg2 = Package(PACKAGE_SIZE.MEDIUM)
    val pkg3 = Package(PACKAGE_SIZE.LARGE)
    val pkg4 = Package(PACKAGE_SIZE.SMALL)
    
    val accessCode1 = depositManager.deposit(pkg1)
    val accessCode2 = depositManager.deposit(pkg2)
    val accessCode3 = depositManager.deposit(pkg3)
    val accessCode4 = depositManager.deposit(pkg4)

    if(accessCode1 != null){
        val retrievedPkg1 = retrieveManager.retrieve(accessCode1)
        println("Retrieved package 1 of size ${retrievedPkg1?.size}")
    }
    if(accessCode2 != null){
        val retrievedPkg2 = retrieveManager.retrieve(accessCode2)
        println("Retrieved package 2 of size ${retrievedPkg2?.size}")
    }
    if(accessCode3 != null){
        val retrievedPkg3 = retrieveManager.retrieve(accessCode3)
        println("Retrieved package 3 of size ${retrievedPkg3?.size}")
    }

    if(accessCode4 != null){
        val retrievedPkg4 = retrieveManager.staffRemoval(accessCode4.compartmentId)
        println("Retrieved package 4 of size ${retrievedPkg4?.size}")
    }
}