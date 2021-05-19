package processes.implementations

import processes.Process

class RunningProcess(private val processHandle: ProcessHandle) :
    Process(command = processHandle.info().command().orElse("")) {

    val user: String get() = processHandle.info().user().orElse("")

    fun kill() {
        processHandle.destroy()
    }

}