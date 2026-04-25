enum class DIRECTION {
    UP,
    DOWN,
    IDLE
}

enum class RequestType {
    PICKUP_UP,
    PICKUP_DOWN,
    DESTINATION
}

data class Request(
    val floor: Int,
    val type: RequestType
)

class Elevator {
    var currentFloor: Int = 0
        private set
    var direction: DIRECTION = DIRECTION.IDLE
        private set
    private val requests: HashSet<Request> = HashSet()

    private fun hasRequestsAhead(): Boolean {
        var floor = currentFloor
        while((direction == DIRECTION.UP && floor < 9) || (direction == DIRECTION.DOWN && floor > 0)){
            val nextFloor = floor + (if(direction == DIRECTION.UP) 1 else -1)
            val possibleRequests = listOf(Request(nextFloor, RequestType.PICKUP_UP), Request(nextFloor, RequestType.PICKUP_DOWN), Request(nextFloor, RequestType.DESTINATION))
            for (possibleRequest in possibleRequests) {
                if(requests.contains(possibleRequest)) return true
            }
            floor = nextFloor
        }
        return false
    }

    private fun fulfillRequest(request: Request): Boolean {
        if(requests.contains(request)){
            requests.remove(request)
            return true
        }
        return false
    }

    fun addRequest(request: Request) {
        if(request.floor < 0 || request.floor > 9) return
        if(request.floor == currentFloor) return
        if(requests.isEmpty()){
            val distance = (request.floor - currentFloor)
            direction = if(distance > 0) DIRECTION.UP else DIRECTION.DOWN
        }
        requests.add(request)
    }

    fun step(){
        if(requests.isEmpty()){
            return
        }

        val currentDestinationRequest = Request(currentFloor, RequestType.DESTINATION)
        var isRequestFulfilled = fulfillRequest(currentDestinationRequest)

        val currentPickUpRequest = Request(currentFloor, if(direction == DIRECTION.UP) RequestType.PICKUP_UP else RequestType.PICKUP_DOWN)
        isRequestFulfilled = isRequestFulfilled or fulfillRequest(currentPickUpRequest)

        if(requests.isEmpty()){
            direction = DIRECTION.IDLE
            return
        }

        if(isRequestFulfilled) return

        if(hasRequestsAhead()){
            currentFloor += if(direction == DIRECTION.UP) 1 else -1
        }else{
            direction = if(direction == DIRECTION.UP) DIRECTION.DOWN else DIRECTION.UP
        }
    }

    fun canFulfillRequest(request: Request): Boolean {
        val floor = request.floor
        val dir = if(request.type == RequestType.PICKUP_UP) DIRECTION.UP else DIRECTION.DOWN
        for (req in requests) {
            if(dir == DIRECTION.UP && req.floor > floor && (req.type == RequestType.PICKUP_UP || req.type == RequestType.DESTINATION)) return true
            if(dir == DIRECTION.DOWN && req.floor < floor && (req.type == RequestType.PICKUP_DOWN || req.type == RequestType.DESTINATION)) return true
        }
        return false
    }
}

class ElevatorController {
    private val elevators: MutableList<Elevator>

    init {
        elevators = mutableListOf(
            Elevator(),
            Elevator(),
            Elevator()
        )
    }

    private fun validateRequest(request: Request): Boolean{
        if(request.floor < 0 || request.floor > 9 || request.type == RequestType.DESTINATION) return false
        return true
    }

    private fun selectBestElevator(request: Request): Elevator? {
        val floor = request.floor
        val dir = if(request.type == RequestType.PICKUP_UP) DIRECTION.UP else DIRECTION.DOWN

        var minDistance = Int.MAX_VALUE
        var selectedElevator: Elevator? = null

        for (elevator in elevators) {
            if(elevator.direction != dir) continue
            val distance = floor - elevator.currentFloor
            if((elevator.direction == DIRECTION.UP && distance < 0) || 
                (elevator.direction == DIRECTION.DOWN && distance > 0)
            ) continue

            if(elevator.canFulfillRequest(request)) {
                if(minDistance > Math.abs(distance)) {
                    minDistance = Math.abs(distance)
                    selectedElevator = elevator
                }
            }
        }
        return selectedElevator
    }

    private fun getNearestIdleElevator(request: Request): Elevator? {
        var minDistance = Int.MAX_VALUE
        var selectedElevator: Elevator? = null
        for (elevator in elevators) {
            if(elevator.direction == DIRECTION.IDLE){
                val distance = Math.abs(request.floor - elevator.currentFloor)
                if(minDistance > distance) {
                    minDistance = distance
                    selectedElevator = elevator
                }
            }
        }
        return selectedElevator
    }

    private fun getNearestElevator(request: Request): Elevator? {
        val floor = request.floor

        var minDistance = Int.MAX_VALUE
        var selectedElevator: Elevator? = null

        for (elevator in elevators) {
            var distance = floor - elevator.currentFloor

            if(distance > 0 && elevator.direction == DIRECTION.DOWN){
                distance += 2 * elevator.currentFloor
            }else if(distance <= 0 && elevator.direction == DIRECTION.UP){
                distance += 2 * (9 - elevator.currentFloor)
            }

            if(minDistance > Math.abs(distance)) {
                minDistance = Math.abs(distance)
                selectedElevator = elevator
            }
        }
        return selectedElevator
    } 

    fun requestElevator(request: Request) {
        if(!validateRequest(request)) {
            println("Requested floor is invalid")
            return
        }

        var elevator: Elevator? = selectBestElevator(request)
        if(elevator != null) {
            elevator.addRequest(request)
            return
        }

        elevator = getNearestIdleElevator(request)
        if(elevator != null) {
            elevator.addRequest(request)
            return
        }

        elevator = getNearestElevator(request)
        elevator?.addRequest(request)
    }

    fun step() {
        for (elevator in elevators) {
            elevator.step()
        }
    }
}