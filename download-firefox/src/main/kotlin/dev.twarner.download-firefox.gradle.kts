import dev.twarner.gradle.DownloadFirefoxTask

tasks.register("downloadFirefox", DownloadFirefoxTask::class) {
    version = "123.0"
}
