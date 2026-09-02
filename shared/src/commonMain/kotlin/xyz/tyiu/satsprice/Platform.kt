package xyz.tyiu.satsprice

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform