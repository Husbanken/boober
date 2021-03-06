@file:JvmName("Main")

package no.skatteetaten.aurora.boober

import no.skatteetaten.aurora.boober.mapper.platform.ApplicationPlatformHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.ApplicationListener

@SpringBootApplication
@EnableCaching
class Boober(val aphBeans: List<ApplicationPlatformHandler>) : ApplicationListener<ApplicationReadyEvent> {


    companion object {
        val logger: Logger = LoggerFactory.getLogger(Boober::class.java)

        @JvmStatic
        var APPLICATION_PLATFORM_HANDLERS: Map<String, ApplicationPlatformHandler> = emptyMap()
    }


    override fun onApplicationEvent(event: ApplicationReadyEvent?) {

        APPLICATION_PLATFORM_HANDLERS = aphBeans.associateBy { it.name }
        logger.info("Boober started with applicationPlatformHandlers ${APPLICATION_PLATFORM_HANDLERS.keys}")
    }

}

fun main(args: Array<String>) {
    SpringApplication.run(Boober::class.java, *args)
}

