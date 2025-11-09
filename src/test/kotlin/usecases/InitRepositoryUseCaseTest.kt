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
        // Given
        every { repository.initialize() } returns Unit

        // When
        useCase.initRepository()

        // Then
        verify(exactly = 1) { repository.initialize() }
    }

    @Test
    fun `should handle multiple initialization calls`() {
        // Given
        every { repository.initialize() } returns Unit

        // When
        useCase.initRepository()
        useCase.initRepository()
        useCase.initRepository()

        // Then
        verify(exactly = 3) { repository.initialize() }
    }
}
