import com.hbisoft.hbrecorder.HBRecorder
fun main() {
    println(HBRecorder::class.java.methods.joinToString("\n") { it.name })
}
