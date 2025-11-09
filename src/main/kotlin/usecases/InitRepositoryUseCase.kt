package usecases

import ports.BragRepository

class InitRepositoryUseCase(
    private val repository: BragRepository,
) {
    fun initRepository() {
        repository.initialize()
    }
}
