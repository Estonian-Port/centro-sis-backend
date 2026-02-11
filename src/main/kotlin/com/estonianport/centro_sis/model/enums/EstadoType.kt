package com.estonianport.centro_sis.model.enums

enum class EstadoType {
    ACTIVO,
    INACTIVO,
    PENDIENTE,
    BAJA
}

/*
|---------------|
| PARA USUARIOS |
|---------------|
ACTIVO: El usuario está activo y puede acceder a sus funcionalidades.
INACTIVO: El usuario realizo el registro pero no esta en ningun curso.
PENDIENTE: Un admin ha invitado al usuario via mail pero este aun no ha completado su registro.
BAJA: El usuario ha sido dado de baja y no puede acceder a sus funcionalidades.

|---------------|
|  PARA CURSOS  |
|---------------|
ACTIVO: El curso está en funcionamiento y los usuarios pueden inscribirse.
PENDIENTE: El curso ha sido creado por el admin/oficina pero el profesor aun no completo el resto de los datos
BAJA: El curso ha sido cancelado o eliminado y no está disponible para inscripciones.
INACTIVO: Por el momento el sistema no utiliza este estado para cursos.

 */