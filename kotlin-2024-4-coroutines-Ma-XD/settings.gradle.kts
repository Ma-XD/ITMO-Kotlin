fun sanitizeName(name: String): String {
    return name.replace("[ /\\\\:<>\"?*|()]".toRegex(), "_").replace("(^[.]+)|([.]+\$)".toRegex(), "")
}

rootProject.name = "kotlin-2024-4-coroutines"

val tasksDir = rootProject.projectDir.resolve("tasks")
tasksDir.walkTopDown().forEach {
    if (!isTaskDir(it) || it.path.contains(".idea")) {
        return@forEach
    }
    val taskRelativePath = tasksDir.toPath().relativize(it.toPath())
    val parts = mutableListOf<String>()
    for (name in taskRelativePath) {
        parts.add(sanitizeName(name.toString()))
    }
    val moduleName = parts.joinToString("-")
    include(moduleName)
    project(":$moduleName").projectDir = it
}

fun isTaskDir(dir: File): Boolean {
    return File(dir, "src").exists()
}

include("util")
