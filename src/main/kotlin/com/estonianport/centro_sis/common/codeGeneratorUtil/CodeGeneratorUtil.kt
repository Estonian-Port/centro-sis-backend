package com.estonianport.centro_sis.common.codeGeneratorUtil

import java.util.*

object CodeGeneratorUtil {

    val base26Only4Letters: String
        get() {
            val random = Random()
            val base26chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()
            val sb = StringBuilder(4)
            for (i in 0..3) {
                sb.append(base26chars[random.nextInt(24)])
            }
            return sb.toString()
        }
}