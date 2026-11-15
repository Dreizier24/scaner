package ru.cora.scanner.data

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import retrofit2.HttpException

object ApiErrorMessages {

    fun forUser(throwable: Throwable): String = when (throwable) {
        is UnknownHostException -> "Нет связи с сервером. Проверьте интернет."
        is SocketTimeoutException -> "Сервер не отвечает. Попробуйте чуть позже."
        is SSLException -> "Не удалось установить защищённое соединение. Обновите приложение или обратитесь в кассу."
        is IOException -> "Проблема с сетью. Проверьте Wi‑Fi или мобильный интернет."
        is HttpException -> when (throwable.code()) {
            in 500..599 -> "Сервер временно недоступен. Попробуйте позже."
            404 -> "Список концертов на сервере не найден."
            else -> "Не удалось загрузить данные (ошибка ${throwable.code()})."
        }
        else -> "Не удалось загрузить концерты. Проверьте интернет и попробуйте снова."
    }
}