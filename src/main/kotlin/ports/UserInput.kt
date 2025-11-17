package ports

interface UserInput {
    fun readLine(prompt: String): String?
}
