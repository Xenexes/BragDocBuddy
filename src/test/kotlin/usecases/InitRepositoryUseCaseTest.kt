package usecases

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import ports.BragRepository

class InitRepositoryUseCaseTest {
    private val repository = mockk<BragRepository>()
    private val useCase = InitRepositoryUseCase(repository)

    @Test
    fun `should call repository initialize method`() {
        every { repository.initialize() } returns Unit

        useCase.initRepository()

        verify(exactly = 1) { repository.initialize() }
    }

    @Test
    fun `should handle multiple initialization calls`() {
        every { repository.initialize() } returns Unit

        useCase.initRepository()
        useCase.initRepository()
        useCase.initRepository()

        verify(exactly = 3) { repository.initialize() }
    }
}
