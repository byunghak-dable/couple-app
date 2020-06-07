package org.personal.coupleapp.interfaces.service

interface HTTPConnectionListener {
    fun onHttpRespond(responseData : HashMap<*, *>)
}