package org.personal.coupleapp.service

interface HTTPConnectionInterface {
    fun onHttpRespond(responseData : HashMap<*, *>)
}