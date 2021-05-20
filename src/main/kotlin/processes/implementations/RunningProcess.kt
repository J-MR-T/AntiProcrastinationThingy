package processes.implementations

import processes.Process

class RunningProcess(private val processHandle: ProcessHandle, val afterKill: ((RunningProcess) -> Unit)? = null) :
    Process(command = processHandle.info().command().orElse("")) {

    val user: String = processHandle.info().user().orElse("").toLowerCase()

    fun kill(afterKill: ((RunningProcess) -> Unit)? = this.afterKill) {
        val worked = processHandle.destroy()
        if (!worked) {
            System.err.println("Couldn't kill process $stringRepresentation")
        }
        afterKill?.invoke(this)
    }

}