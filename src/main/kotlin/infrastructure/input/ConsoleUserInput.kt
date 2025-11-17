package infrastructure.input

import ports.UserInput

class ConsoleUserInput : UserInput {
    override fun readLine(prompt: String): String? {
        print(prompt)
        return readlnOrNull()
    }
}
