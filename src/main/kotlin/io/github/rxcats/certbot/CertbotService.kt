package io.github.rxcats.certbot

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

@Service
class CertbotService {

    private val log = LoggerFactory.getLogger(CertbotService::class.java)

//    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
//    fun test() {
//        log.info("UTC: {}, Asia/Seoul: {}", ZonedDateTime.now(ZoneId.of("UTC")), ZonedDateTime.now(ZoneId.of("Asia/Seoul")))
//
//        val certificates = execute("sudo certbot certificates | grep 'Expiry Date'")
//        log.info("certificates: {}", certificates)
//
//        val days = afterDays(certificates)
//        log.info("days: {}", days)
//
//        val nginxStatus = execute("sudo systemctl status nginx.service")
//        log.info("nginxStatus: {}", nginxStatus)
//    }

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    fun renew() {
        log.info("UTC: {}, Asia/Seoul: {}", ZonedDateTime.now(ZoneId.of("UTC")), ZonedDateTime.now(ZoneId.of("Asia/Seoul")))

        val certificates = execute("sudo certbot certificates | grep 'Expiry Date'")
        log.info("certificates: {}", certificates)

        val days = afterDays(certificates)
        log.info("days: {}", days)

        if (days > -1 && days <= 3) {
            log.info("start renew")
            execute("sudo systemctl stop nginx.service")
            execute("sudo certbot renew --quiet --no-self-upgrade")
            execute("sudo systemctl start nginx.service")
            log.info("end renew")

            val nginxStatus = execute("sudo systemctl status nginx.service")
            log.info("nginxStatus: {}", nginxStatus)
        }

        log.info("completed")
    }

    fun afterDays(str: String): Int {
        val p = Pattern.compile("VALID:\\s([0-9]*)\\sdays")
        val matcher = p.matcher(str)
        if (matcher.find()) {
            val days = matcher.group(1)
            return days.toInt()
        }
        return -1
    }

    fun execute(vararg cmd: String): String {
        val builder = ProcessBuilder()
        builder.command("/bin/bash", "-c", *cmd)

        val output = StringBuilder()

        try {
            log.info("command: {}", builder.command())
            val p = builder.start()
            BufferedReader(InputStreamReader(p.inputStream)).forEachLine { output.append(it).append("\n") }
            p.waitFor(5, TimeUnit.MINUTES)
        } catch (e: IOException) {
            log.error(e.toString(), e)
        } catch (e: InterruptedException) {
            log.error(e.toString(), e)
        }

        return output.toString()
    }

}